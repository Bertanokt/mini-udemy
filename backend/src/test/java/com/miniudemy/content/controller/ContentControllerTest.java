package com.miniudemy.content.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private String student() {
        return "Bearer " + jwtService.generateToken(42L, "STUDENT");
    }
}
