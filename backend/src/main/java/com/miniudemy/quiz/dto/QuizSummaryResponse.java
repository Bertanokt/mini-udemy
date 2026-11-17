package com.miniudemy.quiz.dto;

import com.miniudemy.quiz.domain.ScoringType;

/** Bölüm sınavı özeti; soru içeriği yoktur (docs/api/openapi.yaml › QuizSummaryResponse). */
public record QuizSummaryResponse(Long id, Long sectionId, String title, ScoringType scoringType,
        int passThreshold, boolean required, int questionCount) {
}
