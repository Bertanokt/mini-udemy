package com.miniudemy.auth.service;

import java.time.Instant;

/**
 * Parola yenileme bağlantısını kullanıcıya ulaştırır (#115). E-posta gönderimi kapsam dışı (17.11 ince kapsam
 * kararı); dev profilinde {@link LoggingPasswordResetNotifier} bağlantıyı log'a yazar, diğer profillerde
 * {@link DisabledPasswordResetNotifier} bağlantıyı hiçbir yere yazmaz. İleride SMTP/Mailpit uygulaması bu arayüzle
 * eklenir; servis değişmez.
 */
public interface PasswordResetNotifier {

    /**
     * @param email     alıcının e-posta adresi
     * @param resetLink ön yüzdeki yeni parola sayfasının token'lı adresi ({@code /reset-password?token=...})
     * @param expiresAt bağlantının geçerlilik sonu
     */
    void sendResetLink(String email, String resetLink, Instant expiresAt);
}
