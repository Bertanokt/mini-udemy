package com.miniudemy.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tüm hata yanıtlarının ortak gövdesi (docs/api/openapi.yaml › ErrorResponse).
 *
 * <ul>
 *   <li>{@code status}: HTTP durum kodu</li>
 *   <li>{@code message}: kullanıcıya gösterilebilir Türkçe cümle; stack trace ya da iç ayrıntı içermez</li>
 *   <li>{@code errors}: alan adı → hata mesajı; doğrulama dışındaki hatalarda boş nesne, hiçbir zaman null değil</li>
 * </ul>
 *
 * Spring Security'nin 401/403 yanıtları da (#10) bu gövdeyi kullanmalıdır.
 */
public record ErrorResponse(int status, String message, Map<String, String> errors) {

    public ErrorResponse {
        // Sırayı korumak için LinkedHashMap; dışarıdan değiştirilemesin diye salt okunur.
        errors = (errors == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Map.of());
    }

    public static ErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ErrorResponse(status, message, errors);
    }
}
