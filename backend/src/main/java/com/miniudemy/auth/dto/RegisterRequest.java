package com.miniudemy.auth.dto;

import com.miniudemy.auth.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Kayıt isteği (docs/api/openapi.yaml › RegisterRequest).
 *
 * {@code role} bilerek {@link Role} tipinde: {@code ADMIN} de JSON'dan okunabilsin ki servis onu alan bazlı bir 400
 * ile ({@code errors.role}) reddedebilsin. Yalnız STUDENT/INSTRUCTOR içeren ayrı bir enum olsaydı {@code "ADMIN"}
 * gövdenin okunamamasına, yani alan hatası olmayan genel bir 400'e yol açardı.
 *
 * Parolanın 72 karakter sınırı karakter sayar; BCrypt'in sınırı ise 72 bayttır. Bayt sınırını servis denetler.
 */
public record RegisterRequest(
        @NotBlank(message = "Ad soyad zorunludur.")
        @Size(min = 2, max = 100, message = "Ad soyad 2–100 karakter olmalıdır.")
        String fullName,

        @NotBlank(message = "E-posta zorunludur.")
        @Email(message = "Geçerli bir e-posta adresi girin.")
        @Size(max = 254, message = "E-posta en fazla 254 karakter olabilir.")
        String email,

        @NotBlank(message = "Parola zorunludur.")
        @Size(min = 8, message = "Parola en az 8 karakter olmalıdır.")
        @Size(max = 72, message = "Parola en fazla 72 karakter olabilir.")
        String password,

        @NotNull(message = "Rol zorunludur.")
        Role role) {

    /** {@code toString} parolayı loga ya da hata mesajına yazmasın. */
    @Override
    public String toString() {
        return "RegisterRequest[fullName=" + fullName + ", email=" + email + ", password=***, role=" + role + "]";
    }
}
