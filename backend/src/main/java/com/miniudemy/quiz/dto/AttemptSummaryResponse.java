package com.miniudemy.quiz.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Deneme özeti (docs/api/openapi.yaml › AttemptSummaryResponse). */
public record AttemptSummaryResponse(Long id, Long quizId, BigDecimal score, BigDecimal maxScore, BigDecimal percent,
        boolean passed, Instant submittedAt) {
}
