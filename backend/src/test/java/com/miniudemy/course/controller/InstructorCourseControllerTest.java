package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.service.CourseService;

/** {@code GET /api/instructor/courses} gerçek güvenlik ayarıyla (rol veritabanından); servis taklit. */
@WebMvcTest(controllers = InstructorCourseController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class InstructorCourseControllerTest {

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
        given(courseService.listByInstructor(7L, null)).willReturn(List.of(
                course(2L, CourseStatus.DRAFT, "Örnekler yetersiz.", null),
                course(1L, CourseStatus.UNPUBLISHED, null, "Güncel değil.")));
        given(courseService.listByInstructor(7L, CourseStatus.DRAFT))
                .willReturn(List.of(course(2L, CourseStatus.DRAFT, null, null)));
    }

    @Test
    void listMine_asInstructor_returns200WithAllCourses_andReasons() throws Exception {
        mockMvc.perform(get("/api/instructor/courses").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Örnekler yetersiz."))
                .andExpect(jsonPath("$[1].status").value("UNPUBLISHED"))
                .andExpect(jsonPath("$[1].unpublishReason").value("Güncel değil."))
                .andExpect(jsonPath("$[0].instructor.email").doesNotExist());
    }

    @Test
    void listMine_withStatusFilter_passesStatusToService() throws Exception {
        mockMvc.perform(get("/api/instructor/courses").param("status", "DRAFT")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        verify(courseService).listByInstructor(7L, CourseStatus.DRAFT);
    }

    @Test
    void listMine_withInvalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/instructor/courses").param("status", "YOK")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMine_asStudentOrAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/instructor/courses").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/instructor/courses").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verify(courseService, never()).listByInstructor(anyLong(), any());
    }

    @Test
    void listMine_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/instructor/courses")).andExpect(status().isUnauthorized());
    }

    private static CourseResponse course(long id, CourseStatus status, String rejection, String unpublish) {
        return new CourseResponse(id, "Kurs " + id, "Kurs açıklaması.", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, status, new UserSummary(7L, "Mehmet Kaya"), null, List.of(), rejection,
                unpublish, Instant.parse("2026-11-17T10:00:00Z"), Instant.parse("2026-11-17T10:00:00Z"));
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
