package com.miniudemy.admin.dto;

import java.time.Instant;

import com.miniudemy.admin.domain.ComplaintStatus;
import com.miniudemy.auth.dto.UserSummary;

/** Şikâyet yanıtı (docs/api/openapi.yaml › ComplaintResponse). {@code courseId} ile {@code reviewId}'den biri dolu. */
public record ComplaintResponse(
        Long id,
        UserSummary reporter,
        Long courseId,
        Long reviewId,
        String reason,
        ComplaintStatus status,
        Instant createdAt,
        Instant resolvedAt) {
}
