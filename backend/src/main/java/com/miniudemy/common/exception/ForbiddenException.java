package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 403 — kullanıcı giriş yapmış ama bu işlemi yapmaya yetkisi yok (ör. başkasının kursu).
 * Örnek: {@code throw new ForbiddenException("Bu kursu düzenleme yetkiniz yok.");}
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public ForbiddenException(String message, Map<String, String> errors) {
        super(HttpStatus.FORBIDDEN, message, errors);
    }
}
