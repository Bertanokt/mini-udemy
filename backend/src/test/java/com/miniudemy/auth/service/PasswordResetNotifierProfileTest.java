package com.miniudemy.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Bağlantıyı log'a yazan bildirici yalnız dev profilinde etkin; diğer profillerde token log'a düşmez (#115).
 */
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetNotifierProfileTest {

    private static final String LINK = "http://localhost:3000/reset-password?token=GIZLI-TOKEN-123";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LoggingPasswordResetNotifier.class, DisabledPasswordResetNotifier.class);

    @Test
    void devProfile_usesTheLoggingNotifier_whichWritesTheLink(CapturedOutput output) {
        runner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            assertThat(context).hasSingleBean(PasswordResetNotifier.class);
            assertThat(context.getBean(PasswordResetNotifier.class)).isInstanceOf(LoggingPasswordResetNotifier.class);
            context.getBean(PasswordResetNotifier.class).sendResetLink("a@example.com", LINK, Instant.EPOCH);
        });
        assertThat(output).contains(LINK);
    }

    @Test
    void prodAndTestProfiles_useTheDisabledNotifier_whichNeverWritesTheToken(CapturedOutput output) {
        for (String profile : new String[] {"prod", "test"}) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context).hasSingleBean(PasswordResetNotifier.class);
                assertThat(context.getBean(PasswordResetNotifier.class))
                        .isInstanceOf(DisabledPasswordResetNotifier.class);
                context.getBean(PasswordResetNotifier.class).sendResetLink("a@example.com", LINK, Instant.EPOCH);
            });
        }
        assertThat(output).doesNotContain("GIZLI-TOKEN-123").contains("bağlantı gönderilmedi");
    }
}
