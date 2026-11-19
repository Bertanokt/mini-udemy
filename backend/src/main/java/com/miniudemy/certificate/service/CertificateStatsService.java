package com.miniudemy.certificate.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.certificate.repository.CertificateRepository;

/** Sertifika sayısı (platform raporu, #145). */
@Service
@Transactional(readOnly = true)
public class CertificateStatsService {

    private final CertificateRepository certificates;

    public CertificateStatsService(CertificateRepository certificates) {
        this.certificates = certificates;
    }

    public long countAll() {
        return certificates.count();
    }
}
