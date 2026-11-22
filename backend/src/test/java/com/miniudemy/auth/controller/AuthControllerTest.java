package com.miniudemy.auth.controller;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.PasswordResetConfirmRequest;
import com.miniudemy.auth.mapper.UserMapperImpl;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.auth.service.AuthService;
import com.miniudemy.auth.service.PasswordResetService;
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

    @MockitoBean
    private PasswordResetService passwordResetService;

    private User ayse;

    @BeforeEach
    void users() {
        ayse = new User(EMAIL, passwordEncoder.encode(PASSWORD), "Ayşe Yılmaz", Role.STUDENT);
        ReflectionTestUtils.setField(ayse, "id", 42L);
        ReflectionTestUtils.setField(ayse, "createdAt", Instant.parse("2026-11-11T09:30:00Z"));
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(ayse));
        given(userRepository.findById(42L)).willReturn(Optional.of(ayse));
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(inv -> {
            User saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-11-17T08:00:00Z"));
            return saved;
        });
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

    // --- Kayıt (#12) ---

    @Test
    void register_withValidData_returns201WithUserAndNoToken_withoutAuthorizationHeader() throws Exception {
        mockMvc.perform(register("Mehmet Kaya", "mehmet.kaya@example.com", PASSWORD, "INSTRUCTOR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("mehmet.kaya@example.com"))
                .andExpect(jsonPath("$.fullName").value("Mehmet Kaya"))
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.createdAt").value("2026-11-17T08:00:00Z"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void register_withInvalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(register("A", "e-posta-degil", "kisa", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Girilen bilgilerde hata var."))
                .andExpect(jsonPath("$.errors.fullName").value("Ad soyad 2–100 karakter olmalıdır."))
                .andExpect(jsonPath("$.errors.email").value("Geçerli bir e-posta adresi girin."))
                .andExpect(jsonPath("$.errors.password").value("Parola en az 8 karakter olmalıdır."));

        mockMvc.perform(register("Ayşe Yılmaz", "yeni@example.com", "a".repeat(73), "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Parola en fazla 72 karakter olabilir."));

        mockMvc.perform(register("Ayşe Yılmaz", "yeni@example.com", PASSWORD, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").value("Rol zorunludur."));
    }

    @Test
    void register_asAdmin_returns400MatchingContractExample() throws Exception {
        mockMvc.perform(register("Admin Olmak", "admin@example.com", PASSWORD, "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"status": 400, "message": "Girilen bilgilerde hata var.",
                         "errors": {"role": "Bu rol ile kayıt olunamaz."}}""", true));
    }

    @Test
    void register_withUnknownRole_returns400() throws Exception {
        mockMvc.perform(register("Ayşe Yılmaz", "yeni@example.com", PASSWORD, "MODERATOR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withExistingEmail_returns409MatchingContractExample() throws Exception {
        mockMvc.perform(register("Ayşe Yılmaz", EMAIL, PASSWORD, "STUDENT"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"status": 409, "message": "Bu e-posta adresi zaten kayıtlı.",
                         "errors": {"email": "Bu e-posta adresi zaten kayıtlı."}}""", true));
    }

    // --- Parola yenileme (#115): uç sözleşmesi; kurallar PasswordResetServiceTest ve PasswordResetIT'te ---

    @Test
    void passwordResetRequest_anyValidEmail_returns202WithoutBody_withoutToken() throws Exception {
        for (String email : java.util.List.of(EMAIL, "kayitli.degil@example.com")) {
            mockMvc.perform(json("/api/auth/password-reset/request", java.util.Map.of("email", email)))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(""));
            then(passwordResetService).should().requestReset(email);
        }
    }

    @Test
    void passwordResetRequest_withInvalidEmail_returns400FieldError() throws Exception {
        mockMvc.perform(json("/api/auth/password-reset/request", java.util.Map.of("email", "e-posta-degil")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Geçerli bir e-posta adresi girin."));
        then(passwordResetService).shouldHaveNoInteractions();
    }

    @Test
    void passwordResetConfirm_valid_returns204() throws Exception {
        mockMvc.perform(json("/api/auth/password-reset/confirm",
                        java.util.Map.of("token", "abc", "newPassword", "YeniGucluParola456")))
                .andExpect(status().isNoContent());
        then(passwordResetService).should()
                .confirmReset(new PasswordResetConfirmRequest("abc", "YeniGucluParola456"));
    }

    @Test
    void passwordResetConfirm_shortPassword_returns400_andInvalidToken_returns400WithTokenError() throws Exception {
        mockMvc.perform(json("/api/auth/password-reset/confirm",
                        java.util.Map.of("token", "abc", "newPassword", "kisa")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("Parola en az 8 karakter olmalıdır."));

        willThrow(com.miniudemy.common.exception.BadRequestException.forField("token", "Bağlantı geçersiz."))
                .given(passwordResetService).confirmReset(any());
        mockMvc.perform(json("/api/auth/password-reset/confirm",
                        java.util.Map.of("token", "eski", "newPassword", "YeniGucluParola456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").value("Bağlantı geçersiz."));
    }

    private RequestBuilder json(String path, java.util.Map<String, ?> body) throws Exception {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private RequestBuilder register(String fullName, String email, String password, String role) throws Exception {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("fullName", fullName);
        body.put("email", email);
        body.put("password", password);
        body.put("role", role);
        return post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
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
