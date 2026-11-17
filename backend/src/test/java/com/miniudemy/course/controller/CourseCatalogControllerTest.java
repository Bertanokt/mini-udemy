package com.miniudemy.course.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.AccountLookup;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CatalogSort;
import com.miniudemy.course.service.CourseCatalogService;

/** Katalog uçları gerçek güvenlik ayarıyla: herkese açık erişim, sorgu parametresi doğrulaması, isteğe bağlı kimlik. */
@WebMvcTest(controllers = CourseCatalogController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ActiveProfiles("test")
class CourseCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseCatalogService catalogService;

    @MockitoBean
    private AccountLookup accountLookup;

    private static CourseSummaryResponse card() {
        return new CourseSummaryResponse(5L, "Java", new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER,
                CourseStatus.PUBLISHED, new UserSummary(7L, "Mehmet Demir"), null, 12, 3600, 41L, null, 0);
    }

    private static CourseDetailResponse detail(CourseStatus status) {
        return new CourseDetailResponse(5L, "Java", new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER, status,
                new UserSummary(7L, "Mehmet Demir"), null, 12, 3600, 41L, null, 0, "Açıklama", List.of("Hedef"),
                List.of());
    }

    private static JwtAuthenticationToken authenticatedAs(long userId, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(String.valueOf(userId)).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void searchCatalog_withoutToken_returns200WithPageBody() throws Exception {
        given(catalogService.search(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(new CourseSummaryPage(List.of(card()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Java"))
                .andExpect(jsonPath("$.content[0].instructor.fullName").value("Mehmet Demir"))
                .andExpect(jsonPath("$.content[0].enrollmentCount").value(41))
                .andExpect(jsonPath("$.content[0].reviewCount").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchCatalog_passesQueryParametersToService() throws Exception {
        given(catalogService.search(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(new CourseSummaryPage(List.of(), 1, 5, 0, 0));

        mockMvc.perform(get("/api/courses").param("q", "java").param("categoryId", "3").param("level", "ADVANCED")
                .param("instructorId", "9").param("minRating", "4.5").param("sort", "popular")
                .param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        verify(catalogService).search("java", 3L, CourseLevel.ADVANCED, 9L, 4.5, CatalogSort.POPULAR, 1, 5);
    }

    @Test
    void searchCatalog_withoutParameters_usesContractDefaults() throws Exception {
        given(catalogService.search(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(new CourseSummaryPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/courses")).andExpect(status().isOk());

        verify(catalogService).search(eq(null), eq(null), eq(null), eq(null), eq(null), eq(CatalogSort.NEWEST), eq(0),
                eq(20));
    }

    @Test
    void searchCatalog_invalidLevel_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("level", "EXPERT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.level").exists());
        verifyNoInteractions(catalogService);
    }

    @Test
    void searchCatalog_invalidSort_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("sort", "cheapest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").exists());
        verifyNoInteractions(catalogService);
    }

    @Test
    void searchCatalog_sizeOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("size", "101")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").exists());
        mockMvc.perform(get("/api/courses").param("size", "0")).andExpect(status().isBadRequest());
        verifyNoInteractions(catalogService);
    }

    @Test
    void searchCatalog_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("page", "-1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").exists());
    }

    @Test
    void searchCatalog_minRatingOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("minRating", "0.5")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/courses").param("minRating", "5.1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minRating").exists());
    }

    @Test
    void searchCatalog_nonNumericCategoryId_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("categoryId", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void searchCatalog_keywordLongerThan100_returns400() throws Exception {
        mockMvc.perform(get("/api/courses").param("q", "a".repeat(101))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.q").exists());
    }

    @Test
    void searchCatalog_withInvalidToken_returns401AsInContract() throws Exception {
        mockMvc.perform(get("/api/courses").header(HttpHeaders.AUTHORIZATION, "Bearer bozuk.token.x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCourseDetail_withoutToken_returns200AndPassesAnonymousViewer() throws Exception {
        given(catalogService.getDetail(5L, null, false)).willReturn(detail(CourseStatus.PUBLISHED));

        mockMvc.perform(get("/api/courses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.description").value("Açıklama"))
                .andExpect(jsonPath("$.learningOutcomes[0]").value("Hedef"))
                .andExpect(jsonPath("$.sections", hasSize(0)));
    }

    @Test
    void getCourseDetail_withInstructorToken_passesViewerIdAndNoAdmin() throws Exception {
        given(catalogService.getDetail(5L, 7L, false)).willReturn(detail(CourseStatus.DRAFT));

        mockMvc.perform(get("/api/courses/5").with(authentication(authenticatedAs(7, "INSTRUCTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getCourseDetail_withAdminToken_passesAdminFlag() throws Exception {
        given(catalogService.getDetail(5L, 1L, true)).willReturn(detail(CourseStatus.IN_REVIEW));

        mockMvc.perform(get("/api/courses/5").with(authentication(authenticatedAs(1, "ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getCourseDetail_unknownOrHiddenCourse_returns404WithErrorBody() throws Exception {
        given(catalogService.getDetail(5L, null, false)).willThrow(new NotFoundException("Kurs bulunamadı."));

        mockMvc.perform(get("/api/courses/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Kurs bulunamadı."));
    }

    @Test
    void getCourseDetail_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/courses/abc")).andExpect(status().isBadRequest());
    }
}
