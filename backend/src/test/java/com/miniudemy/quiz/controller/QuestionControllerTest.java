package com.miniudemy.quiz.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionRequest;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.service.QuestionService;

/** Soru bankası uçları gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = QuestionController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class QuestionControllerTest {

    private static final String VALID_BODY = """
            {"text":"HTTP'nin varsayılan portu hangisidir?","points":10,
             "options":[{"text":"80","correct":true},{"text":"22","correct":false}]}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QuestionService questionService;

    private final QuestionResponse response = new QuestionResponse(21L, 3L, "HTTP'nin varsayılan portu hangisidir?",
            10, List.of(new OptionResponse(84L, "80", true), new OptionResponse(85L, "22", false)));

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
    void listQuestions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/courses/3/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    void listQuestions_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(questionService);
    }

    @Test
    void listQuestions_asOwnerInstructor_returnsQuestionsWithCorrectFlag() throws Exception {
        given(questionService.listQuestions(3L, 7L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].courseId").value(3))
                .andExpect(jsonPath("$[0].options[0].correct").value(true))
                .andExpect(jsonPath("$[0].options[1].correct").value(false));
    }

    @Test
    void listQuestions_forForeignCourse_returns403FromService() throws Exception {
        given(questionService.listQuestions(3L, 7L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        mockMvc.perform(get("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kurs size ait değil."));
    }

    @Test
    void createQuestion_withValidBody_returns201() throws Exception {
        given(questionService.createQuestion(eq(3L), eq(7L), any(QuestionRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21));
    }

    @Test
    void createQuestion_withSingleOption_returns400WithFieldError() throws Exception {
        String body = """
                {"text":"HTTP'nin varsayılan portu hangisidir?","points":10,"options":[{"text":"80","correct":true}]}""";

        mockMvc.perform(post("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.options").exists());
        verifyNoInteractions(questionService);
    }

    @Test
    void createQuestion_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/courses/3/questions").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createQuestion_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/courses/3/questions").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateQuestion_returns200() throws Exception {
        given(questionService.updateQuestion(eq(21L), eq(7L), any(QuestionRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(10));
    }

    @Test
    void updateQuestion_whenAnswered_returns409() throws Exception {
        given(questionService.updateQuestion(eq(21L), eq(7L), any(QuestionRequest.class)))
                .willThrow(new ConflictException("Denemelerde cevaplanmış bir sorunun şıkları değiştirilemez."));

        mockMvc.perform(put("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void updateQuestion_asStudent_returns403() throws Exception {
        mockMvc.perform(put("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteQuestion_returns204() throws Exception {
        mockMvc.perform(delete("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteQuestion_whenUsedInQuiz_returns409() throws Exception {
        willThrow(new ConflictException("Bu soru bir sınavda kullanılıyor, silinemez."))
                .given(questionService).deleteQuestion(21L, 7L);

        mockMvc.perform(delete("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bu soru bir sınavda kullanılıyor, silinemez."));
    }

    @Test
    void deleteQuestion_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/questions/21")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteQuestion_asStudent_returns403() throws Exception {
        mockMvc.perform(delete("/api/questions/21").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
    }
}
