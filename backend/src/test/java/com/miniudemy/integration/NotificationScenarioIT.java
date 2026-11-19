package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Bildirimler (#124, FR-43). Gerçek HTTP ve gerçek PostgreSQL ile altı olayın her biri commit'ten sonra doğru alıcıya
 * tek bildirim yazar; yayıncının işi başarısız olursa (mükerrer kayıt 409) bildirim yazılmaz.
 */
class NotificationScenarioIT extends IntegrationTestBase {

    private static final String INSTRUCTOR = "zeynep@example.com";
    private static final String STUDENT = "ayse@example.com";
    private static final String OTHER_STUDENT = "mehmet@example.com";

    @Test
    void sixEventsNotifyTheRightRecipientOnce() {
        String instructor = registerAndLogin("Zeynep Kaya", INSTRUCTOR, "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = id(post("/api/courses", instructor, Map.of(
                "title", "Java'ya giriş", "description", "Bildirim senaryosu kursu.", "categoryId", categoryId,
                "level", "BEGINNER", "learningOutcomes", List.of("Temel sözdizimi"))));
        long sectionId = id(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Giriş")));
        long lessonId = lesson(instructor, sectionId, "Kurulum", true);
        long questionId = id(post("/api/courses/" + courseId + "/questions", instructor, Map.of(
                "text", "Java hangi yıl çıktı?", "points", 5,
                "options", List.of(Map.of("text", "Doğru şık", "correct", true),
                        Map.of("text", "Yanlış şık", "correct", false)))));
        long quizId = id(post("/api/sections/" + sectionId + "/quizzes", instructor, Map.of("title", "Giriş sınavı",
                "scoringType", "SINGLE_ANSWER", "passThreshold", 70, "required", true,
                "questionIds", List.of(questionId))));

        // Kurs reddi → eğitmene, gerekçesiyle.
        assertOk(post("/api/courses/" + courseId + "/submit", instructor, null));
        assertOk(post("/api/admin/courses/" + courseId + "/reject", admin, Map.of("reason", "Örnekler yetersiz.")));
        assertThat(notifications(INSTRUCTOR)).containsExactly(tuple("COURSE_REJECTED",
                "\"Java'ya giriş\" kursunuz reddedildi. Gerekçe: Örnekler yetersiz.", "/instructor"));

        // Kurs onayı → eğitmene.
        assertOk(post("/api/courses/" + courseId + "/submit", instructor, null));
        assertOk(post("/api/admin/courses/" + courseId + "/approve", admin, null));
        assertThat(notifications(INSTRUCTOR)).last().isEqualTo(tuple("COURSE_APPROVED",
                "\"Java'ya giriş\" kursunuz onaylandı ve katalogda yayımlandı.", "/courses/" + courseId));

        // Kursa kayıt → eğitmene; mükerrer kayıt (409) ikinci bildirim üretmez.
        String student = registerAndLogin("Ayşe Yılmaz", STUDENT, "STUDENT");
        String otherStudent = registerAndLogin("Mehmet Demir", OTHER_STUDENT, "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/courses/" + courseId + "/enroll", student, null).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(notifications(INSTRUCTOR)).hasSize(3).last().isEqualTo(tuple("ENROLLMENT_CREATED",
                "Ayşe Yılmaz \"Java'ya giriş\" kursunuza kaydoldu.", "/instructor/courses/" + courseId + "/analytics"));

        // Yayımlanmış kursa yeni ders → yalnız kayıtlı kursiyerlere; eğitmene ve kayıtsız kursiyere yok.
        long newLessonId = lesson(instructor, sectionId, "Ek okuma", false);
        assertThat(notifications(STUDENT)).containsExactly(tuple("LESSON_PUBLISHED",
                "\"Java'ya giriş\" kursuna yeni bir ders eklendi.", "/learn/" + courseId + "/" + newLessonId));
        assertThat(notifications(OTHER_STUDENT)).isEmpty();
        assertThat(notifications(INSTRUCTOR)).hasSize(3);

        // Sınav sonucu → kursiyere (kaldı).
        assertThat(attempt(student, quizId, false)).isFalse();
        assertThat(notifications(STUDENT)).hasSize(2).last().isEqualTo(tuple("QUIZ_COMPLETED",
                "\"Java'ya giriş\" kursundaki sınavı geçemediniz.", "/quizzes/" + quizId + "/attempts"));

        // Zorunlu ders tamamlanıp sınav geçilince: sınav sonucu ve sertifika → kursiyere. İkisi de aynı QuizCompleted
        // teslimatında doğar (sertifikayı CertificateListener verir); dinleyicilerin sırası tanımsız olduğu için
        // aralarındaki sıraya bakılmaz.
        assertOk(post("/api/lessons/" + lessonId + "/complete", student, null));
        assertThat(attempt(student, quizId, true)).isTrue();
        List<Tuple> studentNotifications = notifications(STUDENT);
        assertThat(studentNotifications).hasSize(4);
        assertThat(studentNotifications.subList(2, 4)).containsExactlyInAnyOrder(
                tuple("QUIZ_COMPLETED", "\"Java'ya giriş\" kursundaki sınavı geçtiniz.",
                        "/quizzes/" + quizId + "/attempts"),
                tuple("CERTIFICATE_ISSUED", "\"Java'ya giriş\" kursunu tamamladınız; sertifikanız oluşturuldu.",
                        "/my-courses"));

        assertThat(notifications(OTHER_STUDENT)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE read_at IS NOT NULL", Long.class))
                .as("yeni bildirimler okunmamış").isZero();
    }

    // --- Yardımcılar ---

    /** Kullanıcının bildirimleri yazılış sırasıyla: (tür, gövde, bağlantı). */
    private List<Tuple> notifications(String email) {
        return jdbc.query("""
                SELECT n.type, n.body, n.link FROM notifications n JOIN users u ON u.id = n.user_id
                WHERE u.email = ? ORDER BY n.id""",
                (rs, i) -> tuple(rs.getString("type"), rs.getString("body"), rs.getString("link")), email);
    }

    private long lesson(String instructor, long sectionId, String title, boolean mandatory) {
        long lessonId = id(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of(
                "title", title, "contentType", "VIDEO", "mandatory", mandatory, "durationSeconds", 300)));
        byte[] mp4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};
        assertThat(upload("/api/lessons/" + lessonId + "/content", instructor, "ders.mp4", mp4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        return lessonId;
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

    private static void assertOk(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode()).as("yanıt: %s", response.getBody()).isEqualTo(HttpStatus.OK);
    }

    private static long id(ResponseEntity<JsonNode> created) {
        assertThat(created.getStatusCode()).as("yanıt: %s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }
}
