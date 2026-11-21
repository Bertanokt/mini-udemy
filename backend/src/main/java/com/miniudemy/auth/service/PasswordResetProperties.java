package com.miniudemy.auth.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parola yenileme ayarları ({@code app.password-reset.*}, #115).
 *
 * @param ttl      bağlantının geçerlilik süresi (varsayılan 30 dakika)
 * @param linkBase ön yüzün kök adresi; bağlantı {@code linkBase + /reset-password?token=...} olur
 */
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetProperties(Duration ttl, String linkBase) {

    public PasswordResetProperties {
        if (ttl == null) {
            ttl = Duration.ofMinutes(30);
        }
        if (linkBase == null || linkBase.isBlank()) {
            linkBase = "http://localhost:3000";
        }
        linkBase = linkBase.endsWith("/") ? linkBase.substring(0, linkBase.length() - 1) : linkBase;
    }
}
