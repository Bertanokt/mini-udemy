package com.miniudemy.certificate.domain;

import java.time.Instant;

/** Kursiyere sertifika verilince yayınlanır (Observer); yayıncısı {@code CertificateIssuingService} (#129). */
public record CertificateIssued(Long certificateId, Long studentId, Long courseId, Instant issuedAt) {
}
