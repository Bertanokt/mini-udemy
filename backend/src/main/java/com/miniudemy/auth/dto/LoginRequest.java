package com.miniudemy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Giriş isteği (docs/api/openapi.yaml › LoginRequest).
 *
 * Parolada yalnız üst sınır var: en az 8 karakter kuralı kayıtta (#12) uygulanır. Girişte bu kural tekrar
 * edilmez; kısa bir parola yalnız 401 alır, ayrı bir doğrulama hatası parola kuralını ele vermez.
 */
public record LoginRequest(
        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 254, message = "E-posta en fazla 254 karakter olabilir.")
        String email,

        @NotBlank(message = "Parola zorunludur.")
        @Size(max = 72, message = "Parola en fazla 72 karakter olabilir.")
        String password) {

    /** {@code toString} parolayı loga ya da hata mesajına yazmasın. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
