package com.miniudemy.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.miniudemy.admin.service.AdminCourseService;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStateException;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.service.CourseCatalogService;
import com.miniudemy.course.service.CourseLifecycleService;

/**
 * Yönetici kurs uçları (#59) gerçek güvenlik ayarıyla ve gerçek AdminCourseService ile; course servisleri taklit.
 * Rol kuralı ({@code /api/admin/**} yalnız ADMIN), gerekçe doğrulaması ve parametrelerin servise geçişi burada.
 */
@WebMvcTest(controllers = AdminCourseController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class, AdminCourseService.class})
@ActiveProfiles("test")
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CourseCatalogService catalog;

    @MockitoBean
    private CourseLifecycleService lifecycle;

    @BeforeEach
    void users() {
        user(1L, "Ali Yönetici", Role.ADMIN);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
    }

    @Test
    void list_asAdmin_returnsCoursesOfTheRequestedStatus() throws Exception {
        given(catalog.listByStatus(CourseStatus.IN_REVIEW, 2, 10))
                .willReturn(new CourseSummaryPage(List.of(), 2, 10, 21, 3));

        mockMvc.perform(get("/api/admin/courses?status=IN_REVIEW&page=2&size=10").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(21));
    }

    @Test
    void list_withoutStatus_usesDefaultsAndAllStatuses() throws Exception {
        given(catalog.listByStatus(null, 0, 20)).willReturn(new CourseSummaryPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/courses").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk());
        verify(catalog).listByStatus(null, 0, 20);
    }

    @Test
    void list_withInvalidParameters_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/courses?status=WAITING").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/courses?size=101").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest());
        verify(catalog, never()).listByStatus(any(), anyInt(), anyInt());
    }

    @Test
    void approve_asAdmin_returns200WithPublishedCourse() throws Exception {
        given(lifecycle.approve(5L)).willReturn(course(CourseStatus.PUBLISHED, null));

        mockMvc.perform(post("/api/admin/courses/5/approve").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void reject_asAdmin_withReason_returns200_andPassesTheReason() throws Exception {
        given(lifecycle.reject(5L, "Ders içerikleri eksik.")).willReturn(course(CourseStatus.DRAFT, "Ders içerikleri eksik."));

        mockMvc.perform(reject("{\"reason\":\"Ders içerikleri eksik.\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.rejectionReason").value("Ders içerikleri eksik."));
    }

    @Test
    void reject_withMissingBlankOrShortReason_returns400WithFieldError_andRejectsNothing() throws Exception {
        for (String body : List.of("{}", "{\"reason\":\"   \"}")) {
            mockMvc.perform(reject(body).header(HttpHeaders.AUTHORIZATION, admin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        mockMvc.perform(reject("{\"reason\":\"Kısa\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").value("Gerekçe 5–1000 karakter olmalıdır."));
        verify(lifecycle, never()).reject(any(), any());
    }

    @Test
    void approveOrReject_notInReview_returns409_andMissingCourse_returns404() throws Exception {
        given(lifecycle.approve(5L)).willThrow(new CourseStateException(
                "Kurs bu durumdayken onaylanamaz; yalnız incelemedeki kurslar onaylanabilir."));
        given(lifecycle.reject(6L, "Ders içerikleri eksik.")).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(post("/api/admin/courses/5/approve").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Kurs bu durumdayken onaylanamaz; yalnız incelemedeki kurslar onaylanabilir."));
        mockMvc.perform(post("/api/admin/courses/6/reject").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Ders içerikleri eksik.\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }

    // --- yayından kaldırma (#123) ---

    @Test
    void unpublish_asAdmin_withReason_returns200_andPassesTheReason() throws Exception {
        given(lifecycle.unpublish(5L, "Telif şikâyeti alındı.")).willReturn(new CourseResponse(5L, "Sıfırdan Java",
                "Java'ya giriş kursu.", new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER,
                CourseStatus.UNPUBLISHED, new UserSummary(7L, "Mehmet Kaya"), null, List.of(), null,
                "Telif şikâyeti alındı.", Instant.parse("2026-11-18T10:00:00Z"), Instant.parse("2026-11-18T11:00:00Z")));

        mockMvc.perform(unpublish("{\"reason\":\"Telif şikâyeti alındı.\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNPUBLISHED"))
                .andExpect(jsonPath("$.unpublishReason").value("Telif şikâyeti alındı."));
    }

    @Test
    void unpublish_withMissingBlankOrShortReason_returns400WithFieldError_andUnpublishesNothing() throws Exception {
        for (String body : List.of("{}", "{\"reason\":\"   \"}", "{\"reason\":\"Kısa\"}")) {
            mockMvc.perform(unpublish(body).header(HttpHeaders.AUTHORIZATION, admin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        verify(lifecycle, never()).unpublish(any(), any());
    }

    @Test
    void unpublish_notPublished_returns409_andMissingCourse_returns404() throws Exception {
        given(lifecycle.unpublish(5L, "Telif şikâyeti alındı.")).willThrow(new CourseStateException(
                "Kurs bu durumdayken yayından kaldırılamaz; yalnız yayımlanmış kurslar yayından kaldırılabilir."));
        given(lifecycle.unpublish(6L, "Telif şikâyeti alındı.")).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(unpublish("{\"reason\":\"Telif şikâyeti alındı.\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/courses/6/unpublish").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Telif şikâyeti alındı.\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unpublish_asInstructorOrStudent_returns403() throws Exception {
        for (String token : List.of(bearer(7L, "INSTRUCTOR"), bearer(42L, "STUDENT"))) {
            mockMvc.perform(unpublish("{\"reason\":\"Telif şikâyeti alındı.\"}").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(lifecycle);
    }

    private static MockHttpServletRequestBuilder unpublish(String body) {
        return post("/api/admin/courses/5/unpublish").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void everyEndpoint_asInstructorOrStudent_returns403_andWithoutToken_returns401() throws Exception {
        for (String token : List.of(bearer(7L, "INSTRUCTOR"), bearer(42L, "STUDENT"))) {
            mockMvc.perform(get("/api/admin/courses").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/admin/courses/5/approve").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(reject("{\"reason\":\"Ders içerikleri eksik.\"}").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post("/api/admin/courses/5/approve")).andExpect(status().isUnauthorized());
        verifyNoInteractions(catalog, lifecycle);
    }

    private static MockHttpServletRequestBuilder reject(String body) {
        return post("/api/admin/courses/5/reject").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static CourseResponse course(CourseStatus status, String rejectionReason) {
        return new CourseResponse(5L, "Sıfırdan Java", "Java'ya giriş kursu.", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, status, new UserSummary(7L, "Mehmet Kaya"), null, List.of(), rejectionReason,
                null, Instant.parse("2026-11-18T10:00:00Z"), Instant.parse("2026-11-18T11:00:00Z"));
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String admin() {
        return bearer(1L, "ADMIN");
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
