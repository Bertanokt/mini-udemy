package com.miniudemy.certificate.domain;

import java.time.Instant;

/** Kursiyere sertifika verilince yayınlanır (Observer). Yayıncısı sertifika issue'sunda eklenir. */
public record CertificateIssued(Long certificateId, Long studentId, Long courseId, Instant issuedAt) {
}
