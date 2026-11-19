package com.miniudemy.course.dto;

/** Bir sınavın tüm denemeleri: toplam ve geçen sayısı (eğitmen istatistikleri, #133). */
public record QuizAttemptStats(Long quizId, String title, long attemptCount, long passedCount) {
}
