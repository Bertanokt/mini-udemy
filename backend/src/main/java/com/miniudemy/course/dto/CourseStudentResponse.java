package com.miniudemy.course.dto;

import java.time.Instant;
import java.util.List;

import com.miniudemy.auth.dto.UserSummary;

/**
 * Eğitmenin gördüğü kursiyer satırı (docs/api/openapi.yaml › CourseStudentResponse); e-posta yok. {@code lastAccessedAt}
 * kursiyer hiçbir ders açmadıysa null.
 */
public record CourseStudentResponse(
        UserSummary student,
        Instant enrolledAt,
        double progressPercent,
        Instant lastAccessedAt,
        List<QuizResult> quizResults) {

    /** Sınav başına en iyi sonuç; denemesi olmayan sınav listede yoktur. */
    public record QuizResult(Long quizId, double bestPercent, boolean passed) {
    }
}
