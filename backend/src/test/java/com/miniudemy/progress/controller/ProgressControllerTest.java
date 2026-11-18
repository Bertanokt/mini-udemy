package com.miniudemy.progress.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.CourseProgressResponse.SectionProgress;
import com.miniudemy.progress.service.ProgressService;

/** İlerleme ucu gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = ProgressController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProgressService progressService;

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
    void getMyProgress_asEnrolledStudent_returnsPercentSectionsAndCompletedIds() throws Exception {
        given(progressService.getMyProgress(42L, 7L)).willReturn(new CourseProgressResponse(7L, 75.0,
                List.of(11L, 12L), List.of(new SectionProgress(3L, 100.0), new SectionProgress(4L, 50.0))));

        mockMvc.perform(get("/api/me/courses/7/progress").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(7))
                .andExpect(jsonPath("$.progressPercent").value(75.0))
                .andExpect(jsonPath("$.completedLessonIds[1]").value(12))
                .andExpect(jsonPath("$.sections[1].sectionId").value(4))
                .andExpect(jsonPath("$.sections[1].progressPercent").value(50.0));
    }

    @Test
    void getMyProgress_notEnrolled_returns403FromService() throws Exception {
        given(progressService.getMyProgress(42L, 7L)).willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        mockMvc.perform(get("/api/me/courses/7/progress").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kursa kayıtlı değilsiniz."));
    }

    @Test
    void getMyProgress_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/me/courses/7/progress")).andExpect(status().isUnauthorized());
        verifyNoInteractions(progressService);
    }

    @Test
    void getMyProgress_asInstructor_returns403() throws Exception {
        mockMvc.perform(get("/api/me/courses/7/progress")
                .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(progressService);
    }
}
