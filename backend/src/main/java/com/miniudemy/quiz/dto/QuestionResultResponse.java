package com.miniudemy.quiz.dto;

import java.math.BigDecimal;

/** Soru bazında sonuç (docs/api/openapi.yaml › AttemptResultResponse.questions). */
public record QuestionResultResponse(Long questionId, boolean correct, BigDecimal earnedPoints, BigDecimal maxPoints) {
}
