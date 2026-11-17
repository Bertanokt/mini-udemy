package com.miniudemy.quiz.dto;

import java.util.List;

/** Eğitmen görünümündeki soru (docs/api/openapi.yaml › QuestionResponse). */
public record QuestionResponse(Long id, Long courseId, String text, int points, List<OptionResponse> options) {
}
