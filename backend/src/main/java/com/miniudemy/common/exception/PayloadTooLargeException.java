package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 413 — yüklenen dosya türü için izin verilen boyutu aşıyor.
 * Örnek: {@code throw new PayloadTooLargeException("Video en fazla 200 MB olabilir.");}
 */
public class PayloadTooLargeException extends ApiException {

    public PayloadTooLargeException(String message) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public PayloadTooLargeException(String message, Map<String, String> errors) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, message, errors);
    }
}
