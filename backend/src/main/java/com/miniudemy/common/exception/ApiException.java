package com.miniudemy.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Service katmanının fırlattığı, kullanıcıya gösterilebilir hataların atası.
 *
 * Her alt sınıf yalnız kendi HTTP durumunu belirler; {@link GlobalExceptionHandler} tek bir metotla
 * hepsini {@link ErrorResponse}'a çevirir. Yeni bir hata türü için yeni bir alt sınıf yazmak yeterlidir,
 * handler değişmez (Açık/Kapalı ilkesi).
 *
 * İsteğe bağlı {@code errors}: hatanın hangi alanla ilgili olduğu (ör. kayıtta {@code email} zaten varsa),
 * frontend'in mesajı ilgili form alanının altında gösterebilmesi için. Varsayılan boş.
 *
 * Mesaj kullanıcıya olduğu gibi gösterilir: Türkçe ve iç ayrıntı (SQL, sınıf adı, id listesi vb.) içermeyen
 * bir cümle olmalıdır.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errors;

    protected ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    protected ApiException(HttpStatus status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        // Map.copyOf kullanılmaz: sırayı karıştırır (alanlar formdaki sırayla dönmeli) ve null değerde
        // NullPointerException atıp 4xx hatayı 500'e çevirir. Sırayı koruyan, salt okunur kopya.
        this.errors = (errors == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
