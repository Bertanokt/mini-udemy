package com.miniudemy.quiz.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.quiz.dto.AttemptResultResponse;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.dto.QuestionResultResponse;
import com.miniudemy.quiz.dto.SubmitAttemptRequest;
import com.miniudemy.quiz.service.QuizAttemptService;

/** GET /api/me/quizzes/{id}/attempts (#55) ve POST /api/quizzes/{id}/attempts (#54) gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
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

    private static final String SUBMIT_URL = "/api/quizzes/31/attempts";
    private static final String SUBMIT_BODY = "{\"answers\":[{\"questionId\":21,\"optionIds\":[84]}]}";

    @Test
    void submit_withoutToken_returns401() throws Exception {
        mockMvc.perform(post(SUBMIT_URL).contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submit_asInstructor_returns403() throws Exception {
        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void submit_asStudent_returns201WithContractShape() throws Exception {
        given(attemptService.submit(org.mockito.ArgumentMatchers.eq(31L), org.mockito.ArgumentMatchers.eq(42L),
                any(SubmitAttemptRequest.class)))
                .willReturn(new AttemptResultResponse(9L, 31L, new BigDecimal("10.00"), new BigDecimal("20.00"),
                        new BigDecimal("50.0"), true, Instant.parse("2026-11-17T10:00:00Z"),
                        List.of(new QuestionResultResponse(21L, true, new BigDecimal("10.00"), new BigDecimal("10")),
                                new QuestionResultResponse(22L, false, BigDecimal.ZERO, new BigDecimal("10")))));

        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.quizId").value(31))
                .andExpect(jsonPath("$.score").value(10.0))
                .andExpect(jsonPath("$.maxScore").value(20.0))
                .andExpect(jsonPath("$.percent").value(50.0))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.submittedAt").value("2026-11-17T10:00:00Z"))
                .andExpect(jsonPath("$.questions[0].questionId").value(21))
                .andExpect(jsonPath("$.questions[0].correct").value(true))
                .andExpect(jsonPath("$.questions[1].correct").value(false))
                .andExpect(jsonPath("$.questions[1].earnedPoints").value(0.0))
                .andExpect(jsonPath("$.questions[1].maxPoints").value(10.0));
    }

    @Test
    void submit_missingAnswersField_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.answers").exists());
        verify(attemptService, never()).submit(any(), any(), any());
    }

    @Test
    void submit_emptyOptionIds_returns400() throws Exception {
        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\":[{\"questionId\":21,\"optionIds\":[]}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_notEnrolled_returns403FromService() throws Exception {
        given(attemptService.submit(any(), any(), any()))
                .willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kursa kayıtlı değilsiniz."));
    }

    @Test
    void submit_foreignQuestion_returns400FromService() throws Exception {
        given(attemptService.submit(any(), any(), any()))
                .willThrow(BadRequestException.forField("answers", "Cevaplanan soru bu sınava ait değil."));

        mockMvc.perform(post(SUBMIT_URL).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(SUBMIT_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.answers").value("Cevaplanan soru bu sınava ait değil."));
    }
}
