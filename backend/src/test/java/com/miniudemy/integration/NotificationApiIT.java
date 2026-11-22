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
 * Bildirim API'si (#125, FR-43). Gerçek HTTP ve gerçek PostgreSQL ile: bildirimler gerçek olaylarla (kurs onayı, iki
 * kayıt) doğar; eğitmen kendi bildirimlerini en yeni önce sayfalı listeler, sayacı görür, tek ve hepsini okundu
 * işaretler. Kursiyer eğitmenin bildirimini göremez ve işaretleyemez (404); eğitmenin bildirimi okunmamış kalır.
 * Kursiyerin "hepsini okundu"su da eğitmenin bildirimlerine dokunmaz.
 */
class NotificationApiIT extends IntegrationTestBase {

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};

    @Test
    void ownerListsCountsAndMarksRead_othersNotificationIsNotFound() {
        String instructor = registerAndLogin("Zeynep Kaya", "zeynep@example.com", "INSTRUCTOR");
        String admin = createAdminAndLogin("admin@example.com");
        long courseId = publishedCourse(instructor, admin);
        String ayse = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        String mehmet = registerAndLogin("Mehmet Demir", "mehmet@example.com", "STUDENT");
        assertThat(post("/api/courses/" + courseId + "/enroll", ayse, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(post("/api/courses/" + courseId + "/enroll", mehmet, null).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        // Eğitmenin üç bildirimi var: onay, sonra iki kayıt. Liste en yeni önce, hepsi okunmamış.
        JsonNode all = ok(get("/api/me/notifications", instructor));
        assertThat(types(all)).containsExactly("ENROLLMENT_CREATED", "ENROLLMENT_CREATED", "COURSE_APPROVED");
        assertThat(all.get("content").get(0).get("body").asText()).startsWith("Mehmet Demir");
        assertThat(all.get("content").findValues("read")).allMatch(read -> !read.asBoolean());
        assertThat(all.get("totalElements").asLong()).isEqualTo(3);
        assertThat(unreadCount(instructor)).isEqualTo(3);

        // Sayfalama: boyut 2 → iki sayfa; ikinci sayfada en eski bildirim (onay).
        JsonNode second = ok(get("/api/me/notifications?page=1&size=2", instructor));
        assertThat(types(second)).containsExactly("COURSE_APPROVED");
        assertThat(second.get("totalPages").asInt()).isEqualTo(2);
        long approvalId = second.get("content").get(0).get("id").asLong();

        // Kursiyer eğitmenin bildirimini göremez ve işaretleyemez; kendi listesi boş.
        ResponseEntity<JsonNode> foreign = post("/api/me/notifications/" + approvalId + "/read", ayse, null);
        assertThat(foreign.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(foreign.getBody().get("message").asText()).isEqualTo("Bildirim bulunamadı.");
        assertThat(ok(get("/api/me/notifications", ayse)).get("content")).isEmpty();
        assertThat(unreadCount(ayse)).isZero();
        assertThat(unreadCount(instructor)).isEqualTo(3);

        // Olmayan bildirim de 404.
        assertThat(post("/api/me/notifications/999999/read", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        // Tek okundu: sayaç düşer, ikinci kez işaretlemek de 204 (idempotent); unreadOnly yalnız okunmamışları verir.
        assertThat(post("/api/me/notifications/" + approvalId + "/read", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(post("/api/me/notifications/" + approvalId + "/read", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(instructor)).isEqualTo(2);
        assertThat(types(ok(get("/api/me/notifications?unreadOnly=true", instructor))))
                .containsExactly("ENROLLMENT_CREATED", "ENROLLMENT_CREATED");

        // Kursiyerin "hepsini okundu"su yalnız kendi bildirimlerine dokunur; eğitmenin okunmamışları yerinde kalır.
        assertThat(post("/api/me/notifications/read-all", ayse, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(instructor)).isEqualTo(2);

        // Hepsini okundu: sayaç sıfır; liste yine üç bildirim, hepsi okunmuş.
        assertThat(post("/api/me/notifications/read-all", instructor, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(instructor)).isZero();
        assertThat(ok(get("/api/me/notifications", instructor)).get("content").findValues("read"))
                .hasSize(3).allMatch(JsonNode::asBoolean);
    }

    @Test
    void notificationEndpoints_withoutToken_return401() {
        assertThat(get("/api/me/notifications", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/me/notifications/unread-count", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/me/notifications/read-all", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Yardımcılar ---

    private long unreadCount(String token) {
        return ok(get("/api/me/notifications/unread-count", token)).get("count").asLong();
    }

    private static List<String> types(JsonNode page) {
        List<String> types = new ArrayList<>();
        page.get("content").forEach(n -> types.add(n.get("type").asText()));
        return types;
    }

    private static JsonNode ok(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode()).as("yanıt: %s", response.getBody()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private long publishedCourse(String instructor, String admin) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class);
        long courseId = ok201(post("/api/courses", instructor, Map.of(
                "title", "Java'ya giriş", "description", "Bildirim API kursu.", "categoryId", categoryId,
                "level", "BEGINNER", "learningOutcomes", List.of("Temel sözdizimi"))));
        long sectionId = ok201(post("/api/courses/" + courseId + "/sections", instructor, Map.of("title", "Giriş")));
        long lessonId = ok201(post("/api/sections/" + sectionId + "/lessons", instructor, Map.of(
                "title", "Kurulum", "contentType", "VIDEO", "mandatory", true, "durationSeconds", 300)));
        assertThat(upload("/api/lessons/" + lessonId + "/content", instructor, "ders.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        ok(post("/api/courses/" + courseId + "/submit", instructor, null));
        ok(post("/api/admin/courses/" + courseId + "/approve", admin, null));
        return courseId;
    }

    private static long ok201(ResponseEntity<JsonNode> created) {
        assertThat(created.getStatusCode()).as("yanıt: %s", created.getBody()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }
}
