package com.miniudemy.quiz.dto;

import java.util.List;

/** Öğrenci görünümündeki soru; doğru cevap bilgisi içermez. */
public record StudentQuestionResponse(Long id, String text, int points, List<StudentOptionResponse> options) {
}
