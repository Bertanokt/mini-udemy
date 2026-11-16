package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.AuthResponse;
import com.miniudemy.auth.dto.LoginRequest;
import com.miniudemy.auth.dto.UserResponse;
import com.miniudemy.auth.mapper.UserMapper;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.common.exception.ApiException;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityMessages;

/**
 * Giriş kuralları (#13, FR-02): doğru bilgilerle token + kullanıcı; yanlış e-posta ile yanlış parola aynı 401 ve
 * aynı mesaj; kullanıcı yoksa da parola karşılaştırması yapılır; pasif hesap yalnız parola doğruysa 403.
 *
 * Parola özeti gerçek BCrypt ile (düşük maliyetle) üretilir; token üretimi taklit edilir.
 */
class AuthServiceTest {

    private static final String EMAIL = "ayse.yilmaz@example.com";
    private static final String PASSWORD = "GucluParola123";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder(4));
    private final JwtService jwtService = mock(JwtService.class);
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, userMapper);
        given(jwtService.generateToken(eq(42L), anyString())).willReturn("imzali.jwt.token");
        given(jwtService.expiresInSeconds()).willReturn(3600L);
    }

    @Test
    void login_withCorrectCredentials_returnsBearerTokenAndUser() {
        User user = user(true);
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo("imzali.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user()).isEqualTo(new UserResponse(42L, EMAIL, "Ayşe Yılmaz", Role.INSTRUCTOR, null,
                Instant.parse("2026-11-11T09:30:00Z")));
        verify(jwtService).generateToken(42L, "INSTRUCTOR");
    }

    @Test
    void login_withWrongPassword_returns401WithGenericMessage() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user(true)));

        assertInvalidCredentials(new LoginRequest(EMAIL, "YanlisParola1"));
        verify(jwtService, never()).generateToken(eq(42L), anyString());
    }

    @Test
    void login_withUnknownEmail_returnsSame401AsWrongPassword() {
        given(userRepository.findByEmail("yok@example.com")).willReturn(Optional.empty());

        assertInvalidCredentials(new LoginRequest("yok@example.com", PASSWORD));
    }

    @Test
    void login_withUnknownEmail_stillComparesPassword_soResponseTimeDoesNotRevealEmail() {
        given(userRepository.findByEmail("yok@example.com")).willReturn(Optional.empty());

        assertInvalidCredentials(new LoginRequest("yok@example.com", PASSWORD));
        verify(passwordEncoder).matches(eq(PASSWORD), anyString());
    }

    @Test
    void login_withDisabledAccountAndCorrectPassword_returns403AccountDisabled() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(ApiException.class)
                .hasMessage(SecurityMessages.ACCOUNT_DISABLED)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void login_withDisabledAccountAndWrongPassword_returns401_soAccountStateIsNotRevealed() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user(false)));

        assertInvalidCredentials(new LoginRequest(EMAIL, "YanlisParola1"));
    }

    @Test
    void login_withPasswordLongerThan72Bytes_returns401NotServerError() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user(true)));

        // 72 karakter ama 144 bayt (UTF-8): @Size(max = 72) geçer, BCrypt'in 72 bayt sınırını aşar.
        assertInvalidCredentials(new LoginRequest(EMAIL, "ş".repeat(72)));
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessage(SecurityMessages.INVALID_CREDENTIALS)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private User user(boolean active) {
        User user = new User(EMAIL, new BCryptPasswordEncoder(4).encode(PASSWORD), "Ayşe Yılmaz", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(user, "id", 42L);
        ReflectionTestUtils.setField(user, "active", active);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        return user;
    }
}
