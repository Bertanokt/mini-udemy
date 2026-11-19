package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kurs düzenleme ve silme (#23), gerçek HTTP ve gerçek PostgreSQL ile. Silme Taslak kursta bölümleri, dersleri, soru
 * bankasını, sınavları ve yüklenen dosyaları (veritabanı ve disk) kaldırır; yayımlanmış ya da kursiyeri olan kurs
 * silinmez. Düzenleme yalnız Taslak ve Yayından kaldırıldı'da.
 */
class CourseEditDeleteIT extends IntegrationTestBase {

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};
    private static final byte[] PDF = "%PDF-1.4\n%deneme\n".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Test
    void owner_updatesDraftCourse_othersCannot_andPublishedCourseIsLocked() {
        String owner = registerAndLogin("Zeynep Kaya", "zeynep@example.com", "INSTRUCTOR");
        String other = registerAndLogin("Can Demir", "can@example.com", "INSTRUCTOR");
        long courseId = course(owner, "Java'ya giriş");
        long design = jdbc.queryForObject("SELECT id FROM categories WHERE name <> 'Yazılım' ORDER BY id LIMIT 1",
                Long.class);
        Map<String, Object> body = Map.of("title", "Java'ya sıfırdan giriş", "description",
                "Güncellenmiş kurs açıklaması.", "categoryId", design, "level", "INTERMEDIATE",
                "learningOutcomes", List.of("Sınıflar", "Koleksiyonlar"));

        ResponseEntity<JsonNode> updated = put("/api/courses/" + courseId, owner, body);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().get("title").asText()).isEqualTo("Java'ya sıfırdan giriş");
        assertThat(updated.getBody().get("category").get("id").asLong()).isEqualTo(design);
        assertThat(updated.getBody().get("level").asText()).isEqualTo("INTERMEDIATE");
        assertThat(updated.getBody().get("status").asText()).isEqualTo("DRAFT");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT title, description, category_id, level, array_to_string(learning_outcomes, '|') AS outcomes "
                        + "FROM courses WHERE id = ?", courseId);
        assertThat(row).containsEntry("title", "Java'ya sıfırdan giriş")
                .containsEntry("description", "Güncellenmiş kurs açıklaması.")
                .containsEntry("category_id", design).containsEntry("level", "INTERMEDIATE")
                .containsEntry("outcomes", "Sınıflar|Koleksiyonlar");

        // Başka eğitmen 403, olmayan kurs 404, olmayan kategori 400.
        assertThat(put("/api/courses/" + courseId, other, body).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete("/api/courses/" + courseId, other).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(put("/api/courses/999999", owner, body).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(delete("/api/courses/999999", owner).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ResponseEntity<JsonNode> badCategory = put("/api/courses/" + courseId, owner,
                Map.of("title", "Başlık", "description", "Geçerli bir açıklama.", "categoryId", 999999,
                        "level", "BEGINNER"));
        assertThat(badCategory.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badCategory.getBody().get("errors").has("categoryId")).isTrue();
        assertThat(jdbc.queryForObject("SELECT title FROM courses WHERE id = ?", String.class, courseId))
                .isEqualTo("Java'ya sıfırdan giriş");

        // Yayımlanınca düzenleme ve silme 409; kurs değişmeden kalır.
        publish(owner, courseId);
        ResponseEntity<JsonNode> locked = put("/api/courses/" + courseId, owner, body);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(locked.getBody().get("status").asInt()).isEqualTo(409);
        assertThat(delete("/api/courses/" + courseId, owner).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE id = ?", Long.class, courseId))
                .isEqualTo(1);
    }

    @Test
    void deletingADraftCourse_removesSectionsLessonsQuestionsQuizzesAndFiles() {
        String owner = registerAndLogin("Zeynep Kaya", "zeynep@example.com", "INSTRUCTOR");
        long courseId = course(owner, "Silinecek kurs");
        long keptCourse = course(owner, "Kalacak kurs");
        long section = id(post("/api/courses/" + courseId + "/sections", owner, Map.of("title", "Giriş")));
        long video = lesson(owner, section, "Video", "VIDEO");
        long pdf = lesson(owner, section, "Notlar", "PDF");
        assertThat(upload("/api/lessons/" + video + "/content", owner, "ders.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(upload("/api/lessons/" + pdf + "/content", owner, "notlar.pdf", PDF).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(upload("/api/courses/" + courseId + "/cover", owner, "kapak.png", PNG).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        long question = id(post("/api/courses/" + courseId + "/questions", owner, Map.of("text", "Soru metni?",
                "points", 5, "options", List.of(Map.of("text", "Doğru", "correct", true),
                        Map.of("text", "Yanlış", "correct", false)))));
        id(post("/api/sections/" + section + "/quizzes", owner, Map.of("title", "Sınav", "scoringType",
                "SINGLE_ANSWER", "passThreshold", 60, "required", true, "questionIds", List.of(question))));
        // Başka kursun verisi silinmemeli.
        long keptSection = id(post("/api/courses/" + keptCourse + "/sections", owner, Map.of("title", "Kalan")));
        long keptLesson = lesson(owner, keptSection, "Kalan ders", "VIDEO");
        assertThat(upload("/api/lessons/" + keptLesson + "/content", owner, "kalan.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        List<String> keys = jdbc.queryForList("""
                SELECT f.storage_key FROM stored_files f
                WHERE f.id IN (SELECT content_file_id FROM lessons l JOIN sections s ON s.id = l.section_id
                               WHERE s.course_id = ?)
                   OR f.id = (SELECT cover_file_id FROM courses WHERE id = ?)""", String.class, courseId, courseId);
        assertThat(keys).hasSize(3).allMatch(this::fileOnDisk);
        String keptKey = jdbc.queryForObject("SELECT f.storage_key FROM stored_files f JOIN lessons l "
                + "ON l.content_file_id = f.id WHERE l.id = ?", String.class, keptLesson);

        assertThat(delete("/api/courses/" + courseId, owner).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(count("SELECT count(*) FROM courses WHERE id = ?", courseId)).isZero();
        assertThat(count("SELECT count(*) FROM sections WHERE course_id = ?", courseId)).isZero();
        assertThat(count("SELECT count(*) FROM lessons WHERE id IN (?, ?)", video, pdf)).isZero();
        assertThat(count("SELECT count(*) FROM questions WHERE course_id = ?", courseId)).isZero();
        assertThat(count("SELECT count(*) FROM quizzes WHERE section_id = ?", section)).isZero();
        assertThat(count("SELECT count(*) FROM quiz_questions WHERE question_id = ?", question)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isEqualTo(1);
        assertThat(keys).noneMatch(this::fileOnDisk);
        assertThat(fileOnDisk(keptKey)).isTrue();
        assertThat(count("SELECT count(*) FROM lessons WHERE id = ?", keptLesson)).isEqualTo(1);
        assertThat(get("/api/instructor/courses", owner).getBody()).hasSize(1);
    }

    @Test
    void draftCourseThatHasStudents_isNotDeleted() {
        // Yayımlanıp kursiyer almış, sonra taslağa dönmüş kurs (sözleşme › deleteCourse). Yayından kaldırma ve
        // yeniden gönderme uçları (#123) henüz yok; taslağa dönüş veritabanında yapılır.
        String owner = registerAndLogin("Zeynep Kaya", "zeynep@example.com", "INSTRUCTOR");
        long courseId = course(owner, "Kursiyerli kurs");
        long section = id(post("/api/courses/" + courseId + "/sections", owner, Map.of("title", "Giriş")));
        long lesson = lesson(owner, section, "Video", "VIDEO");
        upload("/api/lessons/" + lesson + "/content", owner, "ders.mp4", MP4);
        publish(owner, courseId);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        jdbc.update("UPDATE courses SET status = 'DRAFT' WHERE id = ?", courseId);

        ResponseEntity<JsonNode> refused = delete("/api/courses/" + courseId, owner);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody().get("message").asText()).contains("kursiyer");
        assertThat(count("SELECT count(*) FROM courses WHERE id = ?", courseId)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM lessons WHERE id = ?", lesson)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isEqualTo(1);
    }

    // --- Hazırlık ---

    private long course(String instructor, String title) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        return id(post("/api/courses", instructor, Map.of("title", title, "description", title + " açıklaması.",
                "categoryId", categoryId, "level", "BEGINNER", "learningOutcomes", List.of("Temeller"))));
    }

    private long lesson(String instructor, long sectionId, String title, String type) {
        return id(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of("title", title,
                "contentType", type, "mandatory", true, "durationSeconds", 60)));
    }

    private void publish(String instructor, long courseId) {
        if (count("SELECT count(*) FROM sections WHERE course_id = ?", courseId) == 0) {
            long section = id(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Bölüm")));
            long lesson = lesson(instructor, section, "Ders", "VIDEO");
            upload("/api/lessons/" + lesson + "/content", instructor, "ders.mp4", MP4);
        }
        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String admin = createAdminAndLogin("admin" + courseId + "@example.com");
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private static long id(ResponseEntity<JsonNode> created) {
        assertThat(created.getStatusCode()).as("yanıt: %s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }
}
