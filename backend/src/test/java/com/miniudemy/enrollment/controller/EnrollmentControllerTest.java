package com.miniudemy.enrollment.controller;

import static org.mockito.BDDMockito.given;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.enrollment.dto.EnrollmentResponse;
import com.miniudemy.enrollment.service.EnrollmentService;

/** Kayıt uçları gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = EnrollmentController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private EnrollmentService enrollmentService;

    private final EnrollmentResponse response = new EnrollmentResponse(9L,
            new CourseSummaryResponse(5L, "Java", new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER,
                    CourseStatus.PUBLISHED, new UserSummary(7L, "Mehmet Demir"), null, 12, 3600, 1L, null, 0),
            Instant.parse("2026-11-10T10:00:00Z"), 0.0, null, null);

    @BeforeEach
    void users() {
        addUser(7L, Role.INSTRUCTOR);
        addUser(42L, Role.STUDENT);
    }

    private void addUser(long id, Role role) {
        User user = new User("u" + id + "@example.com", "$2a$10$ozet", "Kullanıcı " + id, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }

    @Test
    void enrollCourse_asStudent_returns201WithEnrollment() throws Exception {
        given(enrollmentService.enroll(5L, 42L)).willReturn(response);

        mockMvc.perform(post("/api/courses/5/enroll").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.course.title").value("Java"))
                .andExpect(jsonPath("$.progressPercent").value(0.0))
                .andExpect(jsonPath("$.lastLessonId").doesNotExist());
    }

    @Test
    void enrollCourse_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/courses/5/enroll")).andExpect(status().isUnauthorized());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollCourse_asInstructor_returns403() throws Exception {
        mockMvc.perform(post("/api/courses/5/enroll").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollCourse_unpublishedCourse_returns404FromService() throws Exception {
        given(enrollmentService.enroll(5L, 42L)).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(post("/api/courses/5/enroll").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kurs bulunamadı."));
    }

    @Test
    void enrollCourse_twice_returns409FromService() throws Exception {
        given(enrollmentService.enroll(5L, 42L)).willThrow(new ConflictException("Bu kursa zaten kayıtlısınız."));

        mockMvc.perform(post("/api/courses/5/enroll").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bu kursa zaten kayıtlısınız."));
    }

    @Test
    void listMyEnrollments_asStudent_returnsTitleAndProgress() throws Exception {
        given(enrollmentService.listMyEnrollments(42L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/me/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].course.title").value("Java"))
                .andExpect(jsonPath("$[0].progressPercent").value(0.0));
    }

    @Test
    void listMyEnrollments_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me/enrollments")).andExpect(status().isUnauthorized());
    }

    @Test
    void listMyEnrollments_asInstructor_returns403() throws Exception {
        mockMvc.perform(get("/api/me/enrollments").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enrollmentService);
    }
}
