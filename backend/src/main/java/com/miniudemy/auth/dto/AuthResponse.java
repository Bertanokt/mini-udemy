package com.miniudemy.auth.dto;

/**
 * Başarılı giriş (ve kayıt) yanıtı (docs/api/openapi.yaml › AuthResponse).
 *
 * @param accessToken imzalı JWT
 * @param tokenType   her zaman {@value #BEARER}
 * @param expiresIn   token'ın geçerlilik süresi (saniye)
 * @param user        giriş yapan kullanıcı
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static final String BEARER = "Bearer";

    public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, BEARER, expiresIn, user);
    }
}
