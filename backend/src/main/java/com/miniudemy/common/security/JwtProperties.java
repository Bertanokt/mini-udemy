package com.miniudemy.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT ayarları ({@code app.jwt.*}).
 *
 * <ul>
 *   <li>{@code secret}: HS256 imza anahtarı. {@code application.yml}'da yedeksiz olarak {@code ${JWT_SECRET}}'ten
 *       okunur; yalnız dev ve test profillerinde açıkça "dev-only" / "test-only" etiketli sabit bir değer vardır.</li>
 *   <li>{@code expiration}: token'ın geçerlilik süresi; sözleşmedeki {@code expiresIn} (varsayılan 1 saat).</li>
 * </ul>
 *
 * Anahtar boşsa ya da 32 bayttan kısaysa uygulama açılmaz (güvenlik denetimi O2): HS256 en az 256 bitlik anahtar
 * ister ve zayıf ya da unutulmuş bir anahtarla çalışan sistem, herkesin token üretebildiği bir sistemdir.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {

    /** HS256 için gereken en kısa anahtar (bayt). */
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        // ${JWT_SECRET} ortam değişkeni yoksa Spring ifadeyi çözmeden düz metin olarak bırakır.
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException(
                    "JWT anahtarı tanımlı değil: JWT_SECRET ortam değişkenini ayarlayın (app.jwt.secret).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT anahtarı en az " + MIN_SECRET_BYTES + " bayt olmalı (HS256); JWT_SECRET çok kısa.");
        }
        if (expiration == null) {
            expiration = Duration.ofHours(1);
        }
        if (expiration.isNegative() || expiration.isZero()) {
            throw new IllegalStateException("app.jwt.expiration pozitif bir süre olmalı.");
        }
    }

    /** {@code toString} anahtarı loga ya da hata mesajına yazmasın. */
    @Override
    public String toString() {
        return "JwtProperties[secret=***, expiration=" + expiration + "]";
    }
}
