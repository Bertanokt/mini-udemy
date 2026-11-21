package com.miniudemy.notification.dto;

import java.util.List;

/** Sayfalı bildirim listesi (docs/api/openapi.yaml › NotificationPage). {@code page} 0'dan başlar. */
public record NotificationPage(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
