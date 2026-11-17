package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.dto.CategoryRequest;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.service.CategoryService;

/** Kategori yönetim uçları gerçek güvenlik ayarıyla; service ve kullanıcı veritabanı taklit edilir. */
@WebMvcTest(controllers = AdminCategoryController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CategoryService categoryService;

    @BeforeEach
    void users() {
        addUser(1L, Role.ADMIN);
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
    void createCategory_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Müzik\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Müzik\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(categoryService);
    }

    @Test
    void updateAndDelete_asInstructor_return403() throws Exception {
        mockMvc.perform(put("/api/admin/categories/4").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Müzik\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/categories/4").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_asAdmin_returns201WithBody() throws Exception {
        given(categoryService.createCategory(any(CategoryRequest.class))).willReturn(new CategoryResponse(9L, "Müzik"));

        mockMvc.perform(post("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  Müzik \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Müzik"));
        // Boşluklar istek nesnesinde kırpılır.
        verify(categoryService).createCategory(new CategoryRequest("Müzik"));
    }

    @Test
    void createCategory_withTooShortOrBlankName_returns400WithFieldError() throws Exception {
        for (String body : new String[] {"{\"name\":\" a \"}", "{\"name\":\"   \"}", "{}"}) {
            mockMvc.perform(post("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());
        }
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_withTooLongName_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createCategory_whenNameTaken_returns409WithNameError() throws Exception {
        given(categoryService.createCategory(any(CategoryRequest.class))).willThrow(
                new ConflictException("Bu adda bir kategori zaten var.", Map.of("name", "Bu adda bir kategori zaten var.")));

        mockMvc.perform(post("/api/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"yazılım\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errors.name").value("Bu adda bir kategori zaten var."));
    }

    @Test
    void updateCategory_asAdmin_returns200() throws Exception {
        given(categoryService.updateCategory(eq(4L), any(CategoryRequest.class)))
                .willReturn(new CategoryResponse(4L, "Yabancı Dil"));

        mockMvc.perform(put("/api/admin/categories/4").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Yabancı Dil\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yabancı Dil"));
    }

    @Test
    void updateCategory_whenMissing_returns404() throws Exception {
        given(categoryService.updateCategory(eq(99L), any(CategoryRequest.class)))
                .willThrow(new NotFoundException("Kategori bulunamadı."));

        mockMvc.perform(put("/api/admin/categories/99").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Müzik\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/4").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isNoContent());
        verify(categoryService).deleteCategory(4L);
    }

    @Test
    void deleteCategory_whenInUse_returns409WithServerMessage() throws Exception {
        willThrow(new ConflictException("Bu kategori bir kursta kullanıldığı için silinemez."))
                .given(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/admin/categories/1").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Bu kategori bir kursta kullanıldığı için silinemez."));
    }
}
