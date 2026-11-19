package com.miniudemy.certificate.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Sertifika doğrulama kodu üretir (#129): kısa, okunması ve yazılması kolay, tahmin edilemez.
 *
 * Biçim {@code XXXX-XXXX-XX}: 10 karakter, kriptografik rastgele ({@link SecureRandom}). Alfabe karıştırılabilen
 * karakterleri içermez (0/O, 1/I/L): 31 karakter, yani 31^10 ≈ 8.2 × 10^14 olasılık (~49 bit).
 */
@Component
public class VerificationCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 10;

    private final SecureRandom random;

    public VerificationCodeGenerator() {
        this(new SecureRandom());
    }

    VerificationCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH + 2);
        for (int i = 0; i < LENGTH; i++) {
            if (i == 4 || i == 8) {
                code.append('-');
            }
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
