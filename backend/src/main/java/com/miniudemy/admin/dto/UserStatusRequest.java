package com.miniudemy.admin.dto;

import jakarta.validation.constraints.NotNull;

/** Hesap durumu isteği (docs/api/openapi.yaml › UserStatusRequest). */
public record UserStatusRequest(@NotNull(message = "Bu alan zorunludur.") Boolean active) {
}
