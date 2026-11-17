package com.miniudemy.quiz.dto;

import java.util.List;

import com.miniudemy.quiz.domain.ScoringType;

/**
 * Öğrenci görünümündeki sınav (docs/api/openapi.yaml › StudentQuizResponse). Şıklarda doğru cevap bilgisi bilerek
 * yoktur; bu bilgiyi taşıyan tek DTO {@link InstructorQuizResponse}'tur.
 */
public record StudentQuizResponse(Long id, Long sectionId, String title, ScoringType scoringType,
        int passThreshold, boolean required, int questionCount, List<StudentQuestionResponse> questions) {
}
