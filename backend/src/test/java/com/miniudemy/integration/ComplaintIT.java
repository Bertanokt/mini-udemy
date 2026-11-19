package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Şikâyet (#144, FR-41, UC-09 ve UC-30). Gerçek HTTP ve gerçek PostgreSQL ile.
 *
 * <p>Kapsanan: kursiyer yayımlanmış bir kursu, eğitmen bir yorumu gerekçeyle şikâyet eder → yönetici listeler (en yeni
 * önce, durum süzgeci) → durumu İncelendi / Reddedildi yapar, çözen yönetici ve zaman saklanır. Hata yolları: gerekçe
 * kısa, hedef iki ya da sıfır, kurs yayımlanmamış, yorum gizli; kursiyer yönetici uçlarına erişemez. Tablodaki CHECK
 * kısıtı (tek hedef) migration'la birlikte sınanır.
 */
class ComplaintIT extends IntegrationTestBase {

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};

    @Test
    void userComplainsAboutCourseAndReview_adminListsAndResolvesThem() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = publishedCourse(instructor, admin);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        long reviewId = review(student, courseId);

        // 1. Kursiyer kursu, eğitmen yorumu şikâyet eder; ikisi de açık doğar.
        ResponseEntity<JsonNode> aboutCourse = post("/api/complaints", student,
                Map.of("courseId", courseId, "reason", "Kurs içeriği yanıltıcı."));
        assertThat(aboutCourse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(aboutCourse.getBody().get("status").asText()).isEqualTo("OPEN");
        assertThat(aboutCourse.getBody().get("reporter").get("fullName").asText()).isEqualTo("Ayşe Yılmaz");
        assertThat(aboutCourse.getBody().get("reviewId").isNull()).isTrue();

        ResponseEntity<JsonNode> aboutReview = post("/api/complaints", instructor,
                Map.of("reviewId", reviewId, "reason", "Yorum hakaret içeriyor."));
        assertThat(aboutReview.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long courseComplaintId = aboutCourse.getBody().get("id").asLong();
        long reviewComplaintId = aboutReview.getBody().get("id").asLong();

        // 2. Yönetici listeler: en yeni önce, ikisi de açık.
        JsonNode all = get("/api/admin/complaints", admin).getBody();
        assertThat(all.get("totalElements").asLong()).isEqualTo(2);
        assertThat(all.get("content").get(0).get("id").asLong()).isEqualTo(reviewComplaintId);
        assertThat(all.get("content").get(0).get("reviewId").asLong()).isEqualTo(reviewId);

        // 3. Durumu değiştirir; çözen yönetici ve zaman saklanır.
        ResponseEntity<JsonNode> reviewed = put("/api/admin/complaints/" + courseComplaintId + "/status", admin,
                Map.of("status", "REVIEWED"));
        assertThat(reviewed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reviewed.getBody().get("status").asText()).isEqualTo("REVIEWED");
        assertThat(reviewed.getBody().get("resolvedAt").isNull()).isFalse();
        assertThat(jdbc.queryForObject("SELECT u.email FROM complaints c JOIN users u ON u.id = c.resolved_by_id "
                + "WHERE c.id = ?", String.class, courseComplaintId)).isEqualTo("admin@example.com");

        assertThat(put("/api/admin/complaints/" + reviewComplaintId + "/status", admin, Map.of("status", "REJECTED"))
                .getBody().get("status").asText()).isEqualTo("REJECTED");

        // 4. Durum süzgeci; açık şikâyet kalmadı. Yeniden açmak 400, olmayan şikâyet 404.
        assertThat(get("/api/admin/complaints?status=OPEN", admin).getBody().get("totalElements").asLong()).isZero();
        assertThat(get("/api/admin/complaints?status=REJECTED", admin).getBody().get("content").get(0).get("id")
                .asLong()).isEqualTo(reviewComplaintId);
        ResponseEntity<JsonNode> reopen = put("/api/admin/complaints/" + courseComplaintId + "/status", admin,
                Map.of("status", "OPEN"));
        assertThat(reopen.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reopen.getBody().get("errors").has("status")).isTrue();
        assertThat(put("/api/admin/complaints/999/status", admin, Map.of("status", "REVIEWED")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // 5. Kursiyer yönetici uçlarına erişemez.
        assertThat(get("/api/admin/complaints", student).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(put("/api/admin/complaints/" + courseComplaintId + "/status", student,
                Map.of("status", "REJECTED")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void complaint_withShortReasonWrongTargetOrInvisibleTarget_isRejected_andNothingIsSaved() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long publishedId = publishedCourse(instructor, admin);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        long reviewId = review(student, publishedId);
        long draftId = createCourse(instructor, "Taslak Kurs");

        ResponseEntity<JsonNode> shortReason = post("/api/complaints", student,
                Map.of("courseId", publishedId, "reason", "Kötü"));
        assertThat(shortReason.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(shortReason.getBody().get("errors").has("reason")).isTrue();

        assertThat(post("/api/complaints", student,
                Map.of("courseId", publishedId, "reviewId", reviewId, "reason", "Kurs içeriği yanıltıcı."))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post("/api/complaints", student, Map.of("reason", "Kurs içeriği yanıltıcı.")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        // Göremediği şey şikâyet edilemez: taslak kurs, olmayan kurs, gizli yorum → 404.
        assertThat(post("/api/complaints", student, Map.of("courseId", draftId, "reason", "Kurs içeriği yanıltıcı."))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/api/complaints", student, Map.of("courseId", 999, "reason", "Kurs içeriği yanıltıcı."))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        jdbc.update("UPDATE reviews SET hidden = TRUE WHERE id = ?", reviewId);
        assertThat(post("/api/complaints", student, Map.of("reviewId", reviewId, "reason", "Yorum hakaret içeriyor."))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(post("/api/complaints", null, Map.of("courseId", publishedId, "reason", "Kurs içeriği yanıltıcı."))
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM complaints", Long.class)).isZero();
    }

    @Test
    void complaintsTable_rejectsRowWithBothTargetsOrNone() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = publishedCourse(instructor, admin);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        long reviewId = review(student, courseId);
        long reporterId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'ayse@example.com'", Long.class);
        String insert = "INSERT INTO complaints (reporter_id, course_id, review_id, reason) VALUES (?, ?, ?, 'Gerekçe')";

        assertThatThrownBy(() -> jdbc.update(insert, reporterId, courseId, reviewId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, reporterId, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- Hazırlık ---

    private long publishedCourse(String instructor, String admin) {
        long courseId = createCourse(instructor, "Sıfırdan Java");
        ResponseEntity<JsonNode> section = post("/api/courses/" + courseId + "/sections", instructor,
                Map.of("title", "Giriş"));
        ResponseEntity<JsonNode> lesson = post("/api/sections/" + section.getBody().get("id").asLong() + "/lessons",
                instructor, Map.of("title", "Kurulum", "contentType", "VIDEO", "mandatory", true,
                        "durationSeconds", 300));
        assertThat(upload("/api/lessons/" + lesson.getBody().get("id").asLong() + "/content", instructor, "ders.mp4",
                MP4).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return courseId;
    }

    private long createCourse(String instructor, String title) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        ResponseEntity<JsonNode> created = post("/api/courses", instructor, Map.of(
                "title", title,
                "description", "Java'ya giriş kursu.",
                "categoryId", categoryId,
                "level", "BEGINNER",
                "learningOutcomes", List.of("Temel sözdizimi")));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    private long review(String student, long courseId) {
        assertThat(post("/api/courses/" + courseId + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        ResponseEntity<JsonNode> created = post("/api/courses/" + courseId + "/reviews", student,
                Map.of("rating", 1, "comment", "Berbat bir kurs."));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }
}
