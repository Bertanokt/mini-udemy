package com.miniudemy.quiz.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.dto.QuizSummaryResponse;
import com.miniudemy.quiz.dto.StudentOptionResponse;
import com.miniudemy.quiz.dto.StudentQuestionResponse;
import com.miniudemy.quiz.dto.StudentQuizResponse;
import com.miniudemy.quiz.service.QuizService;

/** Sınav okuma uçları (#49) gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = QuizController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class QuizViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private QuizService quizService;

    private final StudentQuizResponse studentView = new StudentQuizResponse(31L, 11L, "Bölüm 1 sınavı",
            ScoringType.SINGLE_ANSWER, 70, true, 1,
            List.of(new StudentQuestionResponse(21L, "Soru", 10,
                    List.of(new StudentOptionResponse(84L, "80"), new StudentOptionResponse(85L, "22")))));

    private final InstructorQuizResponse instructorView = new InstructorQuizResponse(31L, 11L, "Bölüm 1 sınavı",
            ScoringType.SINGLE_ANSWER, 70, true, 1,
            List.of(new QuestionResponse(21L, 3L, "Soru", 10,
                    List.of(new OptionResponse(84L, "80", true), new OptionResponse(85L, "22", false)))));

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

    // ---- GET /api/quizzes/{id}

    @Test
    void getQuizForStudent_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/quizzes/31")).andExpect(status().isUnauthorized());
    }

    @Test
    void getQuizForStudent_asInstructor_returns403() throws Exception {
        mockMvc.perform(get("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQuizForStudent_bodyNeverContainsCorrectKey() throws Exception {
        given(quizService.getQuizForStudent(31L, 42L)).willReturn(studentView);

        String body = mockMvc.perform(get("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].text").value("80"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContainIgnoringCase("correct");
    }

    @Test
    void getQuizForStudent_notEnrolled_returns403FromService() throws Exception {
        given(quizService.getQuizForStudent(31L, 42L))
                .willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        mockMvc.perform(get("/api/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bu kursa kayıtlı değilsiniz."));
    }

    @Test
    void getQuizForStudent_unknownQuiz_returns404() throws Exception {
        given(quizService.getQuizForStudent(999L, 42L)).willThrow(new NotFoundException("Sınav bulunamadı."));

        mockMvc.perform(get("/api/quizzes/999").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isNotFound());
    }

    // ---- GET /api/instructor/quizzes/{id}

    @Test
    void getQuizForInstructor_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/instructor/quizzes/31")).andExpect(status().isUnauthorized());
    }

    @Test
    void getQuizForInstructor_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/instructor/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQuizForInstructor_asOwner_returnsCorrectFlags() throws Exception {
        given(quizService.getQuizForInstructor(31L, 7L)).willReturn(instructorView);

        mockMvc.perform(get("/api/instructor/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].correct").value(true))
                .andExpect(jsonPath("$.questions[0].options[1].correct").value(false));
    }

    @Test
    void getQuizForInstructor_foreignCourse_returns403FromService() throws Exception {
        given(quizService.getQuizForInstructor(31L, 7L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        mockMvc.perform(get("/api/instructor/quizzes/31").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }

    // ---- GET /api/sections/{id}/quizzes

    @Test
    void listSectionQuizzes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/sections/11/quizzes")).andExpect(status().isUnauthorized());
    }

    @Test
    void listSectionQuizzes_passesRoleToService() throws Exception {
        QuizSummaryResponse summary = new QuizSummaryResponse(31L, 11L, "Bölüm 1 sınavı", ScoringType.SINGLE_ANSWER,
                70, true, 1);
        given(quizService.listSectionQuizzes(11L, 1L, "ADMIN")).willReturn(List.of(summary));
        given(quizService.listSectionQuizzes(11L, 42L, "STUDENT")).willThrow(new ForbiddenException("Kayıtlı değil."));

        mockMvc.perform(get("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionCount").value(1))
                .andExpect(jsonPath("$[0].questions").doesNotExist());
        mockMvc.perform(get("/api/sections/11/quizzes").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
    }
}
