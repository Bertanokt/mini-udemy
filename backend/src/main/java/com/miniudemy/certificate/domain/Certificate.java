package com.miniudemy.certificate.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Kursiyere bir kurs için verilen platform içi sertifika (#129). Tablo: `certificates`. Kursiyer ve kurs, kaydın
 * (enrollment) kendisidir; kayıt başka modülün entity'si olduğu için yalnız id olarak tutulur. Kayıt başına tek
 * sertifika ve tekil doğrulama kodu veritabanında kısıtla sağlanır.
 */
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private Long enrollmentId;

    @Column(name = "verification_code", nullable = false, updatable = false, length = 20)
    private String verificationCode;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    /** JPA için. */
    protected Certificate() {
    }

    public Long getId() {
        return id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
