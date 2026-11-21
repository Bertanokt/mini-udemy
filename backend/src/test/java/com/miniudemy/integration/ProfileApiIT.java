package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/** Profil güncelleme uçtan uca (#116, FR-04): ad soyad ve biyografi değişir; e-posta ve rol değişmez. */
class ProfileApiIT extends IntegrationTestBase {

    @Test
    void userUpdatesNameAndBio_emailAndRoleCannotBeChanged() {
        String token = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");

        ResponseEntity<JsonNode> updated = put("/api/me", token,
                Map.of("fullName", "Ayşe Yılmaz Kaya", "bio", "Veri bilimine meraklıyım."));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("fullName").asText()).isEqualTo("Ayşe Yılmaz Kaya");
        JsonNode me = get("/api/me", token).getBody();
        assertThat(me.get("fullName").asText()).isEqualTo("Ayşe Yılmaz Kaya");
        assertThat(me.get("bio").asText()).isEqualTo("Veri bilimine meraklıyım.");

        ResponseEntity<JsonNode> sneaky = put("/api/me", token,
                Map.of("fullName", "Ayşe", "email", "baska@example.com", "role", "ADMIN"));
        assertThat(sneaky.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sneaky.getBody().get("errors").has("email")).isTrue();
        assertThat(sneaky.getBody().get("errors").has("role")).isTrue();
        assertThat(jdbc.queryForMap("SELECT email, role, full_name FROM users"))
                .containsEntry("email", "ayse@example.com")
                .containsEntry("role", "STUDENT")
                .containsEntry("full_name", "Ayşe Yılmaz Kaya");

        // Boş biyografi temizlenir.
        assertThat(put("/api/me", token, Map.of("fullName", "Ayşe Yılmaz Kaya", "bio", "")).getBody().get("bio")
                .isNull()).isTrue();
    }
}
