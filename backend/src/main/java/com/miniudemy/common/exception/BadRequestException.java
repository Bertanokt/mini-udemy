package com.miniudemy.common.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * 400 — alan doğrulamasıyla (@Valid) yakalanamayan iş kuralı ihlali.
 * Örnek: {@code throw new BadRequestException("Her soruda en az bir doğru şık olmalıdır.");}
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    /** Hatanın ilgili olduğu alanlarla birlikte (ör. {@code Map.of("email", "Bu e-posta adresi zaten kayıtlı.")}). */
    public BadRequestException(String message, Map<String, String> errors) {
        super(HttpStatus.BAD_REQUEST, message, errors);
    }
}
