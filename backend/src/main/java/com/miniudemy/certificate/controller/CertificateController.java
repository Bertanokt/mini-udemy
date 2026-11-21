package com.miniudemy.certificate.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.certificate.dto.CertificateResponse;
import com.miniudemy.certificate.dto.CertificateVerificationResponse;
import com.miniudemy.certificate.service.CertificateQueryService;
import com.miniudemy.common.security.CurrentUser;

/** Sertifikalarım ve herkese açık doğrulama (#130; sözleşme › Certificates). */
@RestController
public class CertificateController {

    private final CertificateQueryService certificates;

    public CertificateController(CertificateQueryService certificates) {
        this.certificates = certificates;
    }

    /** Kursiyerin sertifikaları, en yeni önce; yalnız STUDENT. */
    @GetMapping("/api/me/certificates")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CertificateResponse> listMine(Authentication authentication) {
        return certificates.listMine(CurrentUser.id(authentication));
    }

    /** Kursiyerin bir sertifikası; başkasınınsa 404. */
    @GetMapping("/api/me/certificates/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public CertificateResponse getMine(@PathVariable Long id, Authentication authentication) {
        return certificates.getMine(id, CurrentUser.id(authentication));
    }

    /** Herkese açık doğrulama (girişsiz, {@code SecurityConfig.PUBLIC_GET}); geçersiz kod 404. */
    @GetMapping("/api/certificates/verify/{code}")
    public CertificateVerificationResponse verify(@PathVariable String code) {
        return certificates.verify(code);
    }
}
