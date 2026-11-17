package com.miniudemy.quiz.dto;

import java.util.List;

import com.miniudemy.quiz.domain.ScoringType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sınav oluşturma/güncelleme isteği (docs/api/openapi.yaml › QuizRequest). Soruların aynı kursun bankasından
 * olması ve tekrar etmemesi alan doğrulamasıyla ifade edilemediği için serviste denetlenir.
 */
public record QuizRequest(
        @NotBlank(message = "Sınav başlığı zorunludur.")
        @Size(max = 200, message = "Sınav başlığı en fazla 200 karakter olabilir.")
        String title,

        @NotNull(message = "Puanlama tipi zorunludur.")
        ScoringType scoringType,

        @NotNull(message = "Başarı eşiği zorunludur.")
        @Min(value = 0, message = "Başarı eşiği 0–100 arasında olmalıdır.")
        @Max(value = 100, message = "Başarı eşiği 0–100 arasında olmalıdır.")
        Integer passThreshold,

        @NotNull(message = "Sertifika için zorunlu olup olmadığı belirtilmelidir.")
        Boolean required,

        @NotEmpty(message = "Sınavda en az bir soru olmalıdır.")
        List<@NotNull(message = "Soru kimliği boş olamaz.") Long> questionIds) {
}
