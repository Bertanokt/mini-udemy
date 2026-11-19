package com.miniudemy.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Doğrulama kodu (#129): kısa, okunaklı alfabe, güvenli rastgele. */
class VerificationCodeGeneratorTest {

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator();

    @Test
    void code_isTenCharactersFromTheReadableAlphabet_groupedAsXXXX_XXXX_XX() {
        for (int i = 0; i < 1_000; i++) {
            String code = generator.next();
            assertThat(code).matches("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}"
                    + "-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{2}");
            assertThat(code.length()).isLessThanOrEqualTo(20); // verification_code varchar(20)
        }
    }

    @Test
    void alphabet_hasNoLookAlikeCharacters() {
        assertThat(VerificationCodeGenerator.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
        assertThat(VerificationCodeGenerator.ALPHABET.chars().distinct().count()).isEqualTo(31);
    }

    @Test
    void codes_doNotRepeat_andUseTheWholeAlphabet() {
        Set<String> codes = new HashSet<>();
        Set<Character> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            String code = generator.next();
            codes.add(code);
            code.replace("-", "").chars().forEach(c -> seen.add((char) c));
        }
        assertThat(codes).hasSize(10_000);
        assertThat(seen).hasSize(31);
    }
}
