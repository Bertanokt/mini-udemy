package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Yorum moderasyonu (#142) gerçek PostgreSQL'de: yönetici yorumu gizleyince yorum herkese açık listeden ve
 * ortalamadan (detay özeti ve katalog kartı) çıkar, gerekçe saklanır ve yalnız yönetici listesinde görünür; geri
 * açınca her şey eski haline döner.
 */
class ReviewModerationIT extends IntegrationTestBase {

    @Test
    void hiddenReview_leavesPublicListAndAverage_reasonIsKept_andUnhideRestores() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String ayse = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        String can = registerAndLogin("Can Demir", "can@example.com", "STUDENT");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = publishedCourse(instructor);
        enroll("ayse@example.com", courseId);
        enroll("can@example.com", courseId);
        post("/api/courses/" + courseId + "/reviews", ayse, Map.of("rating", 5, "comment", "Harika"));
        long canReview = post("/api/courses/" + courseId + "/reviews", can,
                Map.of("rating", 1, "comment", "Uygunsuz yorum")).getBody().get("id").asLong();
        assertThat(publicSummary(courseId).get("averageRating").asDouble()).isEqualTo(3.0);

        ResponseEntity<JsonNode> hidden = put("/api/admin/reviews/" + canReview + "/visibility", admin,
                Map.of("hidden", true, "reason", "Hakaret içeriyor"));

        assertThat(hidden.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hidden.getBody().get("moderationReason").asText()).isEqualTo("Hakaret içeriyor");
        // Herkese açık liste ve ortalama: gizli yorum yok.
        JsonNode reviews = get("/api/courses/" + courseId + "/reviews", null).getBody();
        assertThat(reviews.get("content")).hasSize(1);
        assertThat(reviews.get("content").get(0).get("comment").asText()).isEqualTo("Harika");
        assertThat(reviews.get("summary").get("averageRating").asDouble()).isEqualTo(5.0);
        assertThat(reviews.get("summary").get("reviewCount").asLong()).isEqualTo(1);
        JsonNode card = get("/api/courses?q=Moderasyon", null).getBody().get("content").get(0);
        assertThat(card.get("averageRating").asDouble()).isEqualTo(5.0);
        assertThat(card.get("reviewCount").asLong()).isEqualTo(1);
        // Gerekçe saklandı; yönetici listesinde görünür.
        assertThat(jdbc.queryForObject("SELECT moderation_reason FROM reviews WHERE id = ?", String.class, canReview))
                .isEqualTo("Hakaret içeriyor");
        JsonNode adminList = get("/api/admin/reviews?courseId=" + courseId + "&hidden=true", admin).getBody();
        assertThat(adminList.get("content")).hasSize(1);
        assertThat(adminList.get("content").get(0).get("moderationReason").asText()).isEqualTo("Hakaret içeriyor");
        assertThat(get("/api/admin/reviews", admin).getBody().get("totalElements").asLong()).isEqualTo(2);

        ResponseEntity<JsonNode> shown = put("/api/admin/reviews/" + canReview + "/visibility", admin,
                Map.of("hidden", false));

        assertThat(shown.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shown.getBody().get("moderationReason").isNull()).isTrue();
        assertThat(get("/api/courses/" + courseId + "/reviews", null).getBody().get("content")).hasSize(2);
        assertThat(publicSummary(courseId).get("averageRating").asDouble()).isEqualTo(3.0);
    }

    @Test
    void hide_withoutReason_returns400_andReviewStaysVisible() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String ayse = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = publishedCourse(instructor);
        enroll("ayse@example.com", courseId);
        long review = post("/api/courses/" + courseId + "/reviews", ayse, Map.of("rating", 4)).getBody()
                .get("id").asLong();
        Map<String, Object> body = new HashMap<>();
        body.put("hidden", true);

        ResponseEntity<JsonNode> response = put("/api/admin/reviews/" + review + "/visibility", admin, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errors").get("reason").asText()).isEqualTo("Gizleme gerekçesi zorunludur.");
        assertThat(get("/api/courses/" + courseId + "/reviews", null).getBody().get("content")).hasSize(1);
    }

    private JsonNode publicSummary(long courseId) {
        return get("/api/courses/" + courseId + "/reviews", null).getBody().get("summary");
    }

    private long publishedCourse(String instructorToken) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long id = post("/api/courses", instructorToken, Map.of("title", "Moderasyon kursu",
                "description", "Moderasyon testi için kurs.", "categoryId", categoryId, "level", "BEGINNER",
                "learningOutcomes", List.of("Temeller"))).getBody().get("id").asLong();
        jdbc.update("UPDATE courses SET status = 'PUBLISHED' WHERE id = ?", id);
        return id;
    }

    private void enroll(String email, long courseId) {
        jdbc.update("INSERT INTO enrollments (student_id, course_id) VALUES ((SELECT id FROM users WHERE email = ?), ?)",
                email, courseId);
    }
}
