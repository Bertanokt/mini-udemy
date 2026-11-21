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
 * Sertifika verme (#129, proje dokümanı H). Gerçek HTTP ve gerçek PostgreSQL ile: zorunlu dersler tamamlanıp zorunlu
 * sınav geçilince sertifika commit'ten sonra dinleyiciyle yazılır; isteğe bağlı ders ve zorunlu olmayan sınav koşul
 * değildir; kayıt başına tek sertifika.
 */
class CertificateScenarioIT extends IntegrationTestBase {

    private static final String CODE = "[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{4}"
            + "-[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{2}";

    /** Kurs: A bölümü (zorunlu L1, isteğe bağlı L2, zorunlu sınav), B bölümü (zorunlu L3, isteğe bağlı sınav). */
    private record Course(long id, long l1, long l2, long l3, long requiredQuiz, long optionalQuiz) {
    }

    @Test
    void certificateIsIssuedOnlyWhenEveryMandatoryLessonAndRequiredQuizIsDone_oncePerEnrollment() {
        Course course = publishedCourse();
        String student = enrolledStudent("Ayşe Yılmaz", "ayse@example.com", course);

        // Zorunlu dersler tamamlandı, zorunlu sınav henüz yok: sertifika yok.
        complete(student, course.l1());
        complete(student, course.l3());
        assertThat(certificateCount(course)).isZero();

        // İsteğe bağlı sınavı geçmek koşul değil.
        assertThat(attempt(student, course.optionalQuiz(), true)).isTrue();
        assertThat(certificateCount(course)).isZero();

        // Zorunlu sınavdan kalmak sertifika vermez.
        assertThat(attempt(student, course.requiredQuiz(), false)).isFalse();
        assertThat(certificateCount(course)).isZero();

        // Zorunlu sınav geçildi: son koşul QuizCompleted ile sağlandı, isteğe bağlı L2 tamamlanmadan sertifika var.
        assertThat(attempt(student, course.requiredQuiz(), true)).isTrue();
        assertThat(certificateCount(course)).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT c.enrollment_id, c.verification_code, c.issued_at, e.student_id
                FROM certificates c JOIN enrollments e ON e.id = c.enrollment_id WHERE e.course_id = ?""",
                course.id());
        assertThat((String) row.get("verification_code")).matches(CODE);
        assertThat(row.get("issued_at")).isNotNull();

        // Sonradan ders tamamlamak ve sınavı yeniden geçmek ikinci sertifika vermez.
        complete(student, course.l2());
        assertThat(attempt(student, course.requiredQuiz(), true)).isTrue();
        assertThat(certificateCount(course)).isEqualTo(1);
    }

    @Test
    void whenTheLastConditionIsALesson_lessonCompletionIssuesTheCertificate_andCodesAreUnique() {
        Course course = publishedCourse();
        String first = enrolledStudent("Ayşe Yılmaz", "ayse@example.com", course);
        String second = enrolledStudent("Mehmet Demir", "mehmet@example.com", course);

        for (String student : List.of(first, second)) {
            assertThat(attempt(student, course.requiredQuiz(), true)).isTrue();
            complete(student, course.l1());
        }
        assertThat(certificateCount(course)).isZero();

        // Son koşul ders: LessonCompleted yolu.
        complete(first, course.l3());
        assertThat(certificateCount(course)).isEqualTo(1);
        complete(second, course.l3());
        assertThat(certificateCount(course)).isEqualTo(2);

        List<String> codes = jdbc.queryForList("""
                SELECT c.verification_code FROM certificates c JOIN enrollments e ON e.id = c.enrollment_id
                WHERE e.course_id = ?""", String.class, course.id());
        assertThat(codes).hasSize(2).doesNotHaveDuplicates().allMatch(c -> c.matches(CODE));
    }

    @Test
    void studentSeesOwnCertificate_othersCannot_andAnyoneVerifiesItByCode() {
        Course course = publishedCourse();
        String ayse = enrolledStudent("Ayşe Yılmaz", "ayse@example.com", course);
        String mehmet = enrolledStudent("Mehmet Demir", "mehmet@example.com", course);
        complete(ayse, course.l1());
        complete(ayse, course.l3());
        assertThat(attempt(ayse, course.requiredQuiz(), true)).isTrue();
        assertThat(certificateCount(course)).isEqualTo(1);

        // Sertifikalarım (#130): kursiyerin kendi sertifikası, adlarla.
        ResponseEntity<JsonNode> mine = get("/api/me/certificates", ayse);
        assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mine.getBody()).hasSize(1);
        JsonNode certificate = mine.getBody().get(0);
        assertThat(certificate.get("courseId").asLong()).isEqualTo(course.id());
        assertThat(certificate.get("courseTitle").asText()).isEqualTo("Java'ya giriş");
        assertThat(certificate.get("studentName").asText()).isEqualTo("Ayşe Yılmaz");
        assertThat(certificate.get("instructorName").asText()).isEqualTo("Zeynep Kaya");
        assertThat(certificate.get("issuedAt").asText()).isNotBlank();
        String code = certificate.get("verificationCode").asText();
        assertThat(code).matches(CODE);
        long id = certificate.get("id").asLong();
        assertThat(get("/api/me/certificates/" + id, ayse).getBody().get("verificationCode").asText()).isEqualTo(code);

        // Başkası göremez: listesi boş, sertifikanın adresi 404.
        assertThat(get("/api/me/certificates", mehmet).getBody()).isEmpty();
        assertThat(get("/api/me/certificates/" + id, mehmet).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Herkese açık doğrulama: girişsiz; yalnız ad, kurs, tarih ve geçerlilik.
        ResponseEntity<JsonNode> verified = get("/api/certificates/verify/" + code, null);
        assertThat(verified.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verified.getBody().get("valid").asBoolean()).isTrue();
        assertThat(verified.getBody().get("studentName").asText()).isEqualTo("Ayşe Yılmaz");
        assertThat(verified.getBody().get("courseTitle").asText()).isEqualTo("Java'ya giriş");
        assertThat(verified.getBody().get("issuedAt").asText()).isEqualTo(certificate.get("issuedAt").asText());
        List<String> fields = new ArrayList<>();
        verified.getBody().fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder("valid", "courseTitle", "studentName", "issuedAt");
        assertThat(get("/api/certificates/verify/" + code.toLowerCase(), null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get("/api/certificates/verify/YOKK-YOKK-YO", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- Hazırlık ---

    private Course publishedCourse() {
        String instructor = registerAndLogin("Zeynep Kaya", "zeynep@example.com", "INSTRUCTOR");
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = id(post("/api/courses", instructor, Map.of(
                "title", "Java'ya giriş", "description", "Sertifika senaryosu kursu.", "categoryId", categoryId,
                "level", "BEGINNER", "learningOutcomes", List.of("Temel sözdizimi"))));
        long a = id(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Giriş")));
        long b = id(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Nesneler")));
        long l1 = lesson(instructor, a, "Kurulum", true);
        long l2 = lesson(instructor, a, "Ek okuma", false);
        long l3 = lesson(instructor, b, "Sınıflar", true);
        long q1 = question(instructor, courseId, "Java hangi yıl çıktı?");
        long q2 = question(instructor, courseId, "JVM neyin kısaltmasıdır?");
        long required = id(post("/api/sections/" + a + "/quizzes", instructor, Map.of("title", "Giriş sınavı",
                "scoringType", "SINGLE_ANSWER", "passThreshold", 70, "required", true, "questionIds", List.of(q1, q2))));
        long optional = id(post("/api/sections/" + b + "/quizzes", instructor, Map.of("title", "Alıştırma",
                "scoringType", "SINGLE_ANSWER", "passThreshold", 70, "required", false, "questionIds", List.of(q1))));
        assertThat(post("/api/courses/" + courseId + "/submit", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String admin = createAdminAndLogin("admin@example.com");
        assertThat(post("/api/admin/courses/" + courseId + "/approve", admin, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return new Course(courseId, l1, l2, l3, required, optional);
    }

    private long lesson(String instructor, long sectionId, String title, boolean mandatory) {
        long lessonId = id(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of(
                "title", title, "contentType", "VIDEO", "mandatory", mandatory, "durationSeconds", 300)));
        byte[] mp4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};
        assertThat(upload("/api/lessons/" + lessonId + "/content", instructor, "ders.mp4", mp4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return lessonId;
    }

    private long question(String instructor, long courseId, String text) {
        return id(post("/api/courses/" + courseId + "/questions", instructor, Map.of("text", text, "points", 5,
                "options", List.of(Map.of("text", "Doğru şık", "correct", true),
                        Map.of("text", "Yanlış şık", "correct", false)))));
    }

    private String enrolledStudent(String name, String email, Course course) {
        String student = registerAndLogin(name, email, "STUDENT");
        assertThat(post("/api/courses/" + course.id() + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        return student;
    }

    private void complete(String student, long lessonId) {
        assertThat(post("/api/lessons/" + lessonId + "/complete", student, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    /** Sınavı çözer: her soruda doğru (ya da yanlış) şık seçilir; denemenin geçip geçmediğini döner. */
    private boolean attempt(String student, long quizId, boolean correct) {
        ResponseEntity<JsonNode> quiz = get("/api/quizzes/" + quizId, student);
        assertThat(quiz.getStatusCode()).isEqualTo(HttpStatus.OK);
        String wanted = correct ? "Doğru şık" : "Yanlış şık";
        List<Map<String, Object>> answers = new ArrayList<>();
        for (JsonNode question : quiz.getBody().get("questions")) {
            for (JsonNode option : question.get("options")) {
                if (option.get("text").asText().equals(wanted)) {
                    answers.add(Map.of("questionId", question.get("id").asLong(),
                            "optionIds", List.of(option.get("id").asLong())));
                }
            }
        }
        ResponseEntity<JsonNode> result = post("/api/quizzes/" + quizId + "/attempts", student,
                Map.of("answers", answers));
        assertThat(result.getStatusCode()).as("yanıt: %s", result.getBody()).isEqualTo(HttpStatus.CREATED);
        return result.getBody().get("passed").asBoolean();
    }

    private long certificateCount(Course course) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM certificates c JOIN enrollments e ON e.id = c.enrollment_id
                WHERE e.course_id = ?""", Long.class, course.id());
    }

    private static long id(ResponseEntity<JsonNode> created) {
        assertThat(created.getStatusCode()).as("yanıt: %s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }
}
