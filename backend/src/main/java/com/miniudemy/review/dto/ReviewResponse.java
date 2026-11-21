package com.miniudemy.review.dto;

import java.time.Instant;

import com.miniudemy.auth.dto.UserSummary;

/**
 * Değerlendirme yanıtı (docs/api/openapi.yaml › ReviewResponse). {@code moderationReason} yalnız yönetici
 * yanıtlarında ({@code ReviewMapper.toAdminResponse}, #142) dolu olur; herkese açık ve kursiyer uçlarında her zaman null.
 */
public record ReviewResponse(
        Long id,
        Long courseId,
        UserSummary author,
        int rating,
        String comment,
        boolean hidden,
        String moderationReason,
        Instant createdAt,
        Instant updatedAt) {
}
