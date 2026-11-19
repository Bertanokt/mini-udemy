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
 * Senaryo 5: Eğitmen analitiği (docs/use-cases.md, #138), {@link InstructorAnalyticsIT}'i (#133, #134) tamamlayan
 * adımlar: eğitmen panelinde kendi kursunu görür (adım 1) ve kursun değerlendirme istatistikleri gerçek yorumlarla
 * hesaplanır (adım 4: ortalama puan, puan dağılımı). Tamamlanma oranı, sınav başarı oranı, kursiyer listesi, "kayıt
 * yok" (2a) ve başka eğitmen 403 (3a) {@link InstructorAnalyticsIT}'te sınanıyor.
 */
class InstructorAnalyticsScenarioIT extends IntegrationTestBase {

    private static final byte[] PDF = "%PDF-1.4\n% Mini Udemy test\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    @Test
    void instructor_seesOwnCourseInThePanel_andRatingStatsFromRealReviews() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        long courseId = publishedCourse(instructor);
        int[] ratings = {5, 4, 4};
        for (int i = 0; i < ratings.length; i++) {
            String student = registerAndLogin("Kursiyer Hesap", "kursiyer" + i + "@example.com", "STUDENT");
            ok(post("/api/courses/" + courseId + "/enroll", student, null));
            ok(post("/api/courses/" + courseId + "/reviews", student,
                    Map.of("rating", ratings[i], "comment", "Yorum " + i)));
        }

        // 1. Eğitmen panelinde kendi kursu, durumuyla.
        JsonNode mine = ok(get("/api/instructor/courses", instructor));
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("id").asLong()).isEqualTo(courseId);
        assertThat(mine.get(0).get("status").asText()).isEqualTo("PUBLISHED");

        // 2 ve 4. İstatistik: 3 kayıt; puan ortalaması 4,33 → 4,3, dağılım 5:1, 4:2.
        JsonNode stats = ok(get("/api/instructor/courses/" + courseId + "/stats", instructor));
        assertThat(stats.get("enrollmentCount").asLong()).isEqualTo(3);
        JsonNode rating = stats.get("rating");
        assertThat(rating.get("reviewCount").asLong()).isEqualTo(3);
        assertThat(rating.get("averageRating").asDouble()).isEqualTo(4.3);
        assertThat(rating.get("distribution").get("5").asLong()).isEqualTo(1);
        assertThat(rating.get("distribution").get("4").asLong()).isEqualTo(2);
        assertThat(rating.get("distribution").get("1").asLong()).isZero();
    }

    private long publishedCourse(String instructor) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = ok(post("/api/courses", instructor, Map.of("title", "Sıfırdan Python",
                "description", "Python'a temelden giriş kursu.", "categoryId", categoryId, "level", "BEGINNER",
                "learningOutcomes", List.of("Temeller")))).get("id").asLong();
        long sectionId = ok(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Giriş")))
                .get("id").asLong();
        long lessonId = ok(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of("title", "Kurulum",
                "contentType", "PDF", "mandatory", true, "durationSeconds", 300))).get("id").asLong();
        ok(upload("/api/lessons/" + lessonId + "/content", instructor, "kurulum.pdf", PDF));
        ok(post("/api/courses/" + courseId + "/submit", instructor, null));
        ok(post("/api/admin/courses/" + courseId + "/approve", createAdminAndLogin("admin@example.com"), null));
        return courseId;
    }

    private static JsonNode ok(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).as("yanıt: %s", response.getBody()).isTrue();
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NO_CONTENT);
        return response.getBody();
    }
}
