package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 409 — istek mevcut durumla çakışıyor (mükerrer kayıt, izin verilmeyen durum geçişi).
 * Örnek: {@code throw new ConflictException("Bu kursa zaten kayıtlısınız.");}
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public ConflictException(String message, Map<String, String> errors) {
        super(HttpStatus.CONFLICT, message, errors);
    }
}
