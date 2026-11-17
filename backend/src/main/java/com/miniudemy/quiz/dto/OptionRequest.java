package com.miniudemy.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Şık isteği (docs/api/openapi.yaml › OptionRequest). */
public record OptionRequest(
        @NotBlank(message = "Şık metni zorunludur.")
        @Size(max = 500, message = "Şık metni en fazla 500 karakter olabilir.")
        String text,

        @NotNull(message = "Şıkkın doğru olup olmadığı belirtilmelidir.")
        Boolean correct) {
}
