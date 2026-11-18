package com.miniudemy.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Sınav gönderme sonucu: deneme özeti ve soru bazında doğru/yanlış (docs/api/openapi.yaml › AttemptResultResponse). */
public record AttemptResultResponse(Long id, Long quizId, BigDecimal score, BigDecimal maxScore, BigDecimal percent,
        boolean passed, Instant submittedAt, List<QuestionResultResponse> questions) {
}
