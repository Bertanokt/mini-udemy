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

/**
 * Seed verisi (#67) gerçek PostgreSQL'e yüklenir ve uygulamanın kendi uçlarıyla okunur: hesaplara README'deki parolayla
 * girilir, katalog, ilerleme, sertifika ve dosyalar beklendiği gibidir. Runner {@code dev} profiline bağlı olduğu
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
                .containsExactly("ADMIN=1", "INSTRUCTOR=2", "STUDENT=6");
        assertThat(jdbc.queryForList("SELECT status FROM courses", String.class))
                .contains("DRAFT", "IN_REVIEW", "PUBLISHED", "UNPUBLISHED");
        assertThat(count("SELECT count(*) FROM certificates")).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM reviews")).isEqualTo(4);
        assertThat(count("SELECT count(*) FROM quizzes")).isEqualTo(2);

        // Veri varken ikinci çalıştırma hiçbir şeye dokunmaz.
        assertThat(runner.seed()).isFalse();
        assertThat(count("SELECT count(*) FROM users")).isEqualTo(9);
    }

    @Test
    void everyAccount_logsInWithTheReadmePassword() {
        for (String email : List.of("admin@miniudemy.dev", "mehmet.kaya@miniudemy.dev", "zeynep.ak@miniudemy.dev",
                "ayse.yilmaz@miniudemy.dev", "can.demir@miniudemy.dev", "elif.sahin@miniudemy.dev",
                "burak.celik@miniudemy.dev", "deniz.arslan@miniudemy.dev", "selin.koc@miniudemy.dev")) {
            assertThat(login(email, DemoSeedRunner.PASSWORD)).as(email).isNotBlank();
        }
    }

    @Test
    void catalog_showsOnlyPublishedCourses_withRatingsAndCovers() {
        JsonNode python = get("/api/courses?q=python", null).getBody();
        assertThat(python.get("totalElements").asLong()).isEqualTo(1); // "Python 2 (eski)" yayından kaldırılmış
        JsonNode card = python.get("content").get(0);
        assertThat(card.get("title").asText()).isEqualTo("Sıfırdan Python");
        assertThat(card.get("lessonCount").asInt()).isEqualTo(6);
        assertThat(card.get("reviewCount").asInt()).isEqualTo(3);
        assertThat(card.get("averageRating").asDouble()).isEqualTo(4.3);

        assertThat(get("/api/courses", null).getBody().get("totalElements").asLong()).isEqualTo(2);
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
    }

    @Test
    void runner_isOnlyActiveInTheDevProfile() {
        assertThat(DemoSeedRunner.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }
}
