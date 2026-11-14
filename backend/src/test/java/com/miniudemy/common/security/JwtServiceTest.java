package com.miniudemy.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * JwtService: üretilen token doğrulanır ve sözleşmedeki claim'leri taşır (sub = kullanıcı id'si, role);
 * süresi dolmuş, imzası değiştirilmiş ya da başka anahtarla imzalanmış token reddedilir.
 */
class JwtServiceTest {

    private static final String SECRET = "test-only-mini-udemy-jwt-anahtari-yalniz-testler-icin";
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofHours(1));

    private JwtService serviceAt(Instant instant, JwtProperties props) {
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        JwtConfig config = new JwtConfig();
        return new JwtService(config.jwtEncoder(props), config.jwtDecoder(props, clock), props, clock);
    }

    @Test
    void generatedToken_validates_andCarriesSubjectRoleAndExpiry() {
        JwtService service = serviceAt(NOW, properties);

        Jwt jwt = service.validate(service.generateToken(42L, "INSTRUCTOR"));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("INSTRUCTOR");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(service.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void expiredToken_isRejected() {
        String token = serviceAt(NOW, properties).generateToken(42L, "STUDENT");
        // Süre 1 saat, tolerans 30 sn: 1 saat 1 dakika sonra geçersiz.
        JwtService later = serviceAt(NOW.plus(Duration.ofMinutes(61)), properties);

        assertThatThrownBy(() -> later.validate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService service = serviceAt(NOW, properties);
        String token = service.generateToken(42L, "STUDENT");
        // Payload'daki rolü ADMIN yapıp imzayı olduğu gibi bırakmak: imza artık tutmaz.
        String[] parts = token.split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"42\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> service.validate(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtProperties other = new JwtProperties("baska-bir-anahtar-ile-imzalanmis-sahte-token-12345", Duration.ofHours(1));
        String foreign = serviceAt(NOW, other).generateToken(42L, "ADMIN");

        assertThatThrownBy(() -> serviceAt(NOW, properties).validate(foreign)).isInstanceOf(JwtException.class);
    }
}
