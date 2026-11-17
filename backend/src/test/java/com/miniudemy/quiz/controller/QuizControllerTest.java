package com.miniudemy.quiz.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.dto.QuizRequest;
import com.miniudemy.quiz.service.QuizService;

/** Sınav yazma uçları gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = QuizController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class QuizControllerTest {

    private static final String VALID_BODY = """
            {"title":"Bölüm 1 sınavı","scoringType":"SINGLE_ANSWER","passThreshold":70,"required":true,
             "questionIds":[21,22]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QuizService quizService;

    private final InstructorQuizResponse response = new InstructorQuizResponse(31L, 11L, "Bölüm 1 sınavı",
            ScoringType.SINGLE_ANSWER, 70, true, 1,
            List.of(new QuestionResponse(21L, 3L, "Soru", 10,
                    List.of(new OptionResponse(84L, "80", true), new OptionResponse(85L, "22", false)))));

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
    void createQuiz_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/sections/11/quizzes").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createQuiz_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(quizService);
    }

    @Test
    void createQuiz_withValidBody_returns201WithCorrectFlags() throws Exception {
        given(quizService.createQuiz(eq(11L), eq(7L), any(QuizRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.scoringType").value("SINGLE_ANSWER"))
                .andExpect(jsonPath("$.questionCount").value(1))
                .andExpect(jsonPath("$.questions[0].options[0].correct").value(true));
    }

    @Test
    void createQuiz_withThresholdOver100_returns400WithFieldError() throws Exception {
        String body = VALID_BODY.replace("\"passThreshold\":70", "\"passThreshold\":101");

        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.passThreshold").exists());
        verifyNoInteractions(quizService);
    }

    @Test
    void createQuiz_withEmptyQuestionIds_returns400WithFieldError() throws Exception {
        String body = VALID_BODY.replace("[21,22]", "[]");

        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.questionIds").exists());
    }

    @Test
    void createQuiz_withUnknownScoringType_returns400() throws Exception {
        String body = VALID_BODY.replace("SINGLE_ANSWER", "NOPE");

        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(quizService);
    }

    @Test
    void createQuiz_forForeignCourse_returns403FromService() throws Exception {
        given(quizService.createQuiz(eq(11L), eq(7L), any(QuizRequest.class)))
                .willThrow(new ForbiddenException("Bu kurs size ait değil."));

        mockMvc.perform(post("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kurs size ait değil."));
    }

    @Test
    void updateQuiz_returns200() throws Exception {
        given(quizService.updateQuiz(eq(31L), eq(7L), any(QuizRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passThreshold").value(70));
    }

    @Test
    void updateQuiz_whenConflict_returns409() throws Exception {
        given(quizService.updateQuiz(eq(31L), eq(7L), any(QuizRequest.class)))
                .willThrow(new ConflictException("Denemesi olan sınavın soruları değiştirilemez."));

        mockMvc.perform(put("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateQuiz_asStudent_returns403() throws Exception {
        mockMvc.perform(put("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateQuiz_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/quizzes/31").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteQuiz_returns204() throws Exception {
        mockMvc.perform(delete("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteQuiz_withAttempts_returns409() throws Exception {
        willThrow(new ConflictException("Bu sınavın denemeleri var, silinemez.")).given(quizService)
                .deleteQuiz(31L, 7L);

        mockMvc.perform(delete("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bu sınavın denemeleri var, silinemez."));
    }

    @Test
    void deleteQuiz_asStudent_returns403() throws Exception {
        mockMvc.perform(delete("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteQuiz_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/quizzes/31")).andExpect(status().isUnauthorized());
    }
}
