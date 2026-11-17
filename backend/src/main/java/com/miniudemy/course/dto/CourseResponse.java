package com.miniudemy.course.dto;

import java.time.Instant;
import java.util.List;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;

/**
 * Kursun yönetim görünümü, eğitmen ve yönetici için (docs/api/openapi.yaml › CourseResponse). Entity'den yalnız
 * {@code CourseMapper} ile üretilir. Eğitmen, {@code auth} modülünden {@link UserSummary} olarak gelir.
 */
public record CourseResponse(
        Long id,
        String title,
        String description,
        CategoryResponse category,
        CourseLevel level,
        CourseStatus status,
        UserSummary instructor,
        String coverUrl,
        List<String> learningOutcomes,
        String rejectionReason,
        String unpublishReason,
        Instant createdAt,
        Instant updatedAt) {
}
