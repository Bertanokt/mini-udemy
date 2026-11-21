package com.miniudemy.certificate.dto;

import java.time.Instant;

/** Kursiyerin kendi sertifikası (docs/api/openapi.yaml › CertificateResponse, #130). */
public record CertificateResponse(
        Long id,
        Long courseId,
        String courseTitle,
        String studentName,
        String instructorName,
        String verificationCode,
        Instant issuedAt) {
}
