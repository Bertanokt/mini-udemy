package com.miniudemy.quiz.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.service.QuizAttemptService;

/** GET /api/me/quizzes/{id}/attempts (#55) gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = QuizAttemptController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class QuizAttemptControllerTest {

    private static final String URL = "/api/me/quizzes/31/attempts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QuizAttemptService attemptService;

    @BeforeEach
    void users() {
        addUser(7L, Role.INSTRUCTOR);
        addUser(42L, Role.STUDENT);
        addUser(1L, Role.ADMIN);
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
    void listMyAttempts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void listMyAttempts_asInstructor_returns403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMyAttempts_asAdmin_returns403() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMyAttempts_asStudent_returnsContractShape() throws Exception {
        given(attemptService.listMyAttempts(31L, 42L)).willReturn(List.of(new AttemptSummaryResponse(9L, 31L,
                new BigDecimal("35.00"), new BigDecimal("50.00"), new BigDecimal("70.0"), true,
                Instant.parse("2026-11-17T10:00:00Z"))));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].quizId").value(31))
                .andExpect(jsonPath("$[0].score").value(35.0))
                .andExpect(jsonPath("$[0].maxScore").value(50.0))
                .andExpect(jsonPath("$[0].percent").value(70.0))
                .andExpect(jsonPath("$[0].passed").value(true))
                .andExpect(jsonPath("$[0].submittedAt").value("2026-11-17T10:00:00Z"));
    }

    @Test
    void listMyAttempts_notEnrolled_returns403FromService() throws Exception {
        given(attemptService.listMyAttempts(31L, 42L)).willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kursa kayıtlı değilsiniz."));
    }

    @Test
    void listMyAttempts_unknownQuiz_returns404() throws Exception {
        given(attemptService.listMyAttempts(31L, 42L)).willThrow(new NotFoundException("Sınav bulunamadı."));

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isNotFound());
    }
}
