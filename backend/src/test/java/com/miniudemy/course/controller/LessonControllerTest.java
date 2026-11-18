package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.dto.LessonRequest;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.service.LessonService;

/**
 * Ders uçları gerçek güvenlik ayarıyla; servis taklit. Rol kuralı, doğrulama, durum kodları ve eğitmen id'sinin
 * token'dan alınması burada; iş kuralı LessonServiceTest'te.
 */
@WebMvcTest(controllers = LessonController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class LessonControllerTest {

    private static final String VALID_BODY = """
            {"title":"Değişkenler","contentType":"VIDEO","mandatory":true,"durationSeconds":540}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LessonService lessonService;

    @BeforeEach
    void users() {
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(1L, "Ali Yönetici", Role.ADMIN);
    }

    private static LessonResponse lesson(long id, int position) {
        return new LessonResponse(id, 20L, "Ders " + id, LessonContentType.VIDEO, true, 540, position, false);
    }

    // POST /api/sections/{id}/lessons

    @Test
    void create_asInstructor_returns201_andInstructorComesFromToken() throws Exception {
        given(lessonService.create(eq(7L), eq(20L), any(LessonRequest.class))).willReturn(lesson(31L, 2));

        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.sectionId").value(20))
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.contentType").value("VIDEO"))
                .andExpect(jsonPath("$.hasContent").value(false));
        verify(lessonService).create(eq(7L), eq(20L), eq(new LessonRequest("Değişkenler", LessonContentType.VIDEO,
                true, 540)));
    }

    @Test
    void create_asStudentOrAdmin_returns403_andCallsNoService() throws Exception {
        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lessonService);
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withMissingOrInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"durationSeconds\":-1}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Başlık zorunludur."))
                .andExpect(jsonPath("$.errors.contentType").value("İçerik türü zorunludur."))
                .andExpect(jsonPath("$.errors.mandatory").value("Zorunlu ders bilgisi zorunludur."))
                .andExpect(jsonPath("$.errors.durationSeconds").value("Süre negatif olamaz."));
        verify(lessonService, never()).create(any(), any(), any());
    }

    @Test
    void create_withUnknownContentType_returns400() throws Exception {
        mockMvc.perform(post("/api/sections/20/lessons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"contentType\":\"AUDIO\",\"mandatory\":true,\"durationSeconds\":1}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenCourseIsLockedOrNotOwnedOrMissing_mapsServiceErrorsTo409And403And404() throws Exception {
        given(lessonService.create(eq(7L), eq(21L), any())).willThrow(new ConflictException("Kurs kilitli."));
        given(lessonService.create(eq(7L), eq(22L), any())).willThrow(new ForbiddenException("Bu kurs size ait değil."));
        given(lessonService.create(eq(7L), eq(23L), any())).willThrow(new NotFoundException("Bölüm bulunamadı."));

        for (int[] expected : new int[][] {{21, 409}, {22, 403}, {23, 404}}) {
            mockMvc.perform(post("/api/sections/" + expected[0] + "/lessons").contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY).header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                    .andExpect(status().is(expected[1]))
                    .andExpect(jsonPath("$.status").value(expected[1]));
        }
    }

    // PUT /api/lessons/{id}

    @Test
    void update_asInstructor_returns200() throws Exception {
        given(lessonService.update(eq(7L), eq(31L), any(LessonRequest.class))).willReturn(lesson(31L, 0));

        mockMvc.perform(put("/api/lessons/31").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31));
    }

    @Test
    void update_asStudent_returns403_andInvalidBody_returns400() throws Exception {
        mockMvc.perform(put("/api/lessons/31").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/lessons/31").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(lessonService);
    }

    @Test
    void update_whenLessonMissing_returns404() throws Exception {
        given(lessonService.update(eq(7L), eq(99L), any())).willThrow(new NotFoundException("Ders bulunamadı."));

        mockMvc.perform(put("/api/lessons/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ders bulunamadı."));
    }

    // DELETE /api/lessons/{id}

    @Test
    void delete_asInstructor_returns204() throws Exception {
        mockMvc.perform(delete("/api/lessons/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNoContent());
        verify(lessonService).delete(7L, 31L);
    }

    @Test
    void delete_asStudent_returns403_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/lessons/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/lessons/31")).andExpect(status().isUnauthorized());
        verifyNoInteractions(lessonService);
    }

    @Test
    void delete_whenCourseIsLocked_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("Kurs kilitli.")).when(lessonService).delete(7L, 31L);

        mockMvc.perform(delete("/api/lessons/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict());
    }

    // PUT /api/lessons/{id}/position

    @Test
    void move_asInstructor_returnsLessonsInNewOrder() throws Exception {
        given(lessonService.move(7L, 31L, 1)).willReturn(List.of(lesson(32L, 0), lesson(31L, 1)));

        mockMvc.perform(put("/api/lessons/31/position").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":1}").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(32))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void move_withMissingOrNegativePosition_returns400_andAsStudent403() throws Exception {
        mockMvc.perform(put("/api/lessons/31/position").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.position").value("Sıra zorunludur."));
        mockMvc.perform(put("/api/lessons/31/position").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":-1}").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.position").value("Sıra 0 ya da daha büyük olmalıdır."));
        mockMvc.perform(put("/api/lessons/31/position").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":1}").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(lessonService);
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
