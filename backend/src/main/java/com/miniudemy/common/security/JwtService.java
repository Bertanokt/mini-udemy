package com.miniudemy.common.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * JWT üretir ve doğrular (HS256).
 *
 * Token'da {@code sub} kullanıcı id'si, {@code role} rol claim'idir (docs/api/openapi.yaml › bearerAuth).
 * İstek başına doğrulamayı Spring Security'nin hazır filtresi ({@code BearerTokenAuthenticationFilter}) aynı
 * {@link JwtDecoder} ile yapar; bu sınıfın {@link #validate(String)} metodu aynı kuralları servis ve testlerden
 * çağırmak içindir.
 *
 * Bilerek {@code common}'da ve rolü {@code String} olarak alıyor: {@code common} hiçbir modüle (örn.
 * {@code auth.domain.Role}) bağımlı olmaz.
 */
public class JwtService {

    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** Kullanıcı için imzalı token üretir; süresi {@code app.jwt.expiration}. */
    public String generateToken(Long userId, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .issuedAt(now)
                .expiresAt(now.plus(properties.expiration()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Sözleşmedeki {@code expiresIn} (saniye). */
    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

    /**
     * Token'ı imza ve süre açısından doğrular.
     *
     * @throws JwtException imza bozuk, anahtar farklı, süre dolmuş ya da biçim hatalıysa
     */
    public Jwt validate(String token) {
        return decoder.decode(token);
    }
}
