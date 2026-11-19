package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Senaryo 3: Kaldığı yerden devam (docs/use-cases.md, #65). Gerçek HTTP ve gerçek PostgreSQL ile.
 *
 * <p>Kursiyer bir dersin içeriğini açınca son izlenen ders kaydedilir (#119, {@code LessonOpened} olayı, commit'ten
 * sonra). Geri döndüğünde {@code GET /api/me/courses/{id}/resume} ve "kayıtlı kurslarım" listesi son dersi ve güncel
 * yüzdeyi verir. Hiç ders açılmamışsa ilk tamamlanmamış ders döner. Eğitmenin açtığı ders kursiyerin kaydını
 * değiştirmez; kayıtsız kursiyer bu bilgiyi göremez.
 *
 * <p>Kurs gerçek uçlarla hazırlanır ve yayımlanır; yalnız içerik dosyası, yükleme ucu (#112) main'de olmadığı için
 * {@link #storeFile} ile diske ve veritabanına doğrudan yazılır.
 */
class ResumeScenarioIT extends IntegrationTestBase {

    private static final byte[] VIDEO = new byte[2048];

    @Test
    void student_returns_andContinuesFromTheLastOpenedLesson_withCurrentProgress() {
        Course course = publishedCourseWithThreeLessons();
        String student = enrolledStudent(course, "ayse@example.com");
        String resume = "/api/me/courses/" + course.id() + "/resume";

        // Hiçbir ders açılmadı: ilk ders, %0, son erişim yok.
        JsonNode fresh = ok(get(resume, student));
        assertThat(fresh.get("lessonId").asLong()).isEqualTo(course.lesson(0));
        assertThat(fresh.get("progressPercent").asDouble()).isZero();
        assertThat(fresh.get("lastAccessedAt").isNull()).isTrue();

        // Üçüncü dersin içeriğini açar: kaldığı yer o ders olur.
        assertThat(download(content(course.lesson(2)), student, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode afterOpen = ok(get(resume, student));
        assertThat(afterOpen.get("lessonId").asLong()).isEqualTo(course.lesson(2));
        assertThat(afterOpen.get("lessonTitle").asText()).isEqualTo("Koleksiyonlar");
        assertThat(afterOpen.get("lastAccessedAt").isNull()).isFalse();

        // İlk dersi tamamlar: yüzde güncellenir, kaldığı yer değişmez.
        assertThat(post("/api/lessons/" + course.lesson(0) + "/complete", student, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Eğitmen başka bir dersi açar: kursiyerin kaydı değişmez.
        assertThat(download(content(course.lesson(1)), course.instructorToken(), null).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // Kursiyer daha sonra yeniden giriş yapıp döner.
        String returning = login("ayse@example.com", "Parola123!");
        JsonNode back = ok(get(resume, returning));
        assertThat(back.get("lessonId").asLong()).isEqualTo(course.lesson(2));
        assertThat(back.get("progressPercent").asDouble()).isCloseTo(33.33, within(0.01));

        // "Kayıtlı kurslarım" listesinde de aynı bilgi.
        JsonNode enrollment = ok(get("/api/me/enrollments", returning)).get(0);
        assertThat(enrollment.get("lastLessonId").asLong()).isEqualTo(course.lesson(2));
        assertThat(enrollment.get("progressPercent").asDouble()).isCloseTo(33.33, within(0.01));
        assertThat(enrollment.get("lastAccessedAt").isNull()).isFalse();
    }

    @Test
    void withoutOpenedLessons_resumePointsToTheFirstIncompleteLesson_andOthersCannotSeeIt() {
        Course course = publishedCourseWithThreeLessons();
        String student = enrolledStudent(course, "ayse@example.com");
        String resume = "/api/me/courses/" + course.id() + "/resume";

        // İçerik açmadan ilk dersi tamamlar: kaldığı yer ilk tamamlanmamış ders (ikinci ders).
        post("/api/lessons/" + course.lesson(0) + "/complete", student, null);
        JsonNode next = ok(get(resume, student));
        assertThat(next.get("lessonId").asLong()).isEqualTo(course.lesson(1));
        assertThat(next.get("lastAccessedAt").isNull()).isTrue();

        // Kayıtsız kursiyer 403, eğitmen (öğrenci değil) 403, girişsiz 401.
        String stranger = registerAndLogin("Can Demir", "can@example.com", "STUDENT");
        assertThat(get(resume, stranger).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(resume, course.instructorToken()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(resume, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Kayıtsız kursiyerin içerik isteği reddedilir ve kayıtlı kursiyerin kaydını etkilemez.
        assertThat(download(content(course.lesson(2)), stranger, null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ok(get(resume, student)).get("lessonId").asLong()).isEqualTo(course.lesson(1));
    }

    // --- Hazırlık ---

    private record Course(String instructorToken, long id, List<Long> lessons) {
        long lesson(int index) {
            return lessons.get(index);
        }
    }

    /** İki bölüm, üç zorunlu ders (Kurulum, İlk program | Koleksiyonlar); hepsinin içeriği var. Yayımlanmış. */
    private Course publishedCourseWithThreeLessons() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = ok(post("/api/courses", instructor, Map.of(
                "title", "Sıfırdan Java", "description", "Java'ya temelden giriş kursu.",
                "categoryId", categoryId, "level", "BEGINNER", "learningOutcomes", List.of("Temeller"))))
                .get("id").asLong();
        long basics = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Temeller")))
                .get("id").asLong();
        long more = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Devamı")))
                .get("id").asLong();
        long instructorId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'mehmet@example.com'", Long.class);
        List<Long> lessons = List.of(
                lessonWithContent(instructor, instructorId, basics, "Kurulum"),
                lessonWithContent(instructor, instructorId, basics, "İlk program"),
                lessonWithContent(instructor, instructorId, more, "Koleksiyonlar"));

        ok(post("/api/courses/" + courseId + "/submit", instructor, null));
        String admin = createAdminAndLogin("admin@example.com");
        assertThat(ok(post("/api/admin/courses/" + courseId + "/approve", admin, null)).get("status").asText())
                .isEqualTo("PUBLISHED");
        return new Course(instructor, courseId, lessons);
    }

    private long lessonWithContent(String instructor, long instructorId, long sectionId, String title) {
        long lessonId = ok(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of(
                "title", title, "contentType", "VIDEO", "mandatory", true, "durationSeconds", 300)))
                .get("id").asLong();
        long fileId = storeFile(instructorId, title + ".mp4", "video/mp4", VIDEO);
        jdbc.update("UPDATE lessons SET content_file_id = ? WHERE id = ?", fileId, lessonId);
        return lessonId;
    }

    private String enrolledStudent(Course course, String email) {
        String student = registerAndLogin("Ayşe Yılmaz", email, "STUDENT");
        ok(post("/api/courses/" + course.id() + "/enroll", student, null));
        return student;
    }

    private static String content(long lessonId) {
        return "/api/lessons/" + lessonId + "/content";
    }

    private static JsonNode ok(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("yanıt: %s", response.getBody()).isTrue();
        return response.getBody();
    }
}
