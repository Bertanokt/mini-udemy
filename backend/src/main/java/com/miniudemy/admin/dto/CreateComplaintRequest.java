package com.miniudemy.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Şikâyet isteği (docs/api/openapi.yaml › CreateComplaintRequest). {@code courseId} ile {@code reviewId}'den tam biri
 * verilir; bu kural {@code Complaint}'in kurucusundadır.
 */
public record CreateComplaintRequest(
        Long courseId,
        Long reviewId,
        @NotBlank(message = "Gerekçe zorunludur.")
        @Size(min = 5, max = 1000, message = "Gerekçe 5–1000 karakter olmalıdır.")
        String reason) {
}
