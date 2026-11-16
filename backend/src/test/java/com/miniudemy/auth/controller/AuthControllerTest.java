package com.miniudemy.auth.controller;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.mapper.UserMapperImpl;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.auth.service.AuthService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.common.security.SecurityMessages;

/**
 * {@code POST /api/auth/login} gerçek güvenlik ayarıyla (SecurityConfig + JwtConfig), gerçek AuthService ve
 * AccountLookupService ile; yalnız veritabanı ({@link UserRepository}) taklit edilir.
 *
 * Kabul kriteri 3 ("dönen token ile korumalı bir uç çağrılabiliyor"): login'in döndürdüğü token, yalnız kimlikli
 * isteklere açık bir test ucunda kullanılır.
 */
@WebMvcTest(controllers = {AuthController.class, AuthControllerTest.ProtectedEndpoint.class})
@Import({SecurityConfig.class, JwtConfig.class, AuthService.class, AccountLookupService.class, UserMapperImpl.class,
        AuthControllerTest.ProtectedEndpoint.class})
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String EMAIL = "ayse.yilmaz@example.com";
    private static final String PASSWORD = "GucluParola123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    private User ayse;

    @BeforeEach
    void users() {
        ayse = new User(EMAIL, passwordEncoder.encode(PASSWORD), "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(ayse, "id", 42L);
        ReflectionTestUtils.setField(ayse, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(ayse));
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));
    }

    @Test
    void login_withCorrectCredentials_returns200WithTokenAndUser_withoutAuthorizationHeader() throws Exception {
        mockMvc.perform(login(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(42))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.fullName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.createdAt").value("2026-11-11T09:30:00Z"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.active").doesNotExist());
    }

    @Test
    void login_withWrongPassword_returns401ErrorResponse() throws Exception {
        mockMvc.perform(login(EMAIL, "YanlisParola1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(SecurityMessages.INVALID_CREDENTIALS))
                .andExpect(jsonPath("$.errors", anEmptyMap()));
    }

    @Test
    void login_withUnknownEmail_returnsExactlyTheSameBodyAsWrongPassword() throws Exception {
        String wrongPassword = mockMvc.perform(login(EMAIL, "YanlisParola1"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(login("yok@example.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json(wrongPassword, true));
    }

    @Test
    void login_withDisabledAccount_returns403AccountDisabled() throws Exception {
        ReflectionTestUtils.setField(ayse, "active", false);

        mockMvc.perform(login(EMAIL, PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(SecurityMessages.ACCOUNT_DISABLED));
    }

    @Test
    void login_withMissingOrInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").value("Parola zorunludur."));

        mockMvc.perform(login("e-posta-degil", PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Geçerli bir e-posta adresi girin."));

        mockMvc.perform(login(EMAIL, "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Parola en fazla 72 karakter olabilir."));
    }

    @Test
    void returnedToken_canCallProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized());

        String body = mockMvc.perform(login(EMAIL, PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(get("/api/test/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String email, String password)
            throws Exception {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new java.util.LinkedHashMap<>(java.util.Map.of(
                        "email", email, "password", password))));
    }

    /** Yalnız kimlikli isteklere açık bir uç (SecurityConfig › anyRequest().authenticated()). */
    @RestController
    static class ProtectedEndpoint {

        @GetMapping("/api/test/protected")
        String whoAmI(org.springframework.security.core.Authentication authentication) {
            return authentication.getName();
        }
    }
}
