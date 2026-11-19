package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Platform raporu (#145) gerçek PostgreSQL'de: sayım sorguları (rol, durum, kategori, en çok kayıt, %100 ilerleme,
 * sertifika) doğru sayıları döndürüyor ve uç yalnız yöneticiye açık.
 */
class PlatformReportIT extends IntegrationTestBase {

    @Test
    void admin_seesTheCountsOfEveryModule() {
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");
        for (String email : List.of("ayse@example.com", "can@example.com", "elif@example.com")) {
            registerAndLogin("Kursiyer Hesap", email, "STUDENT");
        }
        String admin = createAdminAndLogin("admin@example.com");

        long python = course(instructor, "Sıfırdan Python", "PUBLISHED");
        long java = course(instructor, "Java", "PUBLISHED");
        course(instructor, "Web", "DRAFT");
        course(instructor, "SQL", "IN_REVIEW");
        // Python'a 3, Java'ya 1 kayıt. Ayşe Python'u bitirdi ve sertifika aldı.
        long ayseInPython = enroll("ayse@example.com", python);
        enroll("can@example.com", python);
        enroll("elif@example.com", python);
        enroll("ayse@example.com", java);
        jdbc.update("INSERT INTO course_progress (enrollment_id, progress_percent) VALUES (?, 100)", ayseInPython);
        jdbc.update("INSERT INTO certificates (enrollment_id, verification_code, issued_at) VALUES (?, ?, now())",
                ayseInPython, "K7Q2-M9XA-4T");

        ResponseEntity<JsonNode> response = get("/api/admin/reports", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode report = response.getBody();
        assertThat(report.get("from").isNull()).isTrue();
        assertThat(report.get("usersByRole").get("STUDENT").asLong()).isEqualTo(3);
        assertThat(report.get("usersByRole").get("INSTRUCTOR").asLong()).isEqualTo(2);
        assertThat(report.get("usersByRole").get("ADMIN").asLong()).isEqualTo(1);
        assertThat(report.get("coursesByStatus").get("PUBLISHED").asLong()).isEqualTo(2);
        assertThat(report.get("coursesByStatus").get("DRAFT").asLong()).isEqualTo(1);
        assertThat(report.get("coursesByStatus").get("IN_REVIEW").asLong()).isEqualTo(1);
        assertThat(report.get("coursesByStatus").get("UNPUBLISHED").asLong()).isZero();
        assertThat(report.get("enrollmentCount").asLong()).isEqualTo(4);
        assertThat(report.get("completionCount").asLong()).isEqualTo(1);
        assertThat(report.get("certificateCount").asLong()).isEqualTo(1);

        JsonNode top = report.get("topCourses");
        assertThat(top).hasSize(2);
        assertThat(top.get(0).get("title").asText()).isEqualTo("Sıfırdan Python");
        assertThat(top.get(0).get("enrollmentCount").asLong()).isEqualTo(3);
        assertThat(top.get(1).get("courseId").asLong()).isEqualTo(java);

        JsonNode categories = report.get("categoryDistribution");
        assertThat(categories.get(0).get("name").asText()).isEqualTo("Yazılım");
        assertThat(categories.get(0).get("courseCount").asLong()).isEqualTo(4);
        assertThat(categories).hasSize(5); // migration'daki 5 kategori; kursu olmayanlar 0 ile
    }

    @Test
    void report_isOnlyForAdmins() {
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        String instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");

        assertThat(get("/api/admin/reports", student).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/reports", instructor).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/reports", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private long course(String instructorToken, String title, String status) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long id = post("/api/courses", instructorToken, Map.of("title", title + " kursu",
                "description", title + " kursunun açıklaması.", "categoryId", categoryId, "level", "BEGINNER",
                "learningOutcomes", List.of("Temeller"))).getBody().get("id").asLong();
        jdbc.update("UPDATE courses SET title = ?, status = ? WHERE id = ?", title, status, id);
        return id;
    }

    private long enroll(String email, long courseId) {
        return jdbc.queryForObject("""
                INSERT INTO enrollments (student_id, course_id)
                VALUES ((SELECT id FROM users WHERE email = ?), ?) RETURNING id
                """, Long.class, email, courseId);
    }
}
