package com.miniudemy.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bölüm ekleme ve yeniden adlandırma isteği (docs/api/openapi.yaml › SectionRequest). */
public record SectionRequest(
        @NotBlank(message = "Bölüm başlığı zorunludur.")
        @Size(max = 200, message = "Bölüm başlığı en fazla 200 karakter olabilir.")
        String title) {
}
