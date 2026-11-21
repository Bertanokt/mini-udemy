package com.miniudemy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token ile yeni parola (docs/api/openapi.yaml › PasswordResetConfirmRequest, #115). Parola kuralı kayıttakiyle
 * aynı: 8–72 karakter; BCrypt'in 72 bayt sınırını servis denetler.
 */
public record PasswordResetConfirmRequest(
        @NotBlank(message = "Bağlantı geçersiz ya da süresi dolmuş. Yeni bir bağlantı isteyin.")
        @Size(max = 255, message = "Bağlantı geçersiz ya da süresi dolmuş. Yeni bir bağlantı isteyin.")
        String token,

        @NotBlank(message = "Parola zorunludur.")
        @Size(min = 8, message = "Parola en az 8 karakter olmalıdır.")
        @Size(max = 72, message = "Parola en fazla 72 karakter olabilir.")
        String newPassword) {
}
