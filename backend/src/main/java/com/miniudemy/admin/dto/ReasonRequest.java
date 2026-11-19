package com.miniudemy.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Gerekçe isteyen yönetici işlemleri (docs/api/openapi.yaml › ReasonRequest): kurs reddi (#59) ve yayından kaldırma (#123). */
public record ReasonRequest(
        @NotBlank(message = "Gerekçe zorunludur.")
        @Size(min = 5, max = 1000, message = "Gerekçe 5–1000 karakter olmalıdır.")
        String reason) {
}
