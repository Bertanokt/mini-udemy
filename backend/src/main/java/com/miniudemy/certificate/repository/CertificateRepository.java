package com.miniudemy.certificate.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.certificate.domain.Certificate;

/** `Certificate` kalıcılığı. */
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    boolean existsByEnrollmentId(Long enrollmentId);

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    /**
     * Sertifikayı ekler; kayıt için zaten sertifika varsa ya da kod başka sertifikada kullanılmışsa hiçbir şey yapmaz
     * (#129). Eklenen satır sayısını döner. Tekillik veritabanındaki iki kısıtla tek komutta sağlanır; aynı anda gelen
     * iki olayda biri 1, diğeri 0 alır ve istisna oluşmadığı için transaction bozulmaz.
     */
    @Modifying
    @Query(value = "insert into certificates (enrollment_id, verification_code, issued_at) "
            + "values (:enrollmentId, :code, :issuedAt) on conflict do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("enrollmentId") Long enrollmentId, @Param("code") String code,
            @Param("issuedAt") Instant issuedAt);
}
