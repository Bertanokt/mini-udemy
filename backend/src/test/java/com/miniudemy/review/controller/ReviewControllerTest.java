package com.miniudemy.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.review.dto.CourseReviewsResponse;
import com.miniudemy.review.dto.RatingSummary;
import com.miniudemy.review.dto.ReviewRequest;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.service.ReviewService;

/** Değerlendirme uçları gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class ReviewControllerTest {

    private static final String BODY = "{\"rating\":4,\"comment\":\"İyi\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ReviewService reviewService;

    private final ReviewResponse response = new ReviewResponse(9L, 5L, new UserSummary(42L, "Ayşe Kaya"), 4, "İyi",
            false, null, Instant.parse("2026-11-10T10:00:00Z"), Instant.parse("2026-11-10T10:00:00Z"));

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
    void listCourseReviews_withoutToken_returns200WithSummary() throws Exception {
        given(reviewService.listCourseReviews(5L, 0, 20)).willReturn(new CourseReviewsResponse(List.of(response), 0,
                20, 1, 1, new RatingSummary(4.0, 1, Map.of("4", 1L))));

        mockMvc.perform(get("/api/courses/5/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].author.fullName").value("Ayşe Kaya"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.summary.averageRating").value(4.0))
                .andExpect(jsonPath("$.summary.distribution.4").value(1));
    }

    @Test
    void listCourseReviews_invalidSize_returns400() throws Exception {
        mockMvc.perform(get("/api/courses/5/reviews").param("size", "0")).andExpect(status().isBadRequest());
        verifyNoInteractions(reviewService);
    }

    @Test
    void createReview_asStudent_returns201() throws Exception {
        given(reviewService.create(5L, 42L, new ReviewRequest(4, "İyi"))).willReturn(response);

        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.hidden").value(false));
    }

    @Test
    void createReview_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/courses/5/reviews").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(reviewService);
    }

    @Test
    void createReview_asInstructor_returns403() throws Exception {
        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reviewService);
    }

    @Test
    void createReview_notEnrolled_returns403FromService() throws Exception {
        given(reviewService.create(any(), any(), any())).willThrow(new ForbiddenException("Kayıtlı değilsiniz."));

        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Kayıtlı değilsiniz."));
    }

    @Test
    void createReview_second_returns409FromService() throws Exception {
        given(reviewService.create(any(), any(), any())).willThrow(new ConflictException("Zaten değerlendirdiniz."));

        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createReview_ratingOutOfRange_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());
        verifyNoInteractions(reviewService);
    }

    @Test
    void createReview_missingRating_returns400() throws Exception {
        mockMvc.perform(post("/api/courses/5/reviews").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());
    }

    @Test
    void updateReview_asOwner_returns200() throws Exception {
        given(reviewService.update(9L, 42L, new ReviewRequest(4, "İyi"))).willReturn(response);

        mockMvc.perform(put("/api/reviews/9").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void updateReview_someoneElses_returns403FromService() throws Exception {
        given(reviewService.update(any(), any(), any())).willThrow(new ForbiddenException("Sizin değil."));

        mockMvc.perform(put("/api/reviews/9").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateReview_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/reviews/9").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }
}
