package com.miniudemy.content.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.dto.ContentDownload;
import com.miniudemy.content.service.ContentDeliveryService;
import com.miniudemy.content.service.ContentUploadService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.PayloadTooLargeException;
import com.miniudemy.common.exception.UnsupportedMediaTypeException;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.dto.LessonResponse;

/**
 * İçerik ve kapak uçları (#113) gerçek güvenlik ayarıyla; servis taklit. Başlıklar (Content-Type,
 * Content-Disposition), Range (video 206, diğerleri 200) ve kimlik kuralları burada; erişim kuralı servis testinde.
 */
@WebMvcTest(controllers = ContentController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class ContentControllerTest {

    private static final byte[] BYTES = "0123456789".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ContentDeliveryService delivery;

    @MockitoBean
    private ContentUploadService upload;

    @BeforeEach
    void users() {
        User student = new User("ayse@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(student, "id", 42L);
        given(userRepository.findById(42L)).willReturn(Optional.of(student));
        User admin = new User("ali@example.com", "$2a$10$ozet", "Ali Yönetici", Role.ADMIN);
        ReflectionTestUtils.setField(admin, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
    }

    private void lesson(long id, String mime, String name, DeliveryMode mode, boolean range) {
        given(delivery.openLessonContent(id, 42L, false)).willReturn(
                new ContentDownload(new ByteArrayResource(BYTES), mime, name, BYTES.length, mode, range));
    }

    @Test
    void video_wholeFile_isInlineWithAcceptRanges() throws Exception {
        lesson(11L, "video/mp4", "Kurulum.mp4", DeliveryMode.INLINE, true);

        mockMvc.perform(get("/api/lessons/11/content").header(HttpHeaders.AUTHORIZATION, student()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"=?UTF-8?Q?Kurulum.mp4?=\"; filename*=UTF-8''Kurulum.mp4"))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(content().bytes(BYTES));
    }

    @Test
    void video_rangeRequest_returns206WithTheRequestedBytes() throws Exception {
        lesson(11L, "video/mp4", "Kurulum.mp4", DeliveryMode.INLINE, true);

        mockMvc.perform(get("/api/lessons/11/content").header(HttpHeaders.AUTHORIZATION, student())
                        .header(HttpHeaders.RANGE, "bytes=2-5"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 2-5/10"))
                .andExpect(content().bytes("2345".getBytes()));
    }

    @Test
    void pdf_rangeRequestIsIgnored_wholeFileWith200() throws Exception {
        lesson(12L, "application/pdf", "Notlar.pdf", DeliveryMode.INLINE, false);

        mockMvc.perform(get("/api/lessons/12/content").header(HttpHeaders.AUTHORIZATION, student())
                        .header(HttpHeaders.RANGE, "bytes=2-5"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_RANGE))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "none"))
                .andExpect(content().bytes(BYTES));
    }

    @Test
    void document_isAttachment_withUtf8FileName() throws Exception {
        lesson(13L, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Ders notları.docx",
                DeliveryMode.ATTACHMENT, false);

        mockMvc.perform(get("/api/lessons/13/content").header(HttpHeaders.AUTHORIZATION, student()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"=?UTF-8?Q?Ders_notlar=C4=B1.docx?=\"; filename*=UTF-8''Ders%20notlar%C4%B1.docx"));
    }

    @Test
    void lessonContent_withoutToken_returns401_andForbiddenFromService_returns403() throws Exception {
        mockMvc.perform(get("/api/lessons/11/content")).andExpect(status().isUnauthorized());
        verify(delivery, never()).openLessonContent(anyLong(), anyLong(), anyBoolean());

        given(delivery.openLessonContent(11L, 42L, false))
                .willThrow(new ForbiddenException("Bu ders içeriğine erişim yetkiniz yok."));
        mockMvc.perform(get("/api/lessons/11/content").header(HttpHeaders.AUTHORIZATION, student()))
                .andExpect(status().isForbidden());
    }

    @Test
    void lessonContent_asAdmin_passesAdminFlagToService() throws Exception {
        given(delivery.openLessonContent(11L, 1L, true)).willReturn(new ContentDownload(new ByteArrayResource(BYTES),
                "video/mp4", "Kurulum.mp4", BYTES.length, DeliveryMode.INLINE, true));

        mockMvc.perform(get("/api/lessons/11/content").header(HttpHeaders.AUTHORIZATION, "Bearer "
                        + jwtService.generateToken(1L, "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void cover_isPublic_inlineImage() throws Exception {
        given(delivery.openCourseCover(3L)).willReturn(new ContentDownload(new ByteArrayResource(BYTES), "image/png",
                "kapak.png", BYTES.length, DeliveryMode.INLINE, false));

        mockMvc.perform(get("/api/courses/3/cover"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"=?UTF-8?Q?kapak.png?=\"; filename*=UTF-8''kapak.png"))
                .andExpect(content().bytes(BYTES));
    }

    // yükleme (#112)

    private static final MockMultipartFile FILE = new MockMultipartFile("file", "Kurulum.mp4", "video/mp4",
            new byte[] {1, 2, 3});

    private String instructor() {
        return "Bearer " + jwtService.generateToken(7L, "INSTRUCTOR");
    }

    private void instructorUser() {
        User instructor = new User("mehmet@example.com", "$2a$10$ozet", "Mehmet Kaya", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(instructor, "id", 7L);
        given(userRepository.findById(7L)).willReturn(Optional.of(instructor));
    }

    @Test
    void uploadLessonContent_asInstructor_passesTheFileNameAndBytes_andReturnsTheLesson() throws Exception {
        instructorUser();
        given(upload.uploadLessonContent(eq(7L), eq(31L), eq("Kurulum.mp4"), any()))
                .willReturn(new LessonResponse(31L, 20L, "Kurulum", LessonContentType.VIDEO, true, 60, 0, true));

        mockMvc.perform(multipart("/api/lessons/31/content").file(FILE).header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.hasContent").value(true));
    }

    @Test
    void uploadLessonContent_withoutToken_returns401_forStudent403_andNeverCallsTheService() throws Exception {
        mockMvc.perform(multipart("/api/lessons/31/content").file(FILE)).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/lessons/31/content").file(FILE).header(HttpHeaders.AUTHORIZATION, student()))
                .andExpect(status().isForbidden());

        verify(upload, never()).uploadLessonContent(anyLong(), anyLong(), any(), any());
    }

    @Test
    void uploadLessonContent_withoutTheFilePart_returns400() throws Exception {
        instructorUser();

        mockMvc.perform(multipart("/api/lessons/31/content").header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(upload, never()).uploadLessonContent(anyLong(), anyLong(), any(), any());
    }

    @Test
    void uploadLessonContent_serviceErrors_mapToTheStandardBody() throws Exception {
        instructorUser();
        given(upload.uploadLessonContent(eq(7L), eq(1L), any(), any()))
                .willThrow(new ForbiddenException("Bu kurs size ait değil."));
        given(upload.uploadLessonContent(eq(7L), eq(2L), any(), any()))
                .willThrow(new ConflictException("Kurs bu durumdayken düzenlenemez."));
        given(upload.uploadLessonContent(eq(7L), eq(3L), any(), any()))
                .willThrow(new PayloadTooLargeException("Video en fazla 200 MB olabilir."));
        given(upload.uploadLessonContent(eq(7L), eq(4L), any(), any()))
                .willThrow(new UnsupportedMediaTypeException("Yalnız mp4 ve webm videolar yüklenebilir."));

        for (int[] c : new int[][] {{1, 403}, {2, 409}, {3, 413}, {4, 415}}) {
            mockMvc.perform(multipart("/api/lessons/" + c[0] + "/content").file(FILE)
                            .header(HttpHeaders.AUTHORIZATION, instructor()))
                    .andExpect(status().is(c[1]))
                    .andExpect(jsonPath("$.status").value(c[1]));
        }
        mockMvc.perform(multipart("/api/lessons/4/content").file(FILE).header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(jsonPath("$.message").value("Yalnız mp4 ve webm videolar yüklenebilir."));
    }

    @Test
    void uploadCourseCover_asInstructor_callsTheService_studentGets403_anonymous401() throws Exception {
        instructorUser();
        MockMultipartFile png = new MockMultipartFile("file", "kapak.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/api/courses/5/cover").file(png).header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(status().isOk());
        verify(upload).uploadCourseCover(eq(7L), eq(5L), eq("kapak.png"), any());

        mockMvc.perform(multipart("/api/courses/5/cover").file(png).header(HttpHeaders.AUTHORIZATION, student()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/courses/5/cover").file(png)).andExpect(status().isUnauthorized());
    }

    @Test
    void uploadCourseCover_tooLarge_returns413_wrongType415() throws Exception {
        instructorUser();
        MockMultipartFile png = new MockMultipartFile("file", "kapak.png", "image/png", new byte[] {1});
        given(upload.uploadCourseCover(eq(7L), eq(5L), any(), any()))
                .willThrow(new PayloadTooLargeException("Kapak görseli en fazla 2 MB olabilir."));
        given(upload.uploadCourseCover(eq(7L), eq(6L), any(), any()))
                .willThrow(new UnsupportedMediaTypeException("Yalnız jpg, png ve webp görseller yüklenebilir."));

        mockMvc.perform(multipart("/api/courses/5/cover").file(png).header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Kapak görseli en fazla 2 MB olabilir."));
        mockMvc.perform(multipart("/api/courses/6/cover").file(png).header(HttpHeaders.AUTHORIZATION, instructor()))
                .andExpect(status().isUnsupportedMediaType());
    }

    private String student() {
        return "Bearer " + jwtService.generateToken(42L, "STUDENT");
    }
}
