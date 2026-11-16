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

    /**
     * Tek bir alanın, @Valid ile yakalanamayan doğrulama hatası. Gövde, @Valid ihlalleriyle aynı biçimde döner:
     * genel mesaj "Girilen bilgilerde hata var.", ayrıntı {@code errors.<alan>}'da.
     * Örnek: {@code throw BadRequestException.forField("role", "Bu rol ile kayıt olunamaz.");}
     */
    public static BadRequestException forField(String field, String fieldMessage) {
        return new BadRequestException(GlobalExceptionHandler.VALIDATION_MESSAGE, Map.of(field, fieldMessage));
    }
}
