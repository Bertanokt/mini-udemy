package com.miniudemy.course.dto;

import java.util.List;
import java.util.Map;

/**
 * Eğitmen paneli kurs istatistikleri (docs/api/openapi.yaml › CourseStatsResponse). Oranlar ve yüzdeler iki
 * ondalıktır; kayıt yoksa hepsi 0.
 */
public record CourseStatsResponse(
        Long courseId,
        long enrollmentCount,
        double averageProgressPercent,
        double completionRate,
        List<QuizStats> quizzes,
        RatingStats rating) {

    /** {@code passRate}: geçen denemelerin tüm denemelere oranı (yüzde); deneme yoksa 0. */
    public record QuizStats(Long quizId, String title, long attemptCount, double passRate) {
    }

    /** Ortalama puan (bir ondalık, yorum yoksa null), görünür yorum sayısı ve "1"–"5" dağılımı. */
    public record RatingStats(Double averageRating, long reviewCount, Map<String, Long> distribution) {
    }
}
