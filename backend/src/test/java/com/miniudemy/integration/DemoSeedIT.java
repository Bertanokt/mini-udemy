package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.miniudemy.common.seed.DemoSeedRunner;
import com.miniudemy.notification.domain.NotificationContent;
import com.miniudemy.notification.domain.NotificationType;
import com.miniudemy.notification.service.NotificationMessages;

/**
 * Seed verisi (#67) gerçek PostgreSQL'e yüklenir ve uygulamanın kendi uçlarıyla okunur: hesaplara README'deki parolayla
 * girilir, katalog, ilerleme, sertifika, bildirimler ve dosyalar beklendiği gibidir. Runner {@code dev} profiline bağlı olduğu
 * için bu testte elle kurulur.
 */
class DemoSeedIT extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.storage.root}")
    private Path storageRoot;

    private DemoSeedRunner runner;

    @BeforeEach
    void seed() {
        runner = new DemoSeedRunner(jdbc, dataSource, passwordEncoder, transactionManager, storageRoot);
        assertThat(runner.seed()).isTrue();
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @Test
    void seed_createsTheAccountsAndCoursesOfEveryStatus_andRunsOnlyOnce() {
        assertThat(jdbc.queryForList("SELECT role, count(*) AS n FROM users GROUP BY role ORDER BY role"))
                .extracting(r -> r.get("role") + "=" + r.get("n"))
                .containsExactly("ADMIN=1", "INSTRUCTOR=3", "STUDENT=6");
        assertThat(jdbc.queryForList("SELECT status FROM courses", String.class))
                .contains("DRAFT", "IN_REVIEW", "PUBLISHED", "UNPUBLISHED");
        assertThat(count("SELECT count(*) FROM certificates")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM reviews")).isEqualTo(10);
        assertThat(count("SELECT count(*) FROM quizzes")).isEqualTo(11);
        assertThat(count("SELECT count(*) FROM complaints WHERE status = 'OPEN'")).isEqualTo(1);

        // Veri varken ikinci çalıştırma hiçbir şeye dokunmaz.
        assertThat(runner.seed()).isFalse();
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(10);
    }

    @Test
    void everyAccount_logsInWithTheReadmePassword() {
        for (String email : List.of("admin@miniudemy.dev", "mehmet.kaya@miniudemy.dev", "zeynep.ak@miniudemy.dev",
                "ayse.yilmaz@miniudemy.dev", "can.demir@miniudemy.dev", "elif.sahin@miniudemy.dev",
                "burak.celik@miniudemy.dev", "deniz.arslan@miniudemy.dev", "selin.koc@miniudemy.dev",
                "ece.yildiz@miniudemy.dev")) {
            assertThat(login(email, DemoSeedRunner.PASSWORD)).as(email).isNotBlank();
        }
    }

    @Test
    void catalog_showsOnlyPublishedCourses_withRatingsAndCovers() {
        JsonNode python = get("/api/courses?q=python", null).getBody();
        assertThat(python.get("totalElements").asLong()).isEqualTo(1); // "Python 2 (eski)" yayından kaldırılmış
        JsonNode card = python.get("content").get(0);
        assertThat(card.get("title").asText()).isEqualTo("Sıfırdan Python");
        assertThat(card.get("lessonCount").asInt()).isEqualTo(12);
        assertThat(card.get("reviewCount").asInt()).isEqualTo(3);
        assertThat(card.get("averageRating").asDouble()).isEqualTo(4.3);

        assertThat(get("/api/courses", null).getBody().get("totalElements").asLong()).isEqualTo(7);
        assertThat(count("SELECT count(*) FROM courses WHERE status = 'PUBLISHED' AND cover_file_id IS NULL")).isZero();
        ResponseEntity<byte[]> cover = download(card.get("coverUrl").asText(), null, null);
        assertThat(cover.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cover.getHeaders().getContentType()).hasToString("image/png");
    }

    @Test
    void students_haveDifferentProgressLevels_andAyseHasTheCertificate() {
        Map<String, Double> pythonProgress = Map.of(
                "ayse.yilmaz@miniudemy.dev", 100.0, "can.demir@miniudemy.dev", 66.67,
                "elif.sahin@miniudemy.dev", 50.0, "burak.celik@miniudemy.dev", 16.67,
                "deniz.arslan@miniudemy.dev", 0.0);
        long pythonId = count("SELECT id FROM courses WHERE title = 'Sıfırdan Python'");
        pythonProgress.forEach((email, percent) -> {
            String token = login(email, DemoSeedRunner.PASSWORD);
            JsonNode progress = get("/api/me/courses/" + pythonId + "/progress", token).getBody();
            assertThat(progress.get("progressPercent").asDouble()).as(email).isEqualTo(percent);
        });

        // Can kaldığı yerden devam eder: son açtığı ders.
        JsonNode resume = get("/api/me/courses/" + pythonId + "/resume",
                login("can.demir@miniudemy.dev", DemoSeedRunner.PASSWORD)).getBody();
        assertThat(resume.get("lessonTitle").asText()).isEqualTo("Fonksiyon Tanımlama");

        assertThat(jdbc.queryForObject("""
                SELECT u.email FROM certificates c
                JOIN enrollments e ON e.id = c.enrollment_id JOIN users u ON u.id = e.student_id
                """, String.class)).isEqualTo("ayse.yilmaz@miniudemy.dev");
    }

    @Test
    void notifications_matchTheSeededEvents_andUseTheSameTextsAsTheApplication() {
        // Kayıt → eğitmen, onay/red → eğitmen, sınav ve sertifika → kursiyer.
        assertThat(jdbc.queryForList("""
                SELECT u.email || '=' || count(*) FROM notifications n JOIN users u ON u.id = n.user_id
                GROUP BY u.email ORDER BY u.email
                """, String.class)).containsExactly("ayse.yilmaz@miniudemy.dev=3", "burak.celik@miniudemy.dev=1", "can.demir@miniudemy.dev=2",
                "ece.yildiz@miniudemy.dev=4", "elif.sahin@miniudemy.dev=1", "mehmet.kaya@miniudemy.dev=9",
                "zeynep.ak@miniudemy.dev=8");
        // Son 7 günün bildirimleri okunmamış: 7 kayıt, red, Elif'in, Can'in ve Burak'ın sınavı.
        assertThat(count("SELECT count(*) FROM notifications WHERE read_at IS NULL")).isEqualTo(11);

        long python = count("SELECT id FROM courses WHERE title = 'Sıfırdan Python'");
        long giris = count("SELECT id FROM quizzes WHERE title = 'Giriş sınavı'");
        String web = "Web Geliştirmeye Giriş";
        String reason = jdbc.queryForObject("SELECT rejection_reason FROM courses WHERE title = ?", String.class, web);

        assertThat(notification("zeynep.ak@miniudemy.dev", NotificationType.COURSE_REJECTED))
                .containsExactly(NotificationMessages.courseRejected(web, reason));
        assertThat(notification("mehmet.kaya@miniudemy.dev", NotificationType.COURSE_APPROVED))
                .hasSize(2)
                .contains(NotificationMessages.courseApproved(python, "Sıfırdan Python"));
        assertThat(notification("mehmet.kaya@miniudemy.dev", NotificationType.ENROLLMENT_CREATED))
                .hasSize(7)
                .contains(NotificationMessages.enrollmentCreated(python, "Sıfırdan Python", "Deniz Arslan"));
        assertThat(notification("elif.sahin@miniudemy.dev", NotificationType.QUIZ_COMPLETED))
                .containsExactly(NotificationMessages.quizCompleted(giris, "Sıfırdan Python", false));
        assertThat(notification("ayse.yilmaz@miniudemy.dev", NotificationType.CERTIFICATE_ISSUED))
                .containsExactly(NotificationMessages.certificateIssued("Sıfırdan Python"));
    }

    private List<NotificationContent> notification(String email, NotificationType type) {
        return jdbc.query("""
                SELECT n.title, n.body, n.link FROM notifications n JOIN users u ON u.id = n.user_id
                WHERE u.email = ? AND n.type = ? ORDER BY n.created_at
                """, (rs, i) -> new NotificationContent(type, rs.getString("title"), rs.getString("body"),
                rs.getString("link")), email, type.name());
    }

    @Test
    void lessonFiles_areCopiedToStorage_andServedToEnrolledStudents() throws IOException {
        String token = login("ayse.yilmaz@miniudemy.dev", DemoSeedRunner.PASSWORD);
        long lessonId = count("SELECT id FROM lessons WHERE title = 'Kurulum' AND content_file_id IS NOT NULL");
        String key = jdbc.queryForObject("""
                SELECT f.storage_key FROM lessons l JOIN stored_files f ON f.id = l.content_file_id WHERE l.id = ?
                """, String.class, lessonId);

        ResponseEntity<byte[]> pdf = download("/api/lessons/" + lessonId + "/content", token, null);

        assertThat(pdf.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdf.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(pdf.getBody()).isEqualTo(new ClassPathResource("seed/files/" + key).getContentAsByteArray());
        assertThat(count("SELECT count(*) FROM stored_files WHERE size_bytes = 0")).isZero();
        assertThat(count("SELECT count(*) FROM lessons WHERE content_file_id IS NULL")).isZero();
    }

    @Test
    void seedVideos_arePlayableMp4_andSeekableWithRange() {
        String token = login("ayse.yilmaz@miniudemy.dev", DemoSeedRunner.PASSWORD);
        long lessonId = count("SELECT id FROM lessons WHERE title = 'İlk Program' AND content_type = 'VIDEO'");

        ResponseEntity<byte[]> part = download("/api/lessons/" + lessonId + "/content", token, "bytes=1000-1999");

        assertThat(part.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(part.getHeaders().getContentType()).hasToString("video/mp4");
        assertThat(part.getBody()).hasSize(1000);
        // Her kursta en az iki video ders (seed içeriği kuralı, scripts/seed-content).
        assertThat(count("""
                SELECT count(*) FROM courses co WHERE (SELECT count(*) FROM lessons l JOIN sections s ON s.id = l.section_id
                WHERE s.course_id = co.id AND l.content_type = 'VIDEO') < 2
                """)).isZero();
    }

    @Test
    void runner_isOnlyActiveInTheDevProfile() {
        assertThat(DemoSeedRunner.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }
}
