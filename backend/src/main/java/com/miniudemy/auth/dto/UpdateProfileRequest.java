package com.miniudemy.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

/**
 * Profil güncelleme (docs/api/openapi.yaml › UpdateProfileRequest, #116). Yalnız ad soyad ve kısa biyografi
 * değişir.
 *
 * {@code email} ve {@code role} bilerek bileşen olarak var: sözleşme bu alanlar istekte gelirse yok sayılmasını
 * değil, 400 ile reddedilmesini istiyor. Jackson bilinmeyen alanı sessizce atlar; alanı okuyup {@code @Null} ile
 * reddetmek, hatayı standart gövdede alan bazlı ({@code errors.email}, {@code errors.role}) verir.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Ad soyad zorunludur.")
        @Size(min = 2, max = 100, message = "Ad soyad 2–100 karakter olmalıdır.")
        String fullName,

        @Size(max = 500, message = "Biyografi en fazla 500 karakter olabilir.")
        String bio,

        @Null(message = "E-posta bu ekrandan değiştirilemez.")
        Object email,

        @Null(message = "Rol bu ekrandan değiştirilemez.")
        Object role) {
}
