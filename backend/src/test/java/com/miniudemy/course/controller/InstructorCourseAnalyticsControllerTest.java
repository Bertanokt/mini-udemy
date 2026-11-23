package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.dto.CourseStatsResponse;
import com.miniudemy.course.dto.CourseStatsResponse.ProgressBucket;
import com.miniudemy.course.dto.CourseStatsResponse.QuizStats;
import com.miniudemy.course.dto.CourseStatsResponse.RatingStats;
import com.miniudemy.course.dto.CourseStudentPage;
import com.miniudemy.course.dto.CourseStudentResponse;
import com.miniudemy.course.dto.CourseStudentResponse.QuizResult;
import com.miniudemy.course.service.CourseAnalyticsService;

/** Eğitmen istatistik uçları gerçek güvenlik ayarıyla (rol veritabanından); servis taklit. */
@WebMvcTest(controllers = InstructorCourseAnalyticsController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class InstructorCourseAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CourseAnalyticsService analytics;

    @BeforeEach
    void users() {
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(1L, "Ali Yönetici", Role.ADMIN);
    }

    @Test
    void stats_asOwnerInstructor_returns200WithAllFields() throws Exception {
        given(analytics.getStats(5L, 7L)).willReturn(new CourseStatsResponse(5L, 3, 50.0, 33.33,
                List.of(new ProgressBucket(0, 24, 1), new ProgressBucket(25, 49, 0), new ProgressBucket(50, 74, 1),
                        new ProgressBucket(75, 99, 0), new ProgressBucket(100, 100, 1)),
                List.of(new QuizStats(1L, "Giriş sınavı", 2, 50.0)),
                new RatingStats(4.5, 2, Map.of("4", 1L, "5", 1L))));

        mockMvc.perform(get("/api/instructor/courses/5/stats").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(5))
                .andExpect(jsonPath("$.enrollmentCount").value(3))
                .andExpect(jsonPath("$.averageProgressPercent").value(50.0))
                .andExpect(jsonPath("$.completionRate").value(33.33))
                .andExpect(jsonPath("$.progressDistribution.length()").value(5))
                .andExpect(jsonPath("$.progressDistribution[0].minPercent").value(0))
                .andExpect(jsonPath("$.progressDistribution[0].maxPercent").value(24))
                .andExpect(jsonPath("$.progressDistribution[0].count").value(1))
                .andExpect(jsonPath("$.progressDistribution[4].minPercent").value(100))
                .andExpect(jsonPath("$.progressDistribution[4].maxPercent").value(100))
                .andExpect(jsonPath("$.progressDistribution[4].count").value(1))
                .andExpect(jsonPath("$.quizzes[0].quizId").value(1))
                .andExpect(jsonPath("$.quizzes[0].title").value("Giriş sınavı"))
                .andExpect(jsonPath("$.quizzes[0].attemptCount").value(2))
                .andExpect(jsonPath("$.quizzes[0].passRate").value(50.0))
                .andExpect(jsonPath("$.rating.averageRating").value(4.5))
                .andExpect(jsonPath("$.rating.reviewCount").value(2))
                .andExpect(jsonPath("$.rating.distribution.5").value(1));
    }

    @Test
    void stats_whenServiceSaysNotOwner_returns403WithStandardBody() throws Exception {
        given(analytics.getStats(5L, 7L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        mockMvc.perform(get("/api/instructor/courses/5/stats").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Bu kurs size ait değil."));
    }

    @Test
    void stats_whenCourseMissing_returns404() throws Exception {
        given(analytics.getStats(99L, 7L)).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(get("/api/instructor/courses/99/stats").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void stats_asStudentOrAdmin_returns403_andServiceIsNotCalled() throws Exception {
        mockMvc.perform(get("/api/instructor/courses/5/stats").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructor/courses/5/stats").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verify(analytics, never()).getStats(anyLong(), anyLong());
    }

    @Test
    void stats_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/instructor/courses/5/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void students_asOwnerInstructor_returns200WithPageAndNoEmail() throws Exception {
        given(analytics.listStudents(5L, 7L, 0, 20)).willReturn(new CourseStudentPage(List.of(
                new CourseStudentResponse(new UserSummary(100L, "Ayşe Yılmaz"), Instant.parse("2026-11-17T10:00:00Z"),
                        75.0, Instant.parse("2026-11-18T09:00:00Z"), List.of(new QuizResult(1L, 80.5, true)))),
                0, 20, 1, 1));

        mockMvc.perform(get("/api/instructor/courses/5/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].student.fullName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.content[0].student.email").doesNotExist())
                .andExpect(jsonPath("$.content[0].enrolledAt").value("2026-11-17T10:00:00Z"))
                .andExpect(jsonPath("$.content[0].progressPercent").value(75.0))
                .andExpect(jsonPath("$.content[0].lastAccessedAt").value("2026-11-18T09:00:00Z"))
                .andExpect(jsonPath("$.content[0].quizResults[0].bestPercent").value(80.5))
                .andExpect(jsonPath("$.content[0].quizResults[0].passed").value(true));
    }

    @Test
    void students_passesPageAndSizeToService() throws Exception {
        given(analytics.listStudents(5L, 7L, 2, 50)).willReturn(new CourseStudentPage(List.of(), 2, 50, 0, 0));

        mockMvc.perform(get("/api/instructor/courses/5/students").param("page", "2").param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void students_withInvalidPageOrSize_returns400() throws Exception {
        for (String[] bad : new String[][] {{"page", "-1"}, {"size", "0"}, {"size", "101"}, {"size", "abc"}}) {
            mockMvc.perform(get("/api/instructor/courses/5/students").param(bad[0], bad[1])
                            .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                    .andExpect(status().isBadRequest());
        }
        verify(analytics, never()).listStudents(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void students_whenServiceSaysNotOwner_returns403() throws Exception {
        given(analytics.listStudents(5L, 7L, 0, 20)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        mockMvc.perform(get("/api/instructor/courses/5/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void students_whenCourseMissing_returns404() throws Exception {
        given(analytics.listStudents(99L, 7L, 0, 20)).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(get("/api/instructor/courses/99/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void students_asStudent_returns403_andWithoutToken401() throws Exception {
        mockMvc.perform(get("/api/instructor/courses/5/students").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/instructor/courses/5/students")).andExpect(status().isUnauthorized());
        verify(analytics, never()).listStudents(anyLong(), anyLong(), anyInt(), anyInt());
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
