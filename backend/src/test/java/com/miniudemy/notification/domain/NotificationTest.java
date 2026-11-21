package com.miniudemy.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/** Okundu işaretleme (#125): yeni bildirim okunmamıştır; işaretleme idempotenttir, ilk okunma zamanı korunur. */
class NotificationTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T09:00:00Z");

    private final Notification notification = new Notification(42L,
            new NotificationContent(NotificationType.CERTIFICATE_ISSUED, "Sertifikanız hazır", "Gövde", null),
            CREATED);

    @Test
    void newNotification_isUnread() {
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    void markRead_setsReadAt() {
        notification.markRead(CREATED.plusSeconds(60));

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(CREATED.plusSeconds(60));
    }

    @Test
    void markRead_twice_keepsTheFirstReadTime() {
        notification.markRead(CREATED.plusSeconds(60));
        notification.markRead(CREATED.plusSeconds(120));

        assertThat(notification.getReadAt()).isEqualTo(CREATED.plusSeconds(60));
    }
}
