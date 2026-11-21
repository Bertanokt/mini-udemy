package com.miniudemy.certificate.dto;

import java.time.Instant;

/**
 * Herkese açık doğrulama yanıtı (docs/api/openapi.yaml › CertificateVerificationResponse, #130): yalnız ad, kurs,
 * tarih ve geçerlilik. E-posta, kullanıcı ya da kayıt id'si gibi bilgiler bu tipte yoktur.
 */
public record CertificateVerificationResponse(
        boolean valid,
        String courseTitle,
        String studentName,
        Instant issuedAt) {
}
