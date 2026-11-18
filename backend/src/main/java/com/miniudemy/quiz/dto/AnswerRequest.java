package com.miniudemy.quiz.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Bir sorunun cevabı: seçilen şıklar (docs/api/openapi.yaml › SubmitAttemptRequest). */
public record AnswerRequest(
        @NotNull(message = "Soru kimliği zorunludur.")
        Long questionId,

        @NotEmpty(message = "Her cevapta en az bir şık seçilmelidir.")
        List<@NotNull(message = "Şık kimliği boş olamaz.") Long> optionIds) {
}
