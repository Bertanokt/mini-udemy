package com.miniudemy.enrollment.dto;

import java.time.Instant;

import com.miniudemy.course.dto.CourseSummaryResponse;

/**
 * Kayıt (docs/api/openapi.yaml › EnrollmentResponse). {@code lastLessonId} ve {@code lastAccessedAt} kursiyer hiç ders
 * izlememişse null.
 */
public record EnrollmentResponse(
        Long id,
        CourseSummaryResponse course,
        Instant enrolledAt,
        double progressPercent,
        Long lastLessonId,
        Instant lastAccessedAt) {
}
