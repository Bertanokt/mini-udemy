package com.miniudemy.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Controller'lardan fırlayan tüm istisnaları {@link ErrorResponse} gövdesine çevirir.
 *
 * Kurallar (CLAUDE.md › Standart hata formatı):
 * <ul>
 *   <li>Controller'da elle hata yanıtı kurulmaz; service {@link ApiException} alt sınıflarından birini fırlatır.</li>
 *   <li>{@code message} kullanıcıya gösterilebilir Türkçe bir cümledir; beklenmeyen hatalarda iç ayrıntı
 *       yanıta değil yalnız sunucu loguna yazılır.</li>
 * </ul>
 *
 * Kapsam dışı: kimlik doğrulama (401) ve Spring Security'nin erişim reddi, istek controller'a ulaşmadan
 * filtrede oluşur ve bu sınıfa gelmez. Onlar #10'da aynı {@link ErrorResponse} gövdesiyle yazılacak.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** docs/api/openapi.yaml › responses › BadRequest örnekleriyle aynı olmalı. */
    static final String VALIDATION_MESSAGE = "Girilen bilgilerde hata var.";
    static final String UNEXPECTED_MESSAGE = "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.";

    /** Kendi istisnalarımız: durum kodu ve varsa alan hataları istisnanın kendisinden gelir (400, 403, 404, 409, 413, 415). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return respond(ex.getStatus(), ex.getMessage(), ex.getErrors());
    }

    /** İstek gövdesindeki @Valid ihlalleri: 400 + alan bazlı hata haritası. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Bir alanın birden fazla ihlali varsa ilk mesaj kalır.
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, errors);
    }

    /** Parametre/yol değişkeni doğrulama ihlalleri (Spring 6.1+): 400 + parametre bazlı hata haritası. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().stream().findFirst()
                    .ifPresent(err -> errors.putIfAbsent(name, err.getDefaultMessage()));
        });
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, errors);
    }

    /** Okunamayan istek gövdesi (bozuk JSON, yanlış alan tipi): 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı; JSON biçimini kontrol edin.", Map.of());
    }

    /** Yanlış tipte parametre (ör. sayı beklenen yerde metin): 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, Map.of(ex.getName(), "Geçersiz değer."));
    }

    /** Eksik zorunlu parametre: 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_MESSAGE, Map.of(ex.getParameterName(), "Zorunlu alan."));
    }

    /** Olmayan adres: 404. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoRoute(Exception ex) {
        return respond(HttpStatus.NOT_FOUND, "Aradığınız kaynak bulunamadı.", Map.of());
    }

    /** Adres var ama HTTP yöntemi desteklenmiyor: 405 + Allow başlığı. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(status).headers(headers)
                .body(ErrorResponse.of(status.value(), "Bu adres için " + ex.getMethod() + " yöntemi desteklenmiyor."));
    }

    /** Spring'in genel yükleme sınırı aşıldı (spring.servlet.multipart.max-file-size): 413. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "Dosya boyutu izin verilen sınırı aşıyor.", Map.of());
    }

    /** İsteğin Content-Type'ı desteklenmiyor (ör. JSON beklenen uca düz metin): 415. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Desteklenmeyen içerik türü.", Map.of());
    }

    /** Yukarıdakilerin dışındaki her şey: 500. Ayrıntı yanıta değil, loga yazılır. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_MESSAGE, Map.of());
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message, errors));
    }
}
