package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Senaryo 4: Sınav ve sertifika (docs/use-cases.md, #138). Gerçek HTTP ve gerçek PostgreSQL ile; kurs baştan sona
 * gerçek uçlarla kurulur (içerik yükleme dahil).
 *
 * <p>Kapsanan adımlar: sınavı açma, doğru cevaplar görünmeden (UC-15) → kalma ve önceki denemeler (3a, UC-16) →
 * geçme ve sonuç (puan, yüzde, geçti) → koşullar tamamlanınca doğrulama kodlu sertifika (4), eksik koşulda sertifika
 * yok (4a) → kursiyer sertifikasını görür (UC-17) ve kodu girişsiz doğrulanır (5, UC-06) → "sınav sonucu" ve
 * "sertifika hazır" bildirimleri kursiyerin bildirim listesinde, okunmamış sayısıyla (FR-43, FR-44) → kayıtsız
 * kursiyer sınava giremez (1a).
 *
 * <p>Görüntüleme ve doğrulamanın ayrıntıları (başkasının sertifikası 404, yanıt alanları) {@code CertificateScenarioIT}'te,
 * bildirim alıcıları {@code NotificationScenarioIT}'te; burada aynı kursiyerin akışında uçtan uca bağlanır (#138).
 */
class QuizCertificateScenarioIT extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4\n% Mini Udemy test\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    private record Setup(long courseId, long lessonId, long quizId, Map<Long, Long> correct, Map<Long, Long> wrong) {
    }

    @Test
    void student_failsThenPasses_seesAttempts_andGetsTheCertificateOnlyWhenEverythingIsDone() {
        Setup course = publishedCourseWithAQuiz();
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        assertThat(post("/api/courses/" + course.courseId() + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // 1. Sınavı açar: sorular ve şıklar var, doğru cevap bilgisi yok.
        ResponseEntity<JsonNode> quiz = get("/api/quizzes/" + course.quizId(), student);
        assertThat(quiz.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(quiz.getBody().get("questionCount").asInt()).isEqualTo(2);
        JsonNode firstOption = quiz.getBody().get("questions").get(0).get("options").get(0);
        assertThat(firstOption.has("text")).isTrue();
        assertThat(firstOption.has("correct")).isFalse();
        assertThat(quiz.getBody().toString()).doesNotContain("\"correct\"");

        // 3a. Yanlış cevaplarla kalır.
        JsonNode failed = submit(course, student, course.wrong());
        assertThat(failed.get("passed").asBoolean()).isFalse();
        assertThat(failed.get("percent").asDouble()).isZero();

        // 2–3. Doğru cevaplarla geçer: puan, en yüksek puan, yüzde, geçti.
        JsonNode passed = submit(course, student, course.correct());
        assertThat(passed.get("passed").asBoolean()).isTrue();
        assertThat(passed.get("score").asDouble()).isEqualTo(20.0);
        assertThat(passed.get("maxScore").asDouble()).isEqualTo(20.0);
        assertThat(passed.get("percent").asDouble()).isEqualTo(100.0);

        // UC-16: önceki denemeler listelenir.
        JsonNode attempts = get("/api/me/quizzes/" + course.quizId() + "/attempts", student).getBody();
        assertThat(attempts).hasSize(2);

        // 4a. Sınav geçildi ama zorunlu ders tamamlanmadı: sertifika yok.
        assertThat(certificateCount()).isZero();

        // 4. Zorunlu ders tamamlanınca doğrulama kodlu sertifika oluşur (bir kez).
        assertThat(post("/api/lessons/" + course.lessonId() + "/complete", student, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(certificateCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT verification_code FROM certificates", String.class))
                .matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{2}");

        // Sınavı yeniden geçmek ikinci sertifika üretmez.
        submit(course, student, course.correct());
        assertThat(certificateCount()).isEqualTo(1);

        // 4. Kursiyer sertifikasını "Sertifikalarım"da görür; kod veritabanındakiyle aynı.
        JsonNode mine = get("/api/me/certificates", student).getBody();
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("courseTitle").asText()).isEqualTo("Sıfırdan Python");
        assertThat(mine.get(0).get("studentName").asText()).isEqualTo("Ayşe Yılmaz");
        String code = mine.get(0).get("verificationCode").asText();
        assertThat(code).isEqualTo(jdbc.queryForObject("SELECT verification_code FROM certificates", String.class));

        // 5. Sertifikayı gören üçüncü kişi kodu giriş yapmadan doğrular.
        ResponseEntity<JsonNode> verified = get("/api/certificates/verify/" + code, null);
        assertThat(verified.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verified.getBody().get("valid").asBoolean()).isTrue();
        assertThat(verified.getBody().get("studentName").asText()).isEqualTo("Ayşe Yılmaz");
        assertThat(verified.getBody().get("courseTitle").asText()).isEqualTo("Sıfırdan Python");

        // Bildirimler (FR-43, FR-44): üç sınav sonucu ve bir "sertifika hazır", en yeni önce; hepsi okunmamış.
        JsonNode notifications = get("/api/me/notifications", student).getBody().get("content");
        assertThat(notifications).extracting(n -> n.get("type").asText() + ": " + n.get("title").asText())
                .containsExactly(
                        "QUIZ_COMPLETED: Sınavı geçtiniz",
                        "CERTIFICATE_ISSUED: Sertifikanız hazır",
                        "QUIZ_COMPLETED: Sınavı geçtiniz",
                        "QUIZ_COMPLETED: Sınavı geçemediniz");
        assertThat(notifications.get(1).get("link").asText()).isEqualTo("/certificates");
        assertThat(unreadCount(student)).isEqualTo(4);

        // Kursiyer hepsini okundu işaretler.
        assertThat(post("/api/me/notifications/read-all", student, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(student)).isZero();
    }

    @Test
    void unenrolledStudent_cannotOpenOrSubmitTheQuiz() {
        Setup course = publishedCourseWithAQuiz();
        String stranger = registerAndLogin("Can Demir", "can@example.com", "STUDENT");

        assertThat(get("/api/quizzes/" + course.quizId(), stranger).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(post("/api/quizzes/" + course.quizId() + "/attempts", stranger, answers(course.correct()))
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM quiz_attempts", Long.class)).isZero();
    }

    // --- Hazırlık ---

    /** Bir bölüm, bir zorunlu PDF dersi (gerçek yüklemeyle), iki soruluk zorunlu sınav (eşik 50). Yayımlanmış. */
    private Setup publishedCourseWithAQuiz() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = ok(post("/api/courses", instructor, Map.of("title", "Sıfırdan Python",
                "description", "Python'a temelden giriş kursu.", "categoryId", categoryId, "level", "BEGINNER",
                "learningOutcomes", List.of("Temeller")))).get("id").asLong();
        long sectionId = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Giriş")))
                .get("id").asLong();
        long lessonId = ok(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of("title", "Kurulum",
                "contentType", "PDF", "mandatory", true, "durationSeconds", 300))).get("id").asLong();
        ok(upload("/api/lessons/" + lessonId + "/content", instructor, "kurulum.pdf", PDF));

        JsonNode q1 = question(instructor, courseId, "Ekrana yazı yazdıran fonksiyon?", "print()", "echo()");
        JsonNode q2 = question(instructor, courseId, "Fonksiyon tanımlayan kelime?", "def", "func");
        long quizId = ok(post("/api/sections/" + sectionId + "/quizzes", instructor, Map.of("title", "Giriş sınavı",
                "scoringType", "SINGLE_ANSWER", "passThreshold", 50, "required", true,
                "questionIds", List.of(q1.get("id").asLong(), q2.get("id").asLong())))).get("id").asLong();

        ok(post("/api/courses/" + courseId + "/submit", instructor, null));
        String admin = createAdminAndLogin("admin@example.com");
        ok(post("/api/admin/courses/" + courseId + "/approve", admin, null));

        return new Setup(courseId, lessonId, quizId,
                Map.of(q1.get("id").asLong(), option(q1, true), q2.get("id").asLong(), option(q2, true)),
                Map.of(q1.get("id").asLong(), option(q1, false), q2.get("id").asLong(), option(q2, false)));
    }

    private JsonNode question(String instructor, long courseId, String text, String right, String wrongText) {
        return ok(post("/api/courses/" + courseId + "/questions", instructor, Map.of("text", text, "points", 10,
                "options", List.of(Map.of("text", right, "correct", true), Map.of("text", wrongText, "correct", false)))));
    }

    private static long option(JsonNode question, boolean correct) {
        for (JsonNode option : question.get("options")) {
            if (option.get("correct").asBoolean() == correct) {
                return option.get("id").asLong();
            }
        }
        throw new IllegalStateException("Şık bulunamadı");
    }

    private JsonNode submit(Setup course, String student, Map<Long, Long> chosen) {
        ResponseEntity<JsonNode> response = post("/api/quizzes/" + course.quizId() + "/attempts", student,
                answers(chosen));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private static Map<String, Object> answers(Map<Long, Long> chosen) {
        return Map.of("answers", chosen.entrySet().stream()
                .map(e -> Map.of("questionId", e.getKey(), "optionIds", List.of(e.getValue())))
                .toList());
    }

    private long unreadCount(String token) {
        return get("/api/me/notifications/unread-count", token).getBody().get("count").asLong();
    }

    private long certificateCount() {
        return jdbc.queryForObject("SELECT count(*) FROM certificates", Long.class);
    }

    private static JsonNode ok(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("yanıt: %s", response.getBody()).isTrue();
        return response.getBody();
    }
}
