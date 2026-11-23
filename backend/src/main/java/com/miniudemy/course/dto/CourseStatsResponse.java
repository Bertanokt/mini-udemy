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
        List<ProgressBucket> progressDistribution,
        List<QuizStats> quizzes,
        RatingStats rating) {

    /**
     * Bütün kursun kayıtlarından ilerleme kovası: yüzdesi {@code minPercent}–{@code maxPercent} (dahil) aralığında olan
     * kayıt sayısı. Kovalar 0–24, 25–49, 50–74, 75–99, 100; toplam {@code enrollmentCount}'a eşittir.
     */
    public record ProgressBucket(int minPercent, int maxPercent, long count) {
    }

    /** {@code passRate}: geçen denemelerin tüm denemelere oranı (yüzde); deneme yoksa 0. */
    public record QuizStats(Long quizId, String title, long attemptCount, double passRate) {
    }

    /** Ortalama puan (bir ondalık, yorum yoksa null), görünür yorum sayısı ve "1"–"5" dağılımı. */
    public record RatingStats(Double averageRating, long reviewCount, Map<String, Long> distribution) {
    }
}
