package com.miniudemy.quiz.domain;

import java.time.Instant;

/** Kursiyer bir sınav denemesini bitirince yayınlanır (Observer). Yayıncı: QuizAttemptService.submit. */
public record QuizCompleted(Long attemptId, Long quizId, Long courseId, Long studentId, boolean passed, Instant completedAt) {
}
