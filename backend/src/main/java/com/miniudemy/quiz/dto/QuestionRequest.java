package com.miniudemy.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Soru oluşturma/güncelleme isteği (docs/api/openapi.yaml › QuestionRequest). "En az bir doğru şık" kuralı
 * alan doğrulamasıyla ifade edilemediği için serviste denetlenir.
 */
public record QuestionRequest(
        @NotBlank(message = "Soru metni zorunludur.")
        @Size(min = 3, max = 2000, message = "Soru metni 3–2000 karakter olmalıdır.")
        String text,

        @NotNull(message = "Puan zorunludur.")
        @Min(value = 1, message = "Puan en az 1 olmalıdır.")
        @Max(value = 100, message = "Puan en fazla 100 olabilir.")
        Integer points,

        @NotNull(message = "Şıklar zorunludur.")
        @Size(min = 2, max = 10, message = "Bir soruda 2–10 şık olmalıdır.")
        List<@NotNull(message = "Şık boş olamaz.") @Valid OptionRequest> options) {
}
