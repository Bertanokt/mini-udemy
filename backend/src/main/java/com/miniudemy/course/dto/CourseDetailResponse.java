package com.miniudemy.course.dto;

import java.util.List;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;

/**
 * Kurs detayı (docs/api/openapi.yaml › CourseDetailResponse): özet alanları artı açıklama, öğrenme hedefleri ve bölümler.
 * Sözleşmedeki {@code allOf} Java'da kalıtımla değil alanların tekrarıyla karşılanır (record'lar genişletilemez).
 */
public record CourseDetailResponse(
        Long id,
        String title,
        CategoryResponse category,
        CourseLevel level,
        CourseStatus status,
        UserSummary instructor,
        String coverUrl,
        int lessonCount,
        int totalDurationSeconds,
        long enrollmentCount,
        Double averageRating,
        int reviewCount,
        String description,
        List<String> learningOutcomes,
        List<SectionResponse> sections) {
}
