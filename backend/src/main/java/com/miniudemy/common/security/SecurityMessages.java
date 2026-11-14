package com.miniudemy.common.security;

/**
 * Güvenlik hatalarının kullanıcıya gösterilen mesajları. docs/api/openapi.yaml › components.responses ›
 * {@code Unauthorized} ve {@code Forbidden} örnekleriyle birebir aynı olmalı.
 *
 * Hem güvenlik filtresindeki yanıtlar ({@link JsonSecurityErrorHandler}) hem de controller içinde oluşan
 * yetki hataları ({@code GlobalExceptionHandler}) bu sabitleri kullanır; aynı durum hangi katmanda oluşursa
 * oluşsun aynı mesaj döner.
 */
public final class SecurityMessages {

    /** 401: token yok, geçersiz ya da süresi dolmuş. */
    public static final String UNAUTHORIZED = "Oturumunuz geçersiz ya da süresi dolmuş. Lütfen tekrar giriş yapın.";

    /** 403: rol yetersiz. */
    public static final String FORBIDDEN = "Bu işlem için yetkiniz yok.";

    /** 403: hesap pasifleştirilmiş. */
    public static final String ACCOUNT_DISABLED = "Hesabınız devre dışı bırakılmış.";

    private SecurityMessages() {
    }
}
