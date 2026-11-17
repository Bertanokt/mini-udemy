package com.miniudemy.common.security;

import org.springframework.security.core.Authentication;

/**
 * Giriş yapmış kullanıcının id'si. Controller'lar istekteki kullanıcıyı yalnız buradan alır:
 * {@code Long userId = CurrentUser.id(authentication);}
 *
 * Kimliği {@link JwtAccountAuthenticationConverter} kurar; {@code Authentication#getName()} token'daki {@code sub},
 * yani kullanıcı id'sidir. Converter, kullanıcının veritabanında var ve aktif olduğunu zaten doğrulamıştır.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
