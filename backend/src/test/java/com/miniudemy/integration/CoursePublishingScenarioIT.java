package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Senaryo 1: Kurs yayımlama (docs/use-cases.md, #65). Gerçek HTTP ve gerçek PostgreSQL ile.
 *
 * <p>Kapsanan adımlar: kurs oluşturma (UC-19) → bölüm ve ders ekleme, sıra değiştirme (UC-20) → soru bankası (UC-22)
 * → bölüm sonu sınavı (UC-23) → onaya gönderme (UC-24) → yönetici kuyruğu ve onay (UC-27) → katalogda görünme.
 * Alternatif akışlar: gerekçeli red ve yeniden gönderme (7a), gerekçesiz red (400), geçersiz soru (4a), geçersiz
 * durum geçişleri (6a, 7b), başka eğitmenin kursu (403).
 *
 * <p>Kapsanmayan: içerik dosyası yükleme (UC-21; yükleme ucu #112 henüz main'de değil, derslerin içerik kaydı
 * doğrudan veritabanına eklenir) ve "kurs onaylandı/reddedildi" bildirimleri (bildirim ucu henüz yok).
 */
class CoursePublishingScenarioIT extends IntegrationTestBase {

    @Test
    void instructor_buildsCourse_submits_adminApproves_andCourseAppearsInCatalog() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");

        // 1. Kurs Taslak olarak oluşur; yayımlanmadığı için katalogda ve girişsiz detayda yok.
        long courseId = createCourse(instructor, "Sıfırdan Java");
        assertThat(get("/api/courses/" + courseId, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 2. Bölümler ve dersler eklenir; yeni olan sona gider, sıra değiştirilebilir.
        long intro = createSection(instructor, courseId, "Giriş", 0);
        long objects = createSection(instructor, courseId, "Nesneler", 1);
        long setup = createLesson(instructor, intro, "Kurulum", 0);
        long hello = createLesson(instructor, intro, "İlk program", 1);
        long classes = createLesson(instructor, objects, "Sınıflar", 0);

        ResponseEntity<JsonNode> movedLesson = put("/api/lessons/" + hello + "/position", instructor,
                Map.of("position", 0));
        assertThat(movedLesson.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(movedLesson.getBody())).containsExactly(hello, setup);

        ResponseEntity<JsonNode> movedSection = put("/api/sections/" + objects + "/position", instructor,
                Map.of("position", 0));
        assertThat(movedSection.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(movedSection.getBody())).containsExactly(objects, intro);

        // Sahibi taslak kursun yapısını görür; sıra veritabanına yazılmıştır.
        ResponseEntity<JsonNode> structure = get("/api/courses/" + courseId + "/sections", instructor);
        assertThat(structure.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(structure.getBody())).containsExactly(objects, intro);
        assertThat(ids(structure.getBody().get(1).get("lessons"))).containsExactly(hello, setup);
        assertThat(ids(structure.getBody().get(0).get("lessons"))).containsExactly(classes);

        // 3. İçerik (yükleme ucu yok; dosya kaydı veritabanına).
        attachContent(setup);

        // 4. Soru bankası; 2'den az şıklı soru kaydedilmez.
        long q1 = createQuestion(instructor, courseId, "Java hangi yıl çıktı?");
        long q2 = createQuestion(instructor, courseId, "JVM neyin kısaltmasıdır?");
        ResponseEntity<JsonNode> invalid = post("/api/courses/" + courseId + "/questions", instructor, Map.of(
                "text", "Tek şıklı soru", "points", 5,
                "options", List.of(Map.of("text", "Tek", "correct", true))));
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody().get("errors").has("options")).isTrue();

        // 5. Bölüm sonu sınavı bankadaki sorularla.
        ResponseEntity<JsonNode> quiz = post("/api/sections/" + intro + "/quizzes", instructor, Map.of(
                "title", "Giriş sınavı", "scoringType", "SINGLE_ANSWER", "passThreshold", 70, "required", true,
                "questionIds", List.of(q1, q2)));
        assertThat(quiz.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 6. Onaya gönderilir; İncelemede iken düzenlenemez ve tekrar gönderilemez.
        assertThat(status(post("/api/courses/" + courseId + "/submit", instructor, null))).isEqualTo("IN_REVIEW");
        assertThat(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Ek bölüm"))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        // 7. Yönetici kuyruğunda görür ve onaylar.
        String admin = createAdminAndLogin("admin@example.com");
        ResponseEntity<JsonNode> queue = get("/api/admin/courses?status=IN_REVIEW", admin);
        assertThat(queue.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(queue.getBody().get("content"))).containsExactly(courseId);
        assertThat(status(post("/api/admin/courses/" + courseId + "/approve", admin, null))).isEqualTo("PUBLISHED");

        // 8. Yayımlandı: katalogda aranır, girişsiz detayda bölüm ve dersleriyle görünür.
        ResponseEntity<JsonNode> search = get("/api/courses?q=java", null);
        assertThat(ids(search.getBody().get("content"))).containsExactly(courseId);
        assertThat(search.getBody().get("content").get(0).get("lessonCount").asInt()).isEqualTo(3);
        ResponseEntity<JsonNode> detail = get("/api/courses/" + courseId, null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ids(detail.getBody().get("sections"))).containsExactly(objects, intro);

        // Yayımlanmış kurs yeniden onaylanamaz.
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void admin_rejectsWithReason_courseReturnsToDraft_reasonVisibleToInstructor_andCanBeResubmitted() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = createCourse(instructor, "Sıfırdan Java");
        long section = createSection(instructor, courseId, "Giriş", 0);
        attachContent(createLesson(instructor, section, "Kurulum", 0));
        post("/api/courses/" + courseId + "/submit", instructor, null);
        String admin = createAdminAndLogin("admin@example.com");

        // Gerekçesiz red kabul edilmez; kurs İncelemede kalır.
        ResponseEntity<JsonNode> noReason = post("/api/admin/courses/" + courseId + "/reject", admin,
                Map.of("reason", ""));
        assertThat(noReason.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noReason.getBody().get("errors").has("reason")).isTrue();

        // Gerekçeli red: Taslak'a döner, gerekçe eğitmen panelinde görünür, katalogda yok.
        ResponseEntity<JsonNode> rejected = post("/api/admin/courses/" + courseId + "/reject", admin,
                Map.of("reason", "Örnekler yetersiz."));
        assertThat(status(rejected)).isEqualTo("DRAFT");
        JsonNode mine = get("/api/instructor/courses", instructor).getBody().get(0);
        assertThat(mine.get("status").asText()).isEqualTo("DRAFT");
        assertThat(mine.get("rejectionReason").asText()).isEqualTo("Örnekler yetersiz.");
        assertThat(get("/api/courses?q=java", null).getBody().get("totalElements").asLong()).isZero();

        // Eğitmen düzeltir ve yeniden gönderir; Taslak olmayan kurs reddedilemez.
        assertThat(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Örnekler"))
                .getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(status(post("/api/courses/" + courseId + "/submit", instructor, null))).isEqualTo("IN_REVIEW");
        assertThat(status(post("/api/admin/courses/" + courseId + "/approve", admin, null))).isEqualTo("PUBLISHED");
        assertThat(post("/api/admin/courses/" + courseId + "/reject", admin, Map.of("reason", "Geç kalan red."))
                .getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void anotherInstructor_cannotChangeTheCourse_norSubmitIt() {
        String owner = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = createCourse(owner, "Sıfırdan Java");
        long section = createSection(owner, courseId, "Giriş", 0);
        String other = registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");

        assertThat(post("/api/courses/" + courseId + "/sections", other, Map.of("title", "Sızma"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/sections/" + section + "/lessons", other, lessonBody("Sızma"))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/courses/" + courseId + "/submit", other, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sections WHERE course_id = ?", Long.class, courseId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM courses WHERE id = ?", String.class, courseId))
                .isEqualTo("DRAFT");
    }

    // --- Hazırlık ---

    private long createCourse(String instructor, String title) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        ResponseEntity<JsonNode> created = post("/api/courses", instructor, Map.of(
                "title", title,
                "description", title + " kursunun açıklaması.",
                "categoryId", categoryId,
                "level", "BEGINNER",
                "learningOutcomes", List.of("Temel sözdizimi")));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(status(created)).isEqualTo("DRAFT");
        return created.getBody().get("id").asLong();
    }

    private long createSection(String instructor, long courseId, String title, int expectedPosition) {
        ResponseEntity<JsonNode> created = post("/api/courses/" + courseId + "/sections", instructor,
                Map.of("title", title));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("position").asInt()).isEqualTo(expectedPosition);
        return created.getBody().get("id").asLong();
    }

    private long createLesson(String instructor, long sectionId, String title, int expectedPosition) {
        ResponseEntity<JsonNode> created = post("/api/sections/" + sectionId + "/lessons", instructor,
                lessonBody(title));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("position").asInt()).isEqualTo(expectedPosition);
        return created.getBody().get("id").asLong();
    }

    private static Map<String, Object> lessonBody(String title) {
        return Map.of("title", title, "contentType", "VIDEO", "mandatory", true, "durationSeconds", 300);
    }

    private long createQuestion(String instructor, long courseId, String text) {
        ResponseEntity<JsonNode> created = post("/api/courses/" + courseId + "/questions", instructor, Map.of(
                "text", text, "points", 5,
                "options", List.of(Map.of("text", "Doğru şık", "correct", true),
                        Map.of("text", "Yanlış şık", "correct", false))));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    /**
     * Derse içerik bağlar (onaya gönderme en az bir içerikli ders ister). Yükleme ucu (#112) main'e girince bu adım
     * gerçek yüklemeyle değiştirilecek.
     */
    private void attachContent(long lessonId) {
        long uploader = jdbc.queryForObject("SELECT id FROM users WHERE email = 'mehmet@example.com'", Long.class);
        long fileId = jdbc.queryForObject("""
                INSERT INTO stored_files (uploader_id, storage_key, original_name, mime_type, size_bytes)
                VALUES (?, gen_random_uuid()::text, 'ders.mp4', 'video/mp4', 1024) RETURNING id
                """, Long.class, uploader);
        jdbc.update("UPDATE lessons SET content_file_id = ? WHERE id = ?", fileId, lessonId);
    }

    private static String status(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("yanıt: %s", response.getBody()).isTrue();
        return response.getBody().get("status").asText();
    }

    private static List<Long> ids(JsonNode objects) {
        List<Long> ids = new ArrayList<>();
        objects.forEach(o -> ids.add(o.get("id").asLong()));
        return ids;
    }
}
