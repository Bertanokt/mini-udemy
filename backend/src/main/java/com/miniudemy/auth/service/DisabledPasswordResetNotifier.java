package com.miniudemy.auth.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev dışındaki profiller (#115): e-posta gönderimi yapılandırılmadığı için bağlantı kimseye ulaşmaz. Token ve
 * bağlantı log'a yazılmaz; yalnız isteğin geldiği bir uyarıyla görünür.
 */
@Component
@Profile("!dev")
class DisabledPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(DisabledPasswordResetNotifier.class);

    @Override
    public void sendResetLink(String email, String resetLink, Instant expiresAt) {
        log.warn("Parola yenileme istendi ama e-posta gönderimi yapılandırılmadı; bağlantı gönderilmedi.");
    }
}
