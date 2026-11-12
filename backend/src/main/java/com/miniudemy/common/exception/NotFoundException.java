package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 404 — istenen kaynak yok ya da kullanıcıya görünür değil.
 * Örnek: {@code throw new NotFoundException("Kurs bulunamadı.");}
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public NotFoundException(String message, Map<String, String> errors) {
        super(HttpStatus.NOT_FOUND, message, errors);
    }
}
