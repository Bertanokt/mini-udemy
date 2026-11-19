package com.miniudemy.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Yorum gizle / geri aç isteği (docs/api/openapi.yaml › ReviewVisibilityRequest). Gerekçe yalnız gizlerken
 * zorunludur; bu koşul alanlar arası olduğu için {@code Review.hide} denetler (400, {@code errors.reason}).
 */
public record ReviewVisibilityRequest(
        @NotNull(message = "Bu alan zorunludur.") Boolean hidden,
        @Size(max = 1000, message = "Gerekçe en fazla 1000 karakter olabilir.") String reason) {
}
