package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStateException;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.dto.UpdateCourseRequest;
import com.miniudemy.course.service.CourseService;

/**
 * {@code POST /api/courses} gerçek güvenlik ayarıyla (rol veritabanından, AccountLookupService); servis taklit.
 * Rol kuralı, doğrulama ve eğitmen id'sinin token'dan alınması burada; iş kuralı CourseServiceTest'te.
 */
@WebMvcTest(controllers = CourseController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class CourseControllerTest {

    private static final String VALID_BODY = """
            {"title":"Sıfırdan Java","description":"Java'ya temelden giriş kursu.","categoryId":1,
             "level":"BEGINNER","learningOutcomes":["Koleksiyonlar"]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CourseService courseService;

    @BeforeEach
    void users() {
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(1L, "Ali Yönetici", Role.ADMIN);
        given(courseService.create(eq(7L), any(CreateCourseRequest.class))).willReturn(new CourseResponse(15L,
                "Sıfırdan Java", "Java'ya temelden giriş kursu.", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, CourseStatus.DRAFT, new UserSummary(7L, "Mehmet Kaya"), null,
                List.of("Koleksiyonlar"), null, null, Instant.parse("2026-11-17T10:00:00Z"),
                Instant.parse("2026-11-17T10:00:00Z")));
    }

    @Test
    void create_asInstructor_returns201WithDraftCourse_andInstructorFromToken() throws Exception {
        mockMvc.perform(create(VALID_BODY).header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category.name").value("Yazılım"))
                .andExpect(jsonPath("$.instructor.id").value(7))
                .andExpect(jsonPath("$.instructor.fullName").value("Mehmet Kaya"))
                .andExpect(jsonPath("$.instructor.email").doesNotExist());
        verify(courseService).create(eq(7L), any(CreateCourseRequest.class));
    }

    @Test
    void create_asStudentOrAdmin_returns403_andCreatesNothing() throws Exception {
        mockMvc.perform(create(VALID_BODY).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(create(VALID_BODY).header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verify(courseService, never()).create(any(), any());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(create(VALID_BODY)).andExpect(status().isUnauthorized());
    }

    @Test
    void create_withMissingOrInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(create("{\"title\":\"Ja\",\"description\":\"kısa\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Başlık 3–200 karakter olmalıdır."))
                .andExpect(jsonPath("$.errors.description").value("Açıklama 10–5000 karakter olmalıdır."))
                .andExpect(jsonPath("$.errors.categoryId").value("Kategori zorunludur."))
                .andExpect(jsonPath("$.errors.level").value("Seviye zorunludur."));
        verify(courseService, never()).create(any(), any());
    }

    // --- düzenleme ve silme (#23) ---

    @Test
    void update_asInstructor_returns200_andPassesCourseAndInstructorFromToken() throws Exception {
        given(courseService.update(eq(15L), eq(7L), any(UpdateCourseRequest.class))).willReturn(new CourseResponse(15L,
                "Sıfırdan Java", "Java'ya temelden giriş kursu.", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, CourseStatus.DRAFT, new UserSummary(7L, "Mehmet Kaya"), null,
                List.of("Koleksiyonlar"), null, null, Instant.parse("2026-11-17T10:00:00Z"),
                Instant.parse("2026-11-19T10:00:00Z")));

        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.title").value("Sıfırdan Java"));
        verify(courseService).update(eq(15L), eq(7L), any(UpdateCourseRequest.class));
    }

    @Test
    void update_asStudentOrAdmin_returns403_withoutToken401_andUpdatesNothing() throws Exception {
        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
        verify(courseService, never()).update(any(), any(), any());
    }

    @Test
    void update_withInvalidFields_returns400WithFieldErrors_andUpdatesNothing() throws Exception {
        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ja\",\"description\":\"kısa\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Başlık 3–200 karakter olmalıdır."))
                .andExpect(jsonPath("$.errors.categoryId").value("Kategori zorunludur."));
        verify(courseService, never()).update(any(), any(), any());
    }

    @Test
    void update_inLockedState_returns409WithMessage() throws Exception {
        given(courseService.update(eq(15L), eq(7L), any(UpdateCourseRequest.class))).willThrow(
                new CourseStateException("Kurs bu durumdayken düzenlenemez; yalnız taslak ve yayından kaldırılmış kurslar düzenlenebilir."));

        mockMvc.perform(put("/api/courses/15").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void delete_asInstructor_returns204_andPassesCourseAndInstructorFromToken() throws Exception {
        mockMvc.perform(delete("/api/courses/15").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNoContent());
        verify(courseService).delete(15L, 7L);
    }

    @Test
    void delete_asStudentOrAdmin_returns403_withoutToken401_andDeletesNothing() throws Exception {
        mockMvc.perform(delete("/api/courses/15").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/courses/15").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/courses/15")).andExpect(status().isUnauthorized());
        verify(courseService, never()).delete(any(), any());
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder create(String body) {
        return post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
