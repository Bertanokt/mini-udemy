package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.miniudemy.course.service.CourseLifecycleService;

/** {@code POST /api/courses/{id}/submit} gerçek güvenlik ayarıyla; iş kuralı CourseLifecycleServiceTest'te. */
@WebMvcTest(controllers = CourseLifecycleController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class CourseLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CourseLifecycleService lifecycle;

    @BeforeEach
    void users() {
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(1L, "Ali Yönetici", Role.ADMIN);
    }

    @Test
    void submit_asInstructor_returns200WithInReviewCourse_forTokenOwner() throws Exception {
        given(lifecycle.submit(5L, 7L)).willReturn(new CourseResponse(5L, "Sıfırdan Java", "Java'ya giriş kursu.",
                new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER, CourseStatus.IN_REVIEW,
                new UserSummary(7L, "Mehmet Kaya"), null, List.of(), null, null,
                Instant.parse("2026-11-18T10:00:00Z"), Instant.parse("2026-11-18T11:00:00Z")));

        mockMvc.perform(post("/api/courses/5/submit").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    void submit_asStudentOrAdmin_returns403_andCallsNothing() throws Exception {
        mockMvc.perform(post("/api/courses/5/submit").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/courses/5/submit").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verify(lifecycle, never()).submit(any(), any());
    }

    @Test
    void submit_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/courses/5/submit")).andExpect(status().isUnauthorized());
    }

    @Test
    void submit_inLockedStatus_returns409WithStandardBody() throws Exception {
        given(lifecycle.submit(5L, 7L)).willThrow(new CourseStateException("Kurs bu durumdayken onaya gönderilemez; "
                + "yalnız taslak ve yayından kaldırılmış kurslar onaya gönderilebilir."));

        mockMvc.perform(post("/api/courses/5/submit").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Kurs bu durumdayken onaya gönderilemez; "
                        + "yalnız taslak ve yayından kaldırılmış kurslar onaya gönderilebilir."));
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
