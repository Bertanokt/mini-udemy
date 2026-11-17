package com.miniudemy.course.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.common.security.AccountLookup;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.service.CategoryService;

/** {@code GET /api/categories} gerçek güvenlik ayarıyla: herkese açık, gövde sözleşmedeki gibi. */
@WebMvcTest(controllers = CategoryController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AccountLookup accountLookup;

    @Test
    void listCategories_withoutToken_returns200WithIdAndNameArray() throws Exception {
        given(categoryService.listCategories())
                .willReturn(List.of(new CategoryResponse(4L, "Dil"), new CategoryResponse(2L, "İş")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].name").value("Dil"))
                .andExpect(jsonPath("$[1].name").value("İş"));
    }

    @Test
    void listCategories_withInvalidToken_returns401AsInContract() throws Exception {
        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer bozuk.token.x"))
                .andExpect(status().isUnauthorized());
    }
}
