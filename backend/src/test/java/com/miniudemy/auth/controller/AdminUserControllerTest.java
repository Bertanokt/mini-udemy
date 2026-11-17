package com.miniudemy.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.mapper.AdminUserMapperImpl;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.auth.service.AdminUserService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;

/** {@code /api/admin/users} gerçek güvenlik ayarıyla; yalnız {@link UserRepository} taklit edilir. */
@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, JwtConfig.class, AdminUserService.class, AccountLookupService.class,
        AdminUserMapperImpl.class})
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User admin;
    private User ayse;
    private User student;

    private static User user(long id, String email, String name, Role role) {
        User u = new User(email, "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        return u;
    }

    @BeforeEach
    void users() {
        admin = user(1L, "admin@example.com", "Yönetici", Role.ADMIN);
        ayse = user(42L, "ayse@example.com", "Ayşe Yılmaz", Role.STUDENT);
        student = user(7L, "mehmet@example.com", "Mehmet Kaya", Role.STUDENT);
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));
        given(userRepository.findById(7L)).willReturn(Optional.of(student));
        given(userRepository.findById(99L)).willReturn(Optional.empty());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void list_asStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(7L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void list_asInstructor_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_asAdmin_returns200PageWithoutPasswordHash() throws Exception {
        given(userRepository.searchForAdmin(eq(Role.STUDENT), eq(true), eq("%ay%"), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(ayse), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/users").param("role", "STUDENT").param("active", "true").param("q", "Ay")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].email").value("ayse@example.com"))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.content[0].active").value(true))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-11-11T09:30:00Z"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void list_withInvalidSize_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setStatus_asStudent_returns403() throws Exception {
        mockMvc.perform(statusRequest(42L, "{\"active\":false}", bearer(7L, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void setStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(put("/api/admin/users/42/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setStatus_asAdmin_deactivatesUser() throws Exception {
        mockMvc.perform(statusRequest(42L, "{\"active\":false}", bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void setStatus_adminOnSelf_returns409() throws Exception {
        mockMvc.perform(statusRequest(1L, "{\"active\":false}", bearer(1L, "ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void setStatus_unknownUser_returns404() throws Exception {
        mockMvc.perform(statusRequest(99L, "{\"active\":false}", bearer(1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void setStatus_missingActive_returns400WithFieldError() throws Exception {
        mockMvc.perform(statusRequest(42L, "{}", bearer(1L, "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.active").exists());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder statusRequest(long id,
            String body, String authorization) {
        return put("/api/admin/users/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content(body)
                .header(HttpHeaders.AUTHORIZATION, authorization);
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
