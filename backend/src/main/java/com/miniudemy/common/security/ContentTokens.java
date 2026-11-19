package com.miniudemy.common.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Ders içeriği için kısa ömürlü, derse özel token (#45, ADR-012).
 *
 * Tarayıcının {@code <video>} ve {@code <iframe>} etiketleri istekte {@code Authorization} başlığı gönderemez; içerik
 * adresi bu token'ı {@code ?token=} parametresiyle taşır. Token yalnız kimlik ve ders bilgisi taşır; yetki (kayıt,
 * sahiplik, rol, hesap aktifliği) her istekte yeniden sorulur.
 *
 * Ana API token'ından ayrılması için imza anahtarı, ana JWT anahtarından HMAC ile türetilmiş ayrı bir anahtardır.
 * Böylece içerik token'ı API'nin Bearer doğrulayıcısında imzadan geçmez, API token'ı da burada geçmez. Ek olarak
 * {@code typ=content} ve ders id'si denetlenir.
 */
public class ContentTokens {

    /** Geçerlilik süresi. Oynatma bundan uzun sürerse tarayıcı yeni token alıp kaldığı yerden devam eder. */
    public static final Duration TTL = Duration.ofMinutes(5);

    static final String TYPE_CLAIM = "typ";
    static final String TYPE = "content";
    static final String LESSON_CLAIM = "lesson";
    private static final String KEY_LABEL = "mini-udemy/content-token/v1";

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final Clock clock;

    public ContentTokens(JwtProperties properties, Clock clock) {
        SecretKey key = deriveKey(properties.secret());
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamps.setClock(clock);
        this.decoder.setJwtValidator(timestamps);
        this.clock = clock;
    }

    /** Kullanıcı ve ders için yeni token. Yetkiyi çağıran denetler (içerik servisi). */
    public IssuedContentToken issue(Long userId, Long lessonId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(TTL);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, TYPE)
                .claim(LESSON_CLAIM, lessonId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new IssuedContentToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(),
                expiresAt);
    }

    /** Token geçerli, süresi dolmamış ve bu derse aitse kullanıcının id'si; değilse boş. */
    public Optional<Long> verify(String token, Long lessonId) {
        try {
            Jwt jwt = decoder.decode(token);
            Object lesson = jwt.getClaim(LESSON_CLAIM);
            if (!TYPE.equals(jwt.getClaimAsString(TYPE_CLAIM)) || !(lesson instanceof Number number)
                    || number.longValue() != lessonId) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(jwt.getSubject()));
        } catch (RuntimeException e) {
            // Boş, bozuk, imzası tutmayan ya da süresi dolmuş token: kimlik yok.
            return Optional.empty();
        }
    }

    /** Ana anahtardan HMAC-SHA256 ile türetilmiş, yalnız içerik token'larına ait anahtar. */
    static SecretKey deriveKey(String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return new SecretKeySpec(mac.doFinal(KEY_LABEL.getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("İçerik token anahtarı türetilemedi.", e);
        }
    }
}
