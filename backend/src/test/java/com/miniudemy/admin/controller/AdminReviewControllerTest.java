package com.miniudemy.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.miniudemy.admin.service.AdminReviewService;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.review.dto.ReviewPage;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.service.ReviewModerationService;

/**
 * Yorum moderasyonu uçları (#142) gerçek güvenlik ayarıyla ve gerçek AdminReviewService ile; review servisi taklit.
 * Kriter "Yalnız ADMIN" burada: token yoksa 401, kursiyer ve eğitmen 403, yönetici 200.
 */
@WebMvcTest(controllers = AdminReviewController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class, AdminReviewService.class})
@ActiveProfiles("test")
class AdminReviewControllerTest {

    private static final String HIDE = "{\"hidden\":true,\"reason\":\"Hakaret içeriyor\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ReviewModerationService moderation;

    @BeforeEach
    void users() {
        user(1L, "Ali Yönetici", Role.ADMIN);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
    }

    @Test
    void endpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")).andExpect(status().isUnauthorized());
        mockMvc.perform(visibility(HIDE)).andExpect(status().isUnauthorized());
        verify(moderation, never()).setVisibility(any(), anyBoolean(), any());
    }

    @Test
    void endpoints_asStudentOrInstructor_return403() throws Exception {
        for (String token : List.of(bearer(42L, "STUDENT"), bearer(7L, "INSTRUCTOR"))) {
            mockMvc.perform(get("/api/admin/reviews").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
            mockMvc.perform(visibility(HIDE).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        verify(moderation, never()).setVisibility(any(), anyBoolean(), any());
    }

    @Test
    void list_asAdmin_passesFiltersAndReturnsReasons() throws Exception {
        given(moderation.list(5L, true, 0, 20)).willReturn(new ReviewPage(List.of(hidden()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/admin/reviews?courseId=5&hidden=true").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].hidden").value(true))
                .andExpect(jsonPath("$.content[0].moderationReason").value("Hakaret içeriyor"));
    }

    @Test
    void setVisibility_asAdmin_returns200WithUpdatedReview() throws Exception {
        given(moderation.setVisibility(9L, true, "Hakaret içeriyor")).willReturn(hidden());

        mockMvc.perform(visibility(HIDE).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.hidden").value(true))
                .andExpect(jsonPath("$.moderationReason").value("Hakaret içeriyor"));
    }

    @Test
    void setVisibility_withMissingHiddenOrLongReason_returns400WithFieldError() throws Exception {
        mockMvc.perform(visibility("{}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.hidden").exists());
        mockMvc.perform(visibility("{\"hidden\":true,\"reason\":\"" + "a".repeat(1001) + "\"}")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        verify(moderation, never()).setVisibility(any(), anyBoolean(), any());
    }

    private static ReviewResponse hidden() {
        Instant at = Instant.parse("2026-11-18T10:00:00Z");
        return new ReviewResponse(9L, 5L, new UserSummary(42L, "Ayşe Yılmaz"), 2, "Ses kötü", true,
                "Hakaret içeriyor", at, at);
    }

    private MockHttpServletRequestBuilder visibility(String body) {
        return put("/api/admin/reviews/9/visibility").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String admin() {
        return bearer(1L, "ADMIN");
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
