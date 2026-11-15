package com.miniudemy.auth.domain;

/**
 * Kullanıcı rolü. Değerler `users.role` kolonundaki CHECK kısıtıyla ({@code ck_users_role}) ve
 * OpenAPI'deki `Role` şemasıyla aynıdır; birine değer eklenirse diğerleri de aynı PR'da güncellenir.
 *
 * {@code ADMIN} kayıtla oluşturulamaz, yalnız seed ile gelir (FR-01).
 */
public enum Role {
    STUDENT,
    INSTRUCTOR,
    ADMIN
}
