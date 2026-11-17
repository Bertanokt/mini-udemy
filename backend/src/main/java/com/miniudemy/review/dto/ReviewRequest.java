package com.miniudemy.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Değerlendirme oluşturma/güncelleme isteği (docs/api/openapi.yaml › ReviewRequest). */
public record ReviewRequest(
        @NotNull(message = "Puan zorunludur.")
        @Min(value = 1, message = "Puan 1–5 arasında olmalıdır.")
        @Max(value = 5, message = "Puan 1–5 arasında olmalıdır.")
        Integer rating,

        @Size(max = 2000, message = "Yorum en fazla 2000 karakter olabilir.")
        String comment) {
}
