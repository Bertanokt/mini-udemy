package com.miniudemy.auth.dto;

import java.time.Instant;

import com.miniudemy.auth.domain.Role;

/**
 * Kullanıcının API'ye çıkan yüzü (docs/api/openapi.yaml › UserResponse).
 *
 * Kayıt, `GET /api/me`, `PUT /api/me` ve yöneticinin durum değiştirme yanıtında kullanılır. Parola özeti,
 * hesap durumu ve güncelleme zamanı bilerek yoktur; entity'den yalnız {@code UserMapper} ile üretilir.
 */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        String bio,
        Instant createdAt) {
}
