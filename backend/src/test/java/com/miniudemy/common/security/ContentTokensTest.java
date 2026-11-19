package com.miniudemy.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Ders içeriği token'ı (#45, ADR-012): kullanıcıya ve derse özel, 5 dakika geçerli, ana JWT anahtarından türetilmiş
 * ayrı bir anahtarla imzalı. Ana API token'ı içerik token'ı yerine, içerik token'ı da API token'ı yerine geçmez.
 */
class ContentTokensTest {

    private static final String SECRET = "test-only-mini-udemy-jwt-anahtari-yalniz-testler-icin";
    private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1));

    private ContentTokens tokensAt(Instant instant) {
        return new ContentTokens(properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private JwtService apiAt(Instant instant) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        JwtConfig config = new JwtConfig();
        return new JwtService(config.jwtEncoder(properties), config.jwtDecoder(properties, clock), properties, clock);
    }

    @Test
    void issuedToken_isValidForThatLesson_andGivesTheUser_andExpiresInFiveMinutes() {
        IssuedContentToken issued = tokensAt(NOW).issue(42L, 11L);

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(tokensAt(NOW.plusSeconds(60)).verify(issued.token(), 11L)).contains(42L);
    }

    @Test
    void token_isNotValidForAnotherLesson() {
        IssuedContentToken issued = tokensAt(NOW).issue(42L, 11L);

        assertThat(tokensAt(NOW).verify(issued.token(), 12L)).isEmpty();
    }

    @Test
    void token_isNotValidAfterItExpires() {
        IssuedContentToken issued = tokensAt(NOW).issue(42L, 11L);

        assertThat(tokensAt(NOW.plus(Duration.ofMinutes(6))).verify(issued.token(), 11L)).isEmpty();
    }

    @Test
    void apiLoginToken_isNotAcceptedAsContentToken() {
        String login = apiAt(NOW).generateToken(42L, "STUDENT");

        assertThat(tokensAt(NOW).verify(login, 11L)).isEmpty();
    }

    @Test
    void contentToken_isNotAcceptedAsApiToken() {
        IssuedContentToken issued = tokensAt(NOW).issue(42L, 11L);

        assertThatThrownBy(() -> apiAt(NOW).validate(issued.token())).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedOrGarbageToken_isRejected() {
        String token = tokensAt(NOW).issue(42L, 11L).token();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(tokensAt(NOW).verify(tampered, 11L)).isEmpty();
        assertThat(tokensAt(NOW).verify("çöp", 11L)).isEmpty();
        assertThat(tokensAt(NOW).verify("", 11L)).isEmpty();
    }
}
