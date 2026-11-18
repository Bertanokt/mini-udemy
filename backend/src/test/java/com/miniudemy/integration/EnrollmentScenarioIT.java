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
 * Senaryo 2: Kursa kayıt ve öğrenme (docs/use-cases.md, #65). Gerçek HTTP ve gerçek PostgreSQL ile.
 *
 * <p>Kapsanan adımlar: arama (UC-04) → kurs detayı (UC-05) → kayıt (UC-10) → kayıtlı kurslarım → ders tamamlama
 * (UC-13) → ilerleme. Kapsanmayan adım: içeriğe erişim (UC-12, video/PDF açma); içerik uçları main'e girince
 * eklenecek.
 *
 * <p>Ön koşul olan yayımlanmış kurs, mümkün olduğunca gerçek uçlarla hazırlanır: eğitmen kaydı, kurs oluşturma (#22),
 * onaya gönderme (#27) ve yönetici onayı (#59). Bölüm ve ders uçları (#24, #25) henüz main'de olmadığı için kursun
 * bölüm ve dersleri doğrudan veritabanına eklenir.
 */
class EnrollmentScenarioIT extends IntegrationTestBase {

    @Test
    void student_searchesCatalog_opensDetail_enrolls_completesLesson_andSeesProgress() {
        PublishedCourse java = publishedCourseWithTwoLessons();
        draftCourse(java.instructorToken(), "Java ile Spring Boot");

        // 1. Arama (girişsiz): yalnız yayımlanmış kurs gelir, taslak gelmez.
        ResponseEntity<JsonNode> search = get("/api/courses?q=java", null);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(search.getBody().get("totalElements").asLong()).isEqualTo(1);
        JsonNode hit = search.getBody().get("content").get(0);
        assertThat(hit.get("id").asLong()).isEqualTo(java.courseId());
        assertThat(hit.get("status").asText()).isEqualTo("PUBLISHED");
        assertThat(hit.get("lessonCount").asInt()).isEqualTo(2);

        // 2. Kurs detayı (girişsiz): bölüm ve dersler sırasıyla görünür.
        ResponseEntity<JsonNode> detail = get("/api/courses/" + java.courseId(), null);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode section = detail.getBody().get("sections").get(0);
        assertThat(section.get("title").asText()).isEqualTo("Giriş");
        assertThat(ids(section.get("lessons"))).containsExactlyElementsOf(java.lessonIds());

        // 3. Kursiyer kaydolur; kayıt olmadan ders tamamlayamaz.
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        Long firstLesson = java.lessonIds().get(0);
        assertThat(post("/api/lessons/" + firstLesson + "/complete", student, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<JsonNode> enrolled = post("/api/courses/" + java.courseId() + "/enroll", student, null);
        assertThat(enrolled.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(enrolled.getBody().get("course").get("id").asLong()).isEqualTo(java.courseId());
        assertThat(enrolled.getBody().get("progressPercent").asDouble()).isZero();

        // Aynı kursa ikinci kayıt 409; veritabanında tek kayıt kalır.
        assertThat(post("/api/courses/" + java.courseId() + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments WHERE course_id = ?", Long.class,
                java.courseId())).isEqualTo(1);

        // 4. Kayıtlı kurslarım listesinde görünür.
        ResponseEntity<JsonNode> mine = get("/api/me/enrollments", student);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).hasSize(1);
        assertThat(mine.getBody().get(0).get("course").get("id").asLong()).isEqualTo(java.courseId());

        // 5. İki zorunlu dersten biri tamamlanır: ilerleme %50.
        ResponseEntity<JsonNode> completed = post("/api/lessons/" + firstLesson + "/complete", student, null);
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody().get("progressPercent").asDouble()).isEqualTo(50.0);

        // 6. İlerleme ucu aynı sonucu verir.
        ResponseEntity<JsonNode> progress = get("/api/me/courses/" + java.courseId() + "/progress", student);
        assertThat(progress.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(progress.getBody().get("progressPercent").asDouble()).isEqualTo(50.0);
        assertThat(numbers(progress.getBody().get("completedLessonIds"))).containsExactly(firstLesson);
    }

    @Test
    void draftCourse_isNotInCatalog_andCannotBeEnrolled() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long draft = draftCourse(instructor, "Sıfırdan Java");

        assertThat(get("/api/courses?q=java", null).getBody().get("totalElements").asLong()).isZero();
        assertThat(get("/api/courses/" + draft, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        assertThat(post("/api/courses/" + draft + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments", Long.class)).isZero();
    }

    // --- Hazırlık ---

    private record PublishedCourse(String instructorToken, long courseId, List<Long> lessonIds) {
    }

    /** Eğitmen kurs açar, bir bölüm ve iki zorunlu ders eklenir, onaya gönderilir ve yönetici onaylar. */
    private PublishedCourse publishedCourseWithTwoLessons() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = draftCourse(instructor, "Sıfırdan Java");

        long sectionId = jdbc.queryForObject(
                "INSERT INTO sections (course_id, title, position) VALUES (?, 'Giriş', 0) RETURNING id",
                Long.class, courseId);
        List<Long> lessonIds = new ArrayList<>();
        lessonIds.add(lesson(sectionId, "Kurulum", 0));
        lessonIds.add(lesson(sectionId, "İlk program", 1));

        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String admin = createAdminAndLogin("admin@example.com");
        ResponseEntity<JsonNode> approved = post("/api/admin/courses/" + courseId + "/approve", admin, null);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().get("status").asText()).isEqualTo("PUBLISHED");

        return new PublishedCourse(instructor, courseId, lessonIds);
    }

    private long draftCourse(String instructorToken, String title) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        ResponseEntity<JsonNode> created = post("/api/courses", instructorToken, Map.of(
                "title", title,
                "description", title + " kursunun açıklaması.",
                "categoryId", categoryId,
                "level", "BEGINNER",
                "learningOutcomes", List.of("Temel sözdizimi")));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status").asText()).isEqualTo("DRAFT");
        return created.getBody().get("id").asLong();
    }

    private long lesson(long sectionId, String title, int position) {
        return jdbc.queryForObject("""
                INSERT INTO lessons (section_id, title, content_type, mandatory, duration_seconds, position)
                VALUES (?, ?, 'VIDEO', TRUE, 300, ?) RETURNING id
                """, Long.class, sectionId, title, position);
    }

    private static List<Long> ids(JsonNode objects) {
        List<Long> ids = new ArrayList<>();
        objects.forEach(o -> ids.add(o.get("id").asLong()));
        return ids;
    }

    private static List<Long> numbers(JsonNode array) {
        List<Long> numbers = new ArrayList<>();
        array.forEach(n -> numbers.add(n.asLong()));
        return numbers;
    }
}
