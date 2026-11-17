package com.miniudemy.progress.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.common.security.AccountLookup;
import com.miniudemy.common.security.AccountLookup.AccountSnapshot;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.CourseProgressResponse.SectionProgress;
import com.miniudemy.progress.service.LessonCompletionService;

/** {@code POST /api/lessons/{id}/complete} gerçek güvenlik ayarıyla: yalnız STUDENT, yanıt güncel ilerleme. */
@WebMvcTest(controllers = LessonCompletionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ActiveProfiles("test")
class LessonCompletionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LessonCompletionService completion;

    @MockitoBean
    private AccountLookup accountLookup;

    @Test
    void complete_asStudent_returns200WithCourseProgress_forTokenOwner() throws Exception {
        given(accountLookup.findById(42L)).willReturn(Optional.of(new AccountSnapshot(true, "STUDENT")));
        given(completion.complete(42L, 11L)).willReturn(
                new CourseProgressResponse(3L, 33.33, List.of(11L), List.of(new SectionProgress(7L, 50.0))));

        mockMvc.perform(post("/api/lessons/11/complete").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(3))
                .andExpect(jsonPath("$.progressPercent").value(33.33))
                .andExpect(jsonPath("$.completedLessonIds[0]").value(11))
                .andExpect(jsonPath("$.sections[0].progressPercent").value(50.0));
    }

    @Test
    void complete_asInstructor_returns403() throws Exception {
        given(accountLookup.findById(7L)).willReturn(Optional.of(new AccountSnapshot(true, "INSTRUCTOR")));

        mockMvc.perform(post("/api/lessons/11/complete").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void complete_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/lessons/11/complete")).andExpect(status().isUnauthorized());
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
