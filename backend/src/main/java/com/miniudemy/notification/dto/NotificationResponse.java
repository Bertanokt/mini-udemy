package com.miniudemy.notification.dto;

import java.time.Instant;

import com.miniudemy.notification.domain.NotificationType;

/**
 * Bildirim yanıtı (docs/api/openapi.yaml › NotificationResponse). {@code read}, bildirimin okunma zamanının dolu
 * olmasıdır; zamanın kendisi dışarı verilmez.
 *
 * @param link ön yüzde açılacak yol; yoksa null
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        String link,
        boolean read,
        Instant createdAt) {
}
