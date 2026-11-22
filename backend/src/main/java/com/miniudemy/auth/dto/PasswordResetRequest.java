package com.miniudemy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Parola yenileme bağlantısı isteği (docs/api/openapi.yaml › PasswordResetRequest, #115). */
public record PasswordResetRequest(
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 254, message = "E-posta en fazla 254 karakter olabilir.")
        String email) {
}
