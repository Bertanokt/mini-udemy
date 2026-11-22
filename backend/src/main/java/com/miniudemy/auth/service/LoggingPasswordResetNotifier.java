package com.miniudemy.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Yalnız dev profili (#115): e-posta yerine bağlantıyı log'a yazar; geliştirme ve demo sırasında bağlantı backend
 * logundan alınır. Token'ı log'a yazdığı için başka profilde etkin olmamalıdır.
 */
@Component
@Profile("dev")
class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void sendResetLink(String email, String resetLink, Instant expiresAt) {
        log.info("[dev] Parola yenileme bağlantısı ({}, {} tarihine kadar geçerli): {}", email, expiresAt, resetLink);
    }
}
