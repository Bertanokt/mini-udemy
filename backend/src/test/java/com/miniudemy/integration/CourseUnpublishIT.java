package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Yayından kaldırma ve yeniden incelemeye gönderme (#123, FR-38, UC-25 ve UC-24). Gerçek HTTP ve gerçek PostgreSQL ile.
 *
 * <p>Kapsanan: yönetici gerekçeyle kaldırır → kurs katalogda ve girişsiz detayda yok, yeni kayıt 404 → önceden kayıtlı
 * kursiyer bölümleri, detayı ve ders içeriğini açmaya devam eder → eğitmen düzenler ve yeniden onaya gönderir →
 * yönetici onaylar, kurs katalogda geri gelir ve eski gerekçe temizlenir. Ayrıca eğitmenin kendi kursunu gerekçesiz
 * kaldırması, başka eğitmenin kaldıramaması ve yayımlanmamış kursu kaldırmanın 409 olması.
 */
class CourseUnpublishIT extends IntegrationTestBase {

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};

    @Test
    void adminUnpublishes_enrolledStudentKeepsAccess_instructorResubmits_andCourseReturnsToCatalog() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = createCourse(instructor);
        long sectionId = createSection(instructor, courseId);
        long lessonId = createLessonWithContent(instructor, sectionId);
        publish(instructor, admin, courseId);

        String enrolled = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", enrolled, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Gerekçesiz kaldırma kabul edilmez; kurs yayında kalır.
        ResponseEntity<JsonNode> noReason = post("/api/admin/courses/" + courseId + "/unpublish", admin, Map.of());
        assertThat(noReason.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noReason.getBody().get("errors").has("reason")).isTrue();

        // 1. Yönetici gerekçeyle kaldırır; gerekçe kursta saklanır ve eğitmen panelinde görünür.
        ResponseEntity<JsonNode> unpublished = post("/api/admin/courses/" + courseId + "/unpublish", admin,
                Map.of("reason", "Telif şikâyeti alındı."));
        assertThat(unpublished.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unpublished.getBody().get("status").asText()).isEqualTo("UNPUBLISHED");
        assertThat(unpublished.getBody().get("unpublishReason").asText()).isEqualTo("Telif şikâyeti alındı.");
        JsonNode mine = get("/api/instructor/courses", instructor).getBody().get(0);
        assertThat(mine.get("unpublishReason").asText()).isEqualTo("Telif şikâyeti alındı.");

        // 2. Katalogda ve girişsiz detayda yok; yeni kursiyer kaydolamaz.
        assertThat(get("/api/courses?q=java", null).getBody().get("totalElements").asLong()).isZero();
        assertThat(get("/api/courses/" + courseId, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String newcomer = registerAndLogin("Can Demir", "can@example.com", "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", newcomer, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(get("/api/courses/" + courseId + "/sections", newcomer).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(download("/api/lessons/" + lessonId + "/content", newcomer, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // 3. Önceden kayıtlı kursiyer derslerine devam eder: bölümler, detay ve içerik açılır.
        ResponseEntity<JsonNode> sections = get("/api/courses/" + courseId + "/sections", enrolled);
        assertThat(sections.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(sections.getBody().get(0).get("id").asLong()).isEqualTo(sectionId);
        assertThat(get("/api/courses/" + courseId, enrolled).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(download("/api/lessons/" + lessonId + "/content", enrolled, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // 4. Eğitmen kursu düzenler ve yeniden incelemeye gönderir; yönetici kaldırılma sebebini görür.
        assertThat(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Ek bölüm"))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<JsonNode> resubmitted = post("/api/courses/" + courseId + "/submit", instructor, null);
        assertThat(resubmitted.getBody().get("status").asText()).isEqualTo("IN_REVIEW");
        assertThat(resubmitted.getBody().get("unpublishReason").asText()).isEqualTo("Telif şikâyeti alındı.");

        // 5. Yönetici onaylar: kurs katalogda geri gelir, eski kaldırma gerekçesi temizlenir.
        ResponseEntity<JsonNode> approved = post("/api/admin/courses/" + courseId + "/approve", admin, null);
        assertThat(approved.getBody().get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(approved.getBody().get("unpublishReason").isNull()).isTrue();
        assertThat(get("/api/courses?q=java", null).getBody().get("totalElements").asLong()).isEqualTo(1);
        assertThat(post("/api/courses/" + courseId + "/enroll", newcomer, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void instructorUnpublishesOwnCourse_withoutReason_butNotSomeoneElsesOrAnUnpublishedOne() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = createCourse(instructor);
        createLessonWithContent(instructor, createSection(instructor, courseId));

        // Yayımlanmamış kurs kaldırılamaz.
        assertThat(post("/api/courses/" + courseId + "/unpublish", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        publish(instructor, admin, courseId);

        // Başka eğitmen kaldıramaz.
        String other = registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");
        assertThat(post("/api/courses/" + courseId + "/unpublish", other, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT status FROM courses WHERE id = ?", String.class, courseId))
                .isEqualTo("PUBLISHED");

        // Sahibi gövdesiz kaldırır; gerekçe boş kalır, kurs katalogdan düşer ve ikinci kez kaldırılamaz.
        ResponseEntity<JsonNode> unpublished = post("/api/courses/" + courseId + "/unpublish", instructor, null);
        assertThat(unpublished.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unpublished.getBody().get("status").asText()).isEqualTo("UNPUBLISHED");
        assertThat(unpublished.getBody().get("unpublishReason").isNull()).isTrue();
        assertThat(get("/api/courses?q=java", null).getBody().get("totalElements").asLong()).isZero();
        assertThat(post("/api/courses/" + courseId + "/unpublish", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // --- Hazırlık ---

    private long createCourse(String instructor) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        ResponseEntity<JsonNode> created = post("/api/courses", instructor, Map.of(
                "title", "Sıfırdan Java",
                "description", "Java'ya giriş kursu.",
                "categoryId", categoryId,
                "level", "BEGINNER",
                "learningOutcomes", List.of("Temel sözdizimi")));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    private long createSection(String instructor, long courseId) {
        ResponseEntity<JsonNode> created = post("/api/courses/" + courseId + "/sections", instructor,
                Map.of("title", "Giriş"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    private long createLessonWithContent(String instructor, long sectionId) {
        ResponseEntity<JsonNode> created = post("/api/sections/" + sectionId + "/lessons", instructor,
                Map.of("title", "Kurulum", "contentType", "VIDEO", "mandatory", true, "durationSeconds", 300));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long lessonId = created.getBody().get("id").asLong();
        assertThat(upload("/api/lessons/" + lessonId + "/content", instructor, "ders.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return lessonId;
    }

    private void publish(String instructor, String admin, long courseId) {
        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
