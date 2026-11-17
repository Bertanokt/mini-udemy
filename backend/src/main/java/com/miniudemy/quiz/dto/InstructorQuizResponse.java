package com.miniudemy.quiz.dto;

import java.util.List;

import com.miniudemy.quiz.domain.ScoringType;

/**
 * Eğitmen görünümündeki sınav; soruların şıklarıyla birlikte doğru bilgisini içerir
 * (docs/api/openapi.yaml › InstructorQuizResponse = QuizSummaryResponse + questions).
 */
public record InstructorQuizResponse(Long id, Long sectionId, String title, ScoringType scoringType,
        int passThreshold, boolean required, int questionCount, List<QuestionResponse> questions) {
}
