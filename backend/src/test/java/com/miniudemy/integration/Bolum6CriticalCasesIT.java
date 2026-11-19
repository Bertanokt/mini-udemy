package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Proje dokümanı Bölüm 6'nın açıkça istediği kritik test durumları (#139; docs/test-plan.md › 5), gerçek HTTP ve
 * gerçek PostgreSQL ile. Bu testler silinmez ve {@code @Disabled} yapılmaz (CLAUDE.md). Kurs her testte gerçek
 * uçlarla kurulur (içerik yükleme dahil).
 *
 * <ol>
 *   <li>Kursa kayıt olmadan özel derse/içeriğe erişim → 403</li>
 *   <li>Mükerrer kayıt → 409, veritabanında tek kayıt</li>
 *   <li>Yetkisiz kurs düzenleme (başka eğitmen, kursiyer) → 403, kurs değişmez</li>
 *   <li>İlerleme hesabı: 20 dersin 15'i → %75; hiç ders tamamlanmamış → %0</li>
 *   <li>Dosya türü ihlali → 415, boyut ihlali → 413</li>
 * </ol>
 */
class Bolum6CriticalCasesIT extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4\n% Mini Udemy test\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    private record Course(String instructor, long id, List<Long> lessons) {
    }

    @Test
    void case1_unenrolledStudent_cannotOpenPrivateLessonContent() {
        Course course = publishedCourse(2);
        String stranger = registerAndLogin("Can Demir", "can@example.com", "STUDENT");
        String content = "/api/lessons/" + course.lessons().get(0) + "/content";

        assertThat(download(content, stranger, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/lessons/" + course.lessons().get(0) + "/content-token", stranger, null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/lessons/" + course.lessons().get(0) + "/complete", stranger, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // Kayıt olunca aynı içerik açılır: 403'ün sebebi kayıt.
        ok(post("/api/courses/" + course.id() + "/enroll", stranger, null));
        assertThat(download(content, stranger, null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void case2_duplicateEnrollment_returns409_andKeepsASingleRow() {
        Course course = publishedCourse(1);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");

        assertThat(post("/api/courses/" + course.id() + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/courses/" + course.id() + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments WHERE course_id = ?", Long.class,
                course.id())).isEqualTo(1);
    }

    @Test
    void case3_anotherInstructorOrAStudent_cannotEditTheCourse() {
        String owner = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = draftCourse(owner, "Sıfırdan Python");
        long sectionId = ok(post("/api/courses/" + courseId + "/sections", owner, Map.of("title", "Giriş")))
                .get("id").asLong();
        String other = registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        Map<String, Object> edit = courseBody("Ele geçirilmiş kurs");

        for (String intruder : List.of(other, student)) {
            assertThat(put("/api/courses/" + courseId, intruder, edit).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(delete("/api/courses/" + courseId, intruder).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(post("/api/courses/" + courseId + "/sections", intruder, Map.of("title", "Sızma"))
                    .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(put("/api/sections/" + sectionId, intruder, Map.of("title", "Sızma")).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(post("/api/sections/" + sectionId + "/lessons", intruder, lessonBody("Sızma", "VIDEO"))
                    .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        assertThat(jdbc.queryForObject("SELECT title FROM courses WHERE id = ?", String.class, courseId))
                .isEqualTo("Sıfırdan Python");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sections WHERE course_id = ?", Long.class, courseId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT title FROM sections WHERE id = ?", String.class, sectionId))
                .isEqualTo("Giriş");
    }

    @Test
    void case4_progress_15of20Lessons_is75Percent_andNoLessonIsZero() {
        Course course = publishedCourse(20);
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        ok(post("/api/courses/" + course.id() + "/enroll", student, null));
        String progress = "/api/me/courses/" + course.id() + "/progress";

        assertThat(ok(get(progress, student)).get("progressPercent").asDouble()).isZero();

        for (long lessonId : course.lessons().subList(0, 15)) {
            ok(post("/api/lessons/" + lessonId + "/complete", student, null));
        }
        // Aynı dersi tekrar tamamlamak sayıyı değiştirmez.
        ok(post("/api/lessons/" + course.lessons().get(0) + "/complete", student, null));

        JsonNode result = ok(get(progress, student));
        assertThat(result.get("progressPercent").asDouble()).isEqualTo(75.0);
        assertThat(result.get("completedLessonIds")).hasSize(15);
        // İlk bölümün 10 dersi bitti, ikincinin 10 dersinden 5'i.
        assertThat(result.get("sections").get(0).get("progressPercent").asDouble()).isEqualTo(100.0);
        assertThat(result.get("sections").get(1).get("progressPercent").asDouble()).isEqualTo(50.0);
        // "Kayıtlı kurslarım" listesi de aynı yüzdeyi verir.
        assertThat(ok(get("/api/me/enrollments", student)).get(0).get("progressPercent").asDouble())
                .isEqualTo(75.0);
    }

    @Test
    void case5_fileTypeViolation_is415_andSizeViolation_is413_andNothingIsStored() {
        String owner = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = draftCourse(owner, "Sıfırdan Python");
        long sectionId = ok(post("/api/courses/" + courseId + "/sections", owner, Map.of("title", "Giriş")))
                .get("id").asLong();
        long videoLesson = ok(post("/api/sections/" + sectionId + "/lessons", owner, lessonBody("Video", "VIDEO")))
                .get("id").asLong();

        // Uzantısı .mp4 olan metin dosyası: tür içerikten anlaşılır → 415.
        byte[] text = "bu bir video değil".getBytes(StandardCharsets.UTF_8);
        assertThat(upload("/api/lessons/" + videoLesson + "/content", owner, "ders.mp4", text).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        // Kapak sınırı 2 MB: bir bayt fazlası → 413.
        byte[] bigCover = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 0, bigCover, 0, 8);
        assertThat(upload("/api/courses/" + courseId + "/cover", owner, "kapak.png", bigCover).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    // --- Hazırlık ---

    /** İki bölüme bölünmüş {@code lessonCount} zorunlu ders; ilkinin içeriği gerçek yüklemeyle. Yayımlanmış. */
    private Course publishedCourse(int lessonCount) {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = draftCourse(instructor, "Sıfırdan Python");
        long first = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Birinci")))
                .get("id").asLong();
        long second = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "İkinci")))
                .get("id").asLong();
        List<Long> lessons = new ArrayList<>();
        for (int i = 0; i < lessonCount; i++) {
            long section = i < (lessonCount + 1) / 2 ? first : second;
            Map<String, Object> body = lessonBody("Ders " + (i + 1), "PDF");
            lessons.add(ok(post("/api/sections/" + section + "/lessons", instructor, body)).get("id").asLong());
        }
        ok(upload("/api/lessons/" + lessons.get(0) + "/content", instructor, "ders.pdf", PDF));
        ok(post("/api/courses/" + courseId + "/submit", instructor, null));
        ok(post("/api/admin/courses/" + courseId + "/approve", createAdminAndLogin("admin@example.com"), null));
        return new Course(instructor, courseId, lessons);
    }

    private long draftCourse(String instructor, String title) {
        return ok(post("/api/courses", instructor, courseBody(title))).get("id").asLong();
    }

    private Map<String, Object> courseBody(String title) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        return Map.of("title", title, "description", title + " kursunun açıklaması.", "categoryId", categoryId,
                "level", "BEGINNER", "learningOutcomes", List.of("Temeller"));
    }

    private static Map<String, Object> lessonBody(String title, String type) {
        return Map.of("title", title, "contentType", type, "mandatory", true, "durationSeconds", 300);
    }

    private static JsonNode ok(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("yanıt: %s", response.getBody()).isTrue();
        return response.getBody();
    }
}
