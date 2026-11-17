package com.miniudemy.course.dto;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;

/** Katalog kartı (docs/api/openapi.yaml › CourseSummaryResponse). */
public record CourseSummaryResponse(
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
        int reviewCount) {
}
