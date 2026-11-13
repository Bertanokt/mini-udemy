package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 415 — yüklenen dosyanın türüne izin verilmiyor (içerikten tespit edilen tür).
 * Örnek: {@code throw new UnsupportedMediaTypeException("Yalnız mp4 ve webm videolar yüklenebilir.");}
 */
public class UnsupportedMediaTypeException extends ApiException {

    public UnsupportedMediaTypeException(String message) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public UnsupportedMediaTypeException(String message, Map<String, String> errors) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, errors);
    }
}
