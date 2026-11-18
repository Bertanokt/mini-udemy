package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Senaryo 5: eğitmen istatistikleri ve kursiyer listesi (#133, #134). Gerçek HTTP ve gerçek PostgreSQL ile: kayıt,
 * ders tamamlama ve sınav çözme gerçek uçlarla yapılır, istatistik uçları bu işlemlerin sonucunu doğru sayıyla döner.
 *
 * <p>Üç kursiyer: Ayşe iki dersi bitirir, sınavı önce kaçırıp sonra geçer; Burak bir ders bitirir ve sınava girmez;
 * Can yalnız kaydolur.
 */
class InstructorAnalyticsIT extends IntegrationTestBase {

    @Test
    void stats_and_students_reflectEnrollmentProgressAndQuizAttempts() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = draftCourse(instructor);
        long instructorId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'mehmet@example.com'", Long.class);
        long sectionId = jdbc.queryForObject(
                "INSERT INTO sections (course_id, title, position) VALUES (?, 'Giriş', 0) RETURNING id",
                Long.class, courseId);
        long lesson1 = lesson(sectionId, "Kurulum", 0, storeFile(instructorId, "a.mp4", "video/mp4", new byte[64]));
        long lesson2 = lesson(sectionId, "İlk program", 1, storeFile(instructorId, "b.mp4", "video/mp4", new byte[64]));

        ResponseEntity<JsonNode> question = post("/api/courses/" + courseId + "/questions", instructor, Map.of(
                "text", "Java hangi paradigmaya sahiptir?", "points", 10,
                "options", List.of(Map.of("text", "Nesne yönelimli", "correct", true),
                        Map.of("text", "Yalnız betik", "correct", false))));
        assertThat(question.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long questionId = question.getBody().get("id").asLong();
        long correctOption = question.getBody().get("options").get(0).get("id").asLong();
        long wrongOption = question.getBody().get("options").get(1).get("id").asLong();
        ResponseEntity<JsonNode> quiz = post("/api/sections/" + sectionId + "/quizzes", instructor, Map.of(
                "title", "Giriş sınavı", "scoringType", "SINGLE_ANSWER", "passThreshold", 60, "required", true,
                "questionIds", List.of(questionId)));
        assertThat(quiz.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long quizId = quiz.getBody().get("id").asLong();

        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String admin = createAdminAndLogin("admin@example.com");
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Kayıt öncesi: boş kurs, bölme hatası yok.
        JsonNode empty = get("/api/instructor/courses/" + courseId + "/stats", instructor).getBody();
        assertThat(empty.get("enrollmentCount").asLong()).isZero();
        assertThat(empty.get("averageProgressPercent").asDouble()).isZero();
        assertThat(empty.get("completionRate").asDouble()).isZero();
        assertThat(empty.get("quizzes").get(0).get("attemptCount").asLong()).isZero();
        assertThat(empty.get("quizzes").get(0).get("passRate").asDouble()).isZero();
        assertThat(get("/api/instructor/courses/" + courseId + "/students", instructor).getBody()
                .get("totalElements").asLong()).isZero();

        String ayse = enroll("Ayşe Yılmaz", "ayse@example.com", courseId);
        String burak = enroll("Burak Demir", "burak@example.com", courseId);
        enroll("Can Aksoy", "can@example.com", courseId);

        post("/api/lessons/" + lesson1 + "/complete", ayse, null);
        post("/api/lessons/" + lesson2 + "/complete", ayse, null);
        post("/api/lessons/" + lesson1 + "/complete", burak, null);
        assertThat(download("/api/lessons/" + lesson1 + "/content", ayse, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        attempt(quizId, ayse, questionId, wrongOption);
        attempt(quizId, ayse, questionId, correctOption);

        JsonNode stats = get("/api/instructor/courses/" + courseId + "/stats", instructor).getBody();
        assertThat(stats.get("courseId").asLong()).isEqualTo(courseId);
        assertThat(stats.get("enrollmentCount").asLong()).isEqualTo(3);
        assertThat(stats.get("averageProgressPercent").asDouble()).isEqualTo(50.0);
        assertThat(stats.get("completionRate").asDouble()).isEqualTo(33.33);
        JsonNode quizStats = stats.get("quizzes").get(0);
        assertThat(quizStats.get("quizId").asLong()).isEqualTo(quizId);
        assertThat(quizStats.get("title").asText()).isEqualTo("Giriş sınavı");
        assertThat(quizStats.get("attemptCount").asLong()).isEqualTo(2);
        assertThat(quizStats.get("passRate").asDouble()).isEqualTo(50.0);
        assertThat(stats.get("rating").get("reviewCount").asLong()).isZero();
        assertThat(stats.get("rating").get("distribution").get("5").asLong()).isZero();

        JsonNode students = get("/api/instructor/courses/" + courseId + "/students?page=0&size=2", instructor)
                .getBody();
        assertThat(students.get("totalElements").asLong()).isEqualTo(3);
        assertThat(students.get("totalPages").asInt()).isEqualTo(2);
        assertThat(students.get("size").asInt()).isEqualTo(2);
        assertThat(students.get("content")).hasSize(2);
        // En son kaydolan önce: Can, Burak; ilk sayfada Ayşe yok.
        assertThat(students.get("content").get(0).get("student").get("fullName").asText()).isEqualTo("Can Aksoy");
        JsonNode burakRow = students.get("content").get(1);
        assertThat(burakRow.get("student").get("fullName").asText()).isEqualTo("Burak Demir");
        assertThat(burakRow.get("progressPercent").asDouble()).isEqualTo(50.0);
        assertThat(burakRow.get("quizResults")).isEmpty();
        assertThat(burakRow.has("email")).isFalse();
        assertThat(burakRow.get("student").has("email")).isFalse();

        JsonNode ayseRow = get("/api/instructor/courses/" + courseId + "/students?page=1&size=2", instructor)
                .getBody().get("content").get(0);
        assertThat(ayseRow.get("student").get("fullName").asText()).isEqualTo("Ayşe Yılmaz");
        assertThat(ayseRow.get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(ayseRow.get("lastAccessedAt").isNull()).isFalse();
        assertThat(ayseRow.get("quizResults")).hasSize(1);
        assertThat(ayseRow.get("quizResults").get(0).get("quizId").asLong()).isEqualTo(quizId);
        assertThat(ayseRow.get("quizResults").get(0).get("bestPercent").asDouble()).isEqualTo(100.0);
        assertThat(ayseRow.get("quizResults").get(0).get("passed").asBoolean()).isTrue();

        // Yetki: başka eğitmen 403, kursiyer 403, olmayan kurs 404, girişsiz 401.
        String other = registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");
        for (String path : List.of("stats", "students")) {
            String url = "/api/instructor/courses/" + courseId + "/" + path;
            assertThat(get(url, other).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(get(url, ayse).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(get(url, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(get("/api/instructor/courses/9999/" + path, instructor).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    private String enroll(String name, String email, long courseId) {
        String token = registerAndLogin(name, email, "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", token, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return token;
    }

    private void attempt(long quizId, String token, long questionId, long optionId) {
        ResponseEntity<JsonNode> response = post("/api/quizzes/" + quizId + "/attempts", token, Map.of(
                "answers", List.of(Map.of("questionId", questionId, "optionIds", List.of(optionId)))));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private long draftCourse(String instructorToken) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        ResponseEntity<JsonNode> created = post("/api/courses", instructorToken, Map.of(
                "title", "Sıfırdan Java", "description", "Sıfırdan Java kursunun açıklaması.",
                "categoryId", categoryId, "level", "BEGINNER", "learningOutcomes", List.of("Temel sözdizimi")));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    private long lesson(long sectionId, String title, int position, long contentFileId) {
        return jdbc.queryForObject("""
                INSERT INTO lessons (section_id, content_file_id, title, content_type, mandatory, duration_seconds,
                                     position)
                VALUES (?, ?, ?, 'VIDEO', TRUE, 300, ?) RETURNING id
                """, Long.class, sectionId, contentFileId, title, position);
    }
}
