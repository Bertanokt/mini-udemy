package com.miniudemy.quiz.domain;

import java.time.Instant;

/** Kursiyer bir sınav denemesini bitirince yayınlanır (Observer). Yayıncısı sınav çözme issue'sunda eklenir. */
public record QuizCompleted(Long attemptId, Long quizId, Long courseId, Long studentId, boolean passed, Instant completedAt) {
}
