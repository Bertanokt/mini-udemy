package com.miniudemy.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.miniudemy.admin.dto.PlatformReportResponse;
import com.miniudemy.admin.dto.PlatformReportResponse.CategoryCount;
import com.miniudemy.admin.dto.PlatformReportResponse.TopCourse;
import com.miniudemy.admin.service.PlatformReportService;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;

/** {@code GET /api/admin/reports} (#145) gerçek güvenlik ayarıyla: yalnız ADMIN, sözleşmedeki alan adları. */
@WebMvcTest(controllers = AdminReportController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PlatformReportService reports;

    @BeforeEach
    void users() {
        user(1L, "Ali Yönetici", Role.ADMIN);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
    }

    @Test
    void report_asAdmin_returns200WithTheContractFields() throws Exception {
        given(reports.report()).willReturn(new PlatformReportResponse(null, null,
                Map.of("STUDENT", 6L, "INSTRUCTOR", 2L, "ADMIN", 1L), Map.of("PUBLISHED", 2L), 7, 1, 1,
                List.of(new TopCourse(5L, "Sıfırdan Python", 5)), List.of(new CategoryCount(1L, "Yazılım", 4))));

        mockMvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isEmpty())
                .andExpect(jsonPath("$.usersByRole.STUDENT").value(6))
                .andExpect(jsonPath("$.coursesByStatus.PUBLISHED").value(2))
                .andExpect(jsonPath("$.enrollmentCount").value(7))
                .andExpect(jsonPath("$.completionCount").value(1))
                .andExpect(jsonPath("$.certificateCount").value(1))
                .andExpect(jsonPath("$.topCourses[0].courseId").value(5))
                .andExpect(jsonPath("$.topCourses[0].title").value("Sıfırdan Python"))
                .andExpect(jsonPath("$.topCourses[0].enrollmentCount").value(5))
                .andExpect(jsonPath("$.categoryDistribution[0].name").value("Yazılım"))
                .andExpect(jsonPath("$.categoryDistribution[0].courseCount").value(4));
    }

    @Test
    void report_asInstructorOrStudent_returns403_andWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/reports")).andExpect(status().isUnauthorized());
        verify(reports, never()).report();
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
