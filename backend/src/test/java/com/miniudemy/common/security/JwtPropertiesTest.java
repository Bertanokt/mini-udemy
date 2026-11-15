package com.miniudemy.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Güvenlik denetimi O2: JWT anahtarı eksik, boş ya da 32 bayttan kısaysa uygulama açılmaz; yeterli anahtarla açılır.
 * Gerçek bağlamı ({@link JwtConfig}) farklı ayarlarla başlatarak denenir.
 */
class JwtPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class);

    @Test
    void missingSecret_applicationDoesNotStart() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("JWT anahtarı tanımlı değil");
        });
    }

    @Test
    void blankSecret_applicationDoesNotStart() {
        runner.withPropertyValues("app.jwt.secret=   ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("JWT anahtarı tanımlı değil");
        });
    }

    @Test
    void unresolvedEnvironmentVariable_reportedAsMissing() {
        // application.yml'daki ${JWT_SECRET} ortam değişkeni yokken çözülmeden düz metin olarak gelir;
        // "çok kısa" değil "tanımlı değil" denmeli (prod profiliyle elle denenirken bulundu).
        runner.withPropertyValues("app.jwt.secret=${JWT_SECRET_YOK}").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("JWT anahtarı tanımlı değil");
        });
    }

    @Test
    void shortSecret_applicationDoesNotStart() {
        runner.withPropertyValues("app.jwt.secret=" + "k".repeat(JwtProperties.MIN_SECRET_BYTES - 1)).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("en az 32 bayt");
        });
    }

    @Test
    void secretOf32Bytes_applicationStarts() {
        runner.withPropertyValues("app.jwt.secret=" + "k".repeat(JwtProperties.MIN_SECRET_BYTES)).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JwtProperties.class).toString()).doesNotContain("kkkk");
        });
    }
}
