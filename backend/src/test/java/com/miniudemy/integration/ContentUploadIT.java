package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * İçerik ve kapak yükleme (#112): gerçek HTTP, gerçek PostgreSQL, gerçek disk. Yükle → indir, eski dosyanın diskten
 * silinmesi, tür/boyut denetimi (proje dokümanı Bölüm 6), sahiplik ve kurs durumu.
 */
class ContentUploadIT extends IntegrationTestBase {

    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 1, 2, 3, 4};
    private static final byte[] MP4_2 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 9, 9, 9, 9};
    private static final byte[] PDF = "%PDF-1.7\n1 0 obj\n<< >>\nendobj\n%%EOF\n".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] PNG_2 = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 7, 7, 7};

    private String instructor;
    private long courseId;
    private long sectionId;
    private long lessonId;

    @BeforeEach
    void course() {
        instructor = registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR");
        courseId = create("/api/courses", Map.of("title", "Sıfırdan Java", "description", "Sıfırdan Java kursunun açıklaması.",
                "categoryId", jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Yazılım'", Long.class),
                "level", "BEGINNER", "learningOutcomes", List.of("Temel")));
        sectionId = create("/api/courses/" + courseId + "/sections", Map.of("title", "Giriş"));
        lessonId = lesson("Kurulum", "VIDEO");
    }

    @Test
    void uploadedVideo_isLinkedToTheLesson_andDownloadsByteForByte() {
        ResponseEntity<JsonNode> uploaded = upload("/api/lessons/" + lessonId + "/content", instructor, "Kurulum.mp4", MP4);

        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploaded.getBody().get("hasContent").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT mime_type FROM stored_files", String.class)).isEqualTo("video/mp4");
        ResponseEntity<byte[]> downloaded = download("/api/lessons/" + lessonId + "/content", instructor, null);
        assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(downloaded.getBody()).isEqualTo(MP4);
    }

    @Test
    void replacingTheContent_deletesTheOldFileFromDiskAndDatabase() {
        upload("/api/lessons/" + lessonId + "/content", instructor, "bir.mp4", MP4);
        String oldKey = jdbc.queryForObject("SELECT storage_key FROM stored_files", String.class);
        assertThat(fileOnDisk(oldKey)).isTrue();

        upload("/api/lessons/" + lessonId + "/content", instructor, "iki.mp4", MP4_2);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isEqualTo(1);
        assertThat(fileOnDisk(oldKey)).isFalse();
        String newKey = jdbc.queryForObject("SELECT storage_key FROM stored_files", String.class);
        assertThat(fileOnDisk(newKey)).isTrue();
        assertThat(download("/api/lessons/" + lessonId + "/content", instructor, null).getBody()).isEqualTo(MP4_2);
    }

    @Test
    void wrongTypeForTheLesson_returns415_andLeavesNothingOnDisk() {
        ResponseEntity<JsonNode> pdfIntoVideo = upload("/api/lessons/" + lessonId + "/content", instructor, "x.mp4", PDF);

        assertThat(pdfIntoVideo.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(pdfIntoVideo.getBody().get("message").asText()).isEqualTo("Yalnız mp4 ve webm videolar yüklenebilir.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT content_file_id FROM lessons WHERE id = ?", Long.class, lessonId)).isNull();
    }

    @Test
    void textFileRenamedToMp4_returns415() {
        ResponseEntity<JsonNode> response = upload("/api/lessons/" + lessonId + "/content", instructor, "video.mp4",
                "bu bir video değil".getBytes());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    @Test
    void oversizedPdf_returns413_andLeavesNothingOnDisk() {
        long pdfLesson = lesson("Notlar", "PDF");
        byte[] big = new byte[20 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);

        ResponseEntity<JsonNode> response = upload("/api/lessons/" + pdfLesson + "/content", instructor, "notlar.pdf", big);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().get("message").asText()).isEqualTo("PDF en fazla 20 MB olabilir.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    @Test
    void anotherInstructor_studentAndAnonymous_cannotUpload() {
        String other = registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR");
        String student = registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT");
        String path = "/api/lessons/" + lessonId + "/content";

        assertThat(upload(path, other, "a.mp4", MP4).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(upload(path, student, "a.mp4", MP4).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(upload(path, null, "a.mp4", MP4).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(upload("/api/lessons/99999/content", instructor, "a.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    @Test
    void publishedCourse_acceptsContentForALessonWithoutOne_butNotAReplacement() {
        long withContent = lesson("Dolu", "VIDEO");
        upload("/api/lessons/" + withContent + "/content", instructor, "a.mp4", MP4);
        setStatus("PUBLISHED");

        // FR-43: yayımlanmış kursa sonradan eklenen ders içeriksiz gelir; içeriği yüklenebilir.
        long added = lesson("Yeni", "VIDEO");
        assertThat(upload("/api/lessons/" + added + "/content", instructor, "b.mp4", MP4_2).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // Mevcut içeriği değiştirmek dersi değiştirmektir: yayımlanmış kurs kilitli.
        assertThat(upload("/api/lessons/" + withContent + "/content", instructor, "c.mp4", MP4_2).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isEqualTo(2);
    }

    @Test
    void courseInReview_rejectsUploads_forContentAndCover() {
        setStatus("IN_REVIEW");

        assertThat(upload("/api/lessons/" + lessonId + "/content", instructor, "a.mp4", MP4).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(upload("/api/courses/" + courseId + "/cover", instructor, "k.png", PNG).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    @Test
    void cover_isUploaded_servedPublicly_andReplacedFileIsDeleted() {
        ResponseEntity<JsonNode> first = upload("/api/courses/" + courseId + "/cover", instructor, "kapak.png", PNG);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().get("coverUrl").asText()).isEqualTo("/api/courses/" + courseId + "/cover");
        String oldKey = jdbc.queryForObject("SELECT storage_key FROM stored_files", String.class);

        assertThat(upload("/api/courses/" + courseId + "/cover", instructor, "yeni.png", PNG_2).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(fileOnDisk(oldKey)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isEqualTo(1);
        assertThat(download("/api/courses/" + courseId + "/cover", null, null).getBody()).isEqualTo(PNG_2);
    }

    @Test
    void cover_rejectsOtherTypesAndMoreThan2MB() {
        String path = "/api/courses/" + courseId + "/cover";
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        assertThat(upload(path, instructor, "kapak.png", PDF).getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(upload(path, instructor, "kapak.png", big).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(upload(path, registerAndLogin("Zeynep Ak", "zeynep@example.com", "INSTRUCTOR"), "k.png", PNG)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
    }

    @Test
    void deletingTheLesson_changingItsType_andDeletingTheSection_removeTheirFilesFromDisk() {
        long toDelete = lesson("Silinecek", "VIDEO");
        long toRetype = lesson("Türü değişecek", "VIDEO");
        long inSection = lesson("Bölümle gidecek", "VIDEO");
        List<Long> lessons = List.of(toDelete, toRetype, inSection);
        lessons.forEach(id -> upload("/api/lessons/" + id + "/content", instructor, "a.mp4", MP4));
        List<String> keys = jdbc.queryForList("SELECT storage_key FROM stored_files", String.class);
        assertThat(keys).hasSize(3).allMatch(this::fileOnDisk);

        assertThat(http.exchange("/api/lessons/" + toDelete, org.springframework.http.HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(bearer()), Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(put("/api/lessons/" + toRetype, instructor, Map.of("title", "Türü değişecek",
                "contentType", "PDF", "mandatory", true, "durationSeconds", 60)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.exchange("/api/sections/" + sectionId, org.springframework.http.HttpMethod.DELETE,
                new org.springframework.http.HttpEntity<>(bearer()), Void.class).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files", Long.class)).isZero();
        assertThat(keys).noneMatch(this::fileOnDisk);
    }

    // --- Hazırlık ---

    private org.springframework.http.HttpHeaders bearer() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(instructor);
        return headers;
    }

    private long lesson(String title, String type) {
        return create("/api/sections/" + sectionId + "/lessons",
                Map.of("title", title, "contentType", type, "mandatory", true, "durationSeconds", 60));
    }

    private long create(String path, Map<String, Object> body) {
        ResponseEntity<JsonNode> created = post(path, instructor, body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody().get("id").asLong();
    }

    private void setStatus(String status) {
        jdbc.update("UPDATE courses SET status = ? WHERE id = ?", status, courseId);
    }
}
