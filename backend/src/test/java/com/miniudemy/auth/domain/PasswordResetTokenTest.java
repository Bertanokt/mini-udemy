package com.miniudemy.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Token süre ve kullanım kuralı (#115). */
class PasswordResetTokenTest {

    private static final Instant CREATED = Instant.parse("2026-11-21T10:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-11-21T10:30:00Z");

    private final PasswordResetToken token = new PasswordResetToken(42L, "a".repeat(64), CREATED, EXPIRES);

    @Test
    void newToken_isUsableUntilJustBeforeExpiry() {
        assertThat(token.isUsableAt(CREATED)).isTrue();
        assertThat(token.isUsableAt(EXPIRES.minusMillis(1))).isTrue();
    }

    @Test
    void expiredToken_isNotUsable_fromTheExpiryInstantOn() {
        assertThat(token.isUsableAt(EXPIRES)).isFalse();
        assertThat(token.isUsableAt(EXPIRES.plusSeconds(1))).isFalse();
    }

    @Test
    void usedToken_isNotUsable_evenBeforeExpiry() {
        ReflectionTestUtils.setField(token, "usedAt", CREATED.plusSeconds(60));

        assertThat(token.isUsableAt(CREATED.plusSeconds(120))).isFalse();
    }
}
