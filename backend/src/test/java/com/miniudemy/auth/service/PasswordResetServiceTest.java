package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.PasswordResetToken;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.PasswordResetConfirmRequest;
import com.miniudemy.auth.repository.PasswordResetTokenRepository;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ApiException;

/** Parola yenileme kuralları (#115): kullanıcı varlığı sızmıyor, token hash'li, süreli, tek kullanımlık. */
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");
    private static final String EMAIL = "ayse.yilmaz@example.com";

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetService service = new PasswordResetService(users, tokens, notifier, encoder,
            new PasswordResetProperties(Duration.ofMinutes(30), "http://localhost:3000/"),
            Clock.fixed(NOW, ZoneOffset.UTC));

    private User ayse;

    @BeforeEach
    void setUp() {
        ayse = new User(EMAIL, "$2a$10$eski", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(ayse, "id", 42L);
        given(users.findByEmail(anyString())).willReturn(Optional.empty());
        given(users.findByEmail(EMAIL)).willReturn(Optional.of(ayse));
        given(users.findById(42L)).willReturn(Optional.of(ayse));
        given(encoder.encode(anyString())).willAnswer(inv -> "$2a$10$yeni:" + inv.getArgument(0));
        given(tokens.markUsed(anyLong(), any())).willReturn(1);
    }

    // --- İstek ---

    @Test
    void request_unknownEmail_doesNothing_noTokenNoNotification() {
        service.requestReset("kayitli.degil@example.com");

        then(tokens).shouldHaveNoInteractions();
        then(notifier).shouldHaveNoInteractions();
    }

    @Test
    void request_disabledAccount_doesNothing() {
        ayse.setActive(false);

        service.requestReset(EMAIL);

        then(tokens).shouldHaveNoInteractions();
        then(notifier).shouldHaveNoInteractions();
    }

    @Test
    void request_knownUser_invalidatesOldTokens_storesOnlyTheHash_andSendsLinkWithTheToken() {
        service.requestReset("  " + EMAIL + " ");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        then(tokens).should().invalidateUnused(42L, NOW);
        then(tokens).should().save(saved.capture());
        then(notifier).should().sendResetLink(org.mockito.ArgumentMatchers.eq(EMAIL), link.capture(),
                org.mockito.ArgumentMatchers.eq(NOW.plus(Duration.ofMinutes(30))));

        assertThat(link.getValue()).startsWith("http://localhost:3000/reset-password?token=");
        String token = link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(saved.getValue().getUserId()).isEqualTo(42L);
        assertThat(saved.getValue().getTokenHash()).hasSize(64).isEqualTo(PasswordResetService.hash(token))
                .isNotEqualTo(token);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    void request_twice_producesDifferentTokens() {
        service.requestReset(EMAIL);
        service.requestReset(EMAIL);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        then(tokens).should(org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getTokenHash())
                .isNotEqualTo(saved.getAllValues().get(1).getTokenHash());
    }

    // --- Onay ---

    @Test
    void confirm_validToken_changesThePassword_marksTokenUsed_andInvalidatesTheOthers() {
        PasswordResetToken token = storedToken("gecerli", NOW.plusSeconds(600), null);

        service.confirmReset(new PasswordResetConfirmRequest("gecerli", "YeniGucluParola456"));

        then(tokens).should().markUsed(token.getId(), NOW);
        assertThat(ayse.getPasswordHash()).isEqualTo("$2a$10$yeni:YeniGucluParola456");
        then(tokens).should().invalidateUnused(42L, NOW);
    }

    @Test
    void confirm_unknownToken_returns400OnTokenField() {
        given(tokens.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertInvalidToken("uydurma");
    }

    @Test
    void confirm_expiredToken_returns400_andPasswordUnchanged() {
        storedToken("eski", NOW, null);

        assertInvalidToken("eski");
        then(tokens).should(never()).markUsed(anyLong(), any());
    }

    @Test
    void confirm_usedToken_returns400_andPasswordUnchanged() {
        storedToken("kullanilmis", NOW.plusSeconds(600), NOW.minusSeconds(60));

        assertInvalidToken("kullanilmis");
        then(tokens).should(never()).markUsed(anyLong(), any());
    }

    @Test
    void confirm_whenAnotherRequestUsedTheTokenFirst_returns400_andPasswordUnchanged() {
        storedToken("yaris", NOW.plusSeconds(600), null);
        given(tokens.markUsed(anyLong(), any())).willReturn(0);

        assertInvalidToken("yaris");
    }

    @Test
    void confirm_passwordOver72Bytes_returns400OnNewPassword() {
        String longTurkish = "ğ".repeat(37); // 37 karakter, 74 bayt

        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest("x", longTurkish)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrors())
                .satisfies(errors -> assertThat(errors).containsKey("newPassword"));
        then(tokens).shouldHaveNoInteractions();
    }

    private void assertInvalidToken(String token) {
        assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmRequest(token, "YeniGucluParola456")))
                .isInstanceOf(ApiException.class)
                .hasMessage(PasswordResetService.INVALID_TOKEN)
                .extracting(ex -> ((ApiException) ex).getErrors())
                .isEqualTo(java.util.Map.of("token", PasswordResetService.INVALID_TOKEN));
        assertThat(ayse.getPasswordHash()).isEqualTo("$2a$10$eski");
    }

    private PasswordResetToken storedToken(String token, Instant expiresAt, Instant usedAt) {
        PasswordResetToken stored = new PasswordResetToken(42L, PasswordResetService.hash(token),
                NOW.minusSeconds(60), expiresAt);
        ReflectionTestUtils.setField(stored, "id", 5L);
        ReflectionTestUtils.setField(stored, "usedAt", usedAt);
        given(tokens.findByTokenHash(PasswordResetService.hash(token))).willReturn(Optional.of(stored));
        return stored;
    }
}
