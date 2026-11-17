package com.miniudemy.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import com.miniudemy.auth.mapper.UserMapperImpl;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.auth.service.ProfileService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.common.security.SecurityMessages;

/**
 * {@code GET /api/me} gerçek güvenlik ayarıyla (SecurityConfig + JwtConfig) ve gerçek AccountLookupService ile;
 * yalnız veritabanı ({@link UserRepository}) taklit edilir.
 */
@WebMvcTest(controllers = MeController.class)
@Import({SecurityConfig.class, JwtConfig.class, ProfileService.class, AccountLookupService.class,
        UserMapperImpl.class})
@ActiveProfiles("test")
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User ayse;

    @BeforeEach
    void users() {
        ayse = new User("ayse.yilmaz@example.com", "$2a$10$ozet", "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(ayse, "id", 42L);
        ReflectionTestUtils.setField(ayse, "bio", "Yazılım mühendisliği öğrencisiyim.");
        ReflectionTestUtils.setField(ayse, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));
        given(userRepository.findById(99L)).willReturn(Optional.empty());
    }

    @Test
    void getMe_withoutToken_returns401ErrorResponse() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(SecurityMessages.UNAUTHORIZED));
    }

    @Test
    void getMe_withValidToken_returns200WithOwnProfile() throws Exception {
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("ayse.yilmaz@example.com"))
                .andExpect(jsonPath("$.fullName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.bio").value("Yazılım mühendisliği öğrencisiyim."))
                .andExpect(jsonPath("$.createdAt").value("2026-11-11T09:30:00Z"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getMe_returnsTheTokenOwner_notAnotherUser() throws Exception {
        User mehmet = new User("mehmet.kaya@example.com", "$2a$10$ozet", "Mehmet Kaya", Role.INSTRUCTOR);
        ReflectionTestUtils.setField(mehmet, "id", 7L);
        given(userRepository.findById(7L)).willReturn(Optional.of(mehmet));

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"));
    }

    @Test
    void getMe_withDisabledAccount_returns403AccountDisabled() throws Exception {
        ReflectionTestUtils.setField(ayse, "active", false);

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SecurityMessages.ACCOUNT_DISABLED));
    }

    @Test
    void getMe_whenUserDeletedAfterTokenWasIssued_returns401() throws Exception {
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(99L, "STUDENT")))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
