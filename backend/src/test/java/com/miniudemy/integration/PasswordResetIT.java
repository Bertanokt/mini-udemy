package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.miniudemy.auth.service.PasswordResetNotifier;

/**
 * Parola yenileme uçtan uca (#115, FR-03): gerçek HTTP ve gerçek PostgreSQL. Bağlantıyı yakalamak için yalnız
 * {@link PasswordResetNotifier} taklit edilir (gerçek uygulaması e-posta yerine log'dur).
 */
class PasswordResetIT extends IntegrationTestBase {

    private static final String EMAIL = "ayse@example.com";

    @MockitoBean
    private PasswordResetNotifier notifier;

    @Test
    void forgottenPassword_isResetOnceWithTheLink_oldPasswordStopsWorking() {
        registerAndLogin("Ayşe Yılmaz", EMAIL, "STUDENT");

        // Kayıtlı ve kayıtlı olmayan e-postaya aynı yanıt.
        ResponseEntity<JsonNode> known = post("/api/auth/password-reset/request", null, Map.of("email", EMAIL));
        ResponseEntity<JsonNode> unknown = post("/api/auth/password-reset/request", null,
                Map.of("email", "kimse@example.com"));
        assertThat(known.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(known.getBody()).isNull();
        assertThat(unknown.getBody()).isNull();
        then(notifier).should(never()).sendResetLink(eq("kimse@example.com"), any(), any());

        String token = capturedToken();
        // Veritabanında token'ın kendisi değil, 64 karakterlik özeti var.
        String storedHash = jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens", String.class);
        assertThat(storedHash).hasSize(64).isNotEqualTo(token);

        assertThat(confirm(token, "YeniParola456!").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(loginStatus("Parola123!")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginStatus("YeniParola456!")).isEqualTo(HttpStatus.OK);

        // Aynı bağlantı ikinci kez kullanılamaz.
        ResponseEntity<JsonNode> again = confirm(token, "BaskaParola789!");
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(again.getBody().get("errors").get("token").asText()).contains("geçersiz");
        assertThat(loginStatus("YeniParola456!")).isEqualTo(HttpStatus.OK);
    }

    @Test
    void expiredLink_isRejected_andPasswordUnchanged() {
        registerAndLogin("Ayşe Yılmaz", EMAIL, "STUDENT");
        post("/api/auth/password-reset/request", null, Map.of("email", EMAIL));
        String token = capturedToken();
        jdbc.update("UPDATE password_reset_tokens SET expires_at = now() - interval '1 minute'");

        assertThat(confirm(token, "YeniParola456!").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(loginStatus("Parola123!")).isEqualTo(HttpStatus.OK);
    }

    @Test
    void newRequest_invalidatesThePreviousLink() {
        registerAndLogin("Ayşe Yılmaz", EMAIL, "STUDENT");
        post("/api/auth/password-reset/request", null, Map.of("email", EMAIL));
        String first = capturedToken();
        post("/api/auth/password-reset/request", null, Map.of("email", EMAIL));
        String second = capturedToken();

        assertThat(confirm(first, "YeniParola456!").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(confirm(second, "YeniParola456!").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unknownToken_andWeakPassword_return400() {
        assertThat(confirm("uydurma-token", "YeniParola456!").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ResponseEntity<JsonNode> weak = confirm("uydurma-token", "kisa");
        assertThat(weak.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(weak.getBody().get("errors").has("newPassword")).isTrue();
    }

    private String capturedToken() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        then(notifier).should(org.mockito.Mockito.atLeastOnce()).sendResetLink(eq(EMAIL), link.capture(), any());
        String last = link.getAllValues().get(link.getAllValues().size() - 1);
        assertThat(last).startsWith("http://localhost:3000/reset-password?token=");
        return last.substring(last.indexOf("token=") + "token=".length());
    }

    private ResponseEntity<JsonNode> confirm(String token, String newPassword) {
        return post("/api/auth/password-reset/confirm", null, Map.of("token", token, "newPassword", newPassword));
    }

    private HttpStatus loginStatus(String password) {
        return HttpStatus.valueOf(post("/api/auth/login", null, Map.of("email", EMAIL, "password", password))
                .getStatusCode().value());
    }
}
