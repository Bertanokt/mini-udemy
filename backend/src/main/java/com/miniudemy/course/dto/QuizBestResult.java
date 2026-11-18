package com.miniudemy.course.dto;

/**
 * Bir kursiyerin bir sınavdaki en iyi sonucu (#134). {@code bestPercent}: en yüksek yüzde (bir ondalık);
 * {@code passed}: denemelerinden herhangi biri eşiği geçtiyse.
 */
public record QuizBestResult(Long quizId, double bestPercent, boolean passed) {
}
