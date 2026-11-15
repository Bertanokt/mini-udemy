package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 401 — kimlik doğrulanamadı; şu an yalnız girişte e-posta ya da parola yanlış olduğunda.
 * Örnek: {@code throw new UnauthorizedException(SecurityMessages.INVALID_CREDENTIALS);}
 *
 * Token yok, geçersiz ya da süresi dolmuş durumundaki 401'i bu sınıf değil, güvenlik filtresi
 * ({@code JsonSecurityErrorHandler}) üretir.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte. */
    public UnauthorizedException(String message, Map<String, String> errors) {
        super(HttpStatus.UNAUTHORIZED, message, errors);
    }
}
