-- Sertifika (ER: certificates, #129). Kursiyer ve kurs kaydın (enrollment) kendisidir: kayıt başına tek sertifika.
-- Doğrulama kodu tekil; herkese açık doğrulama (#130) bu kodla yapılır.
CREATE TABLE certificates (
    id                BIGSERIAL   PRIMARY KEY,
    enrollment_id     BIGINT      NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    verification_code VARCHAR(20) NOT NULL,
    issued_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_certificates_enrollment UNIQUE (enrollment_id),
    CONSTRAINT uk_certificates_verification_code UNIQUE (verification_code)
);
