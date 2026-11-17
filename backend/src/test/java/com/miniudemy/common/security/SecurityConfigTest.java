package com.miniudemy.common.security;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.exception.ErrorResponse;
import com.miniudemy.common.security.AccountLookup.AccountSnapshot;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Gerçek güvenlik ayarıyla (SecurityConfig + JwtConfig) uçtan uca: token yok / geçersiz / süresi dolmuş →
 * 401; rol yetersiz → 403 (hem adres kuralında hem {@code @PreAuthorize}'da, 500 değil); pasif kullanıcı → 403
 * "devre dışı"; silinmiş kullanıcı → 401; rol token'dan değil veritabanından. Tüm hata gövdeleri
 * {@link ErrorResponse} biçiminde ve mesajları sözleşmedeki örneklerle aynı.
 *
 * Veritabanı yerine {@link AccountLookup} sahte bir bean'dir; uçlar aşağıdaki iç sınıftadır.
 */
@WebMvcTest(controllers = SecurityConfigTest.TestEndpoints.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityConfigTest.TestEndpoints.class})
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final long STUDENT = 1L;
    private static final long ADMIN = 2L;
    private static final long DISABLED = 3L;
    private static final long DELETED = 4L;
    private static final long DEMOTED = 5L;
    private static final long INSTRUCTOR = 6L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private AccountLookup accountLookup;

    @BeforeEach
    void accounts() {
        given(accountLookup.findById(STUDENT)).willReturn(Optional.of(new AccountSnapshot(true, "STUDENT")));
        given(accountLookup.findById(ADMIN)).willReturn(Optional.of(new AccountSnapshot(true, "ADMIN")));
        given(accountLookup.findById(DISABLED)).willReturn(Optional.of(new AccountSnapshot(false, "STUDENT")));
        given(accountLookup.findById(DELETED)).willReturn(Optional.empty());
        given(accountLookup.findById(INSTRUCTOR)).willReturn(Optional.of(new AccountSnapshot(true, "INSTRUCTOR")));
        // Token'ı ADMIN iken alınmış, sonra rolü STUDENT'a düşürülmüş kullanıcı.
        given(accountLookup.findById(DEMOTED)).willReturn(Optional.of(new AccountSnapshot(true, "STUDENT")));
    }

    // ---------------------------------------------------------------- herkese açık uçlar

    @Test
    void publicEndpoint_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/courses")).andExpect(status().isOk());
    }

    @Test
    void publicPost_withoutToken_isNotBlockedByCsrfOrAuth() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- 401

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        expectError(get("/api/me/ping"), 401, SecurityMessages.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401() throws Exception {
        expectError(get("/api/me/ping").header(HttpHeaders.AUTHORIZATION, "Bearer bu-bir-jwt-degil"),
                401, SecurityMessages.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(String.valueOf(STUDENT)).claim("role", "STUDENT")
                        .issuedAt(past.minusSeconds(3600)).expiresAt(past).build())).getTokenValue();

        expectError(get("/api/me/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired),
                401, SecurityMessages.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withTokenSignedByAnotherKey_returns401() throws Exception {
        byte[] otherKey = "baska-bir-anahtar-ile-imzalanmis-sahte-token-12345".getBytes(StandardCharsets.UTF_8);
        JwtEncoder attacker = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
        String forged = attacker.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(String.valueOf(ADMIN)).claim("role", "ADMIN")
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build())).getTokenValue();

        expectError(get("/api/admin/ping").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged),
                401, SecurityMessages.UNAUTHORIZED);
    }

    @Test
    void deletedUser_tokenRejected_returns401() throws Exception {
        expectError(withToken(get("/api/me/ping"), DELETED, "STUDENT"), 401, SecurityMessages.UNAUTHORIZED);
    }

    // ---------------------------------------------------------------- kimlikli erişim

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        mockMvc.perform(withToken(get("/api/me/ping"), STUDENT, "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void protectedPost_withValidToken_isNotBlockedByCsrf() throws Exception {
        mockMvc.perform(withToken(post("/api/me/ping"), STUDENT, "STUDENT")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- 403

    @Test
    void adminUrl_asStudent_returns403FromFilter() throws Exception {
        expectError(withToken(get("/api/admin/ping"), STUDENT, "STUDENT"), 403, SecurityMessages.FORBIDDEN);
    }

    @Test
    void adminUrl_asAdmin_returns200() throws Exception {
        mockMvc.perform(withToken(get("/api/admin/ping"), ADMIN, "ADMIN")).andExpect(status().isOk());
    }

    @Test
    void preAuthorize_asStudent_returns403NotServerError() throws Exception {
        // @PreAuthorize reddi controller içinde AccessDeniedException fırlatır; GlobalExceptionHandler'ın
        // genel yakalayıcısına düşüp 500 olmamalı (#11'den #10'a düşülen not).
        expectError(withToken(get("/test-secure/admin-only"), STUDENT, "STUDENT"), 403, SecurityMessages.FORBIDDEN);
    }

    @Test
    void preAuthorize_asAdmin_returns200() throws Exception {
        mockMvc.perform(withToken(get("/test-secure/admin-only"), ADMIN, "ADMIN")).andExpect(status().isOk());
    }

    @Test
    void preAuthorizeInstructor_asStudent_returns403WithStandardBody() throws Exception {
        expectError(withToken(get("/test-secure/instructor-only"), STUDENT, "STUDENT"), 403, SecurityMessages.FORBIDDEN);
    }

    @Test
    void preAuthorizeInstructor_asInstructor_returns200() throws Exception {
        mockMvc.perform(withToken(get("/test-secure/instructor-only"), INSTRUCTOR, "INSTRUCTOR"))
                .andExpect(status().isOk());
    }

    @Test
    void disabledUser_returns403AccountDisabled() throws Exception {
        expectError(withToken(get("/api/me/ping"), DISABLED, "STUDENT"), 403, SecurityMessages.ACCOUNT_DISABLED);
    }

    @Test
    void roleComesFromDatabase_notFromToken() throws Exception {
        // Token "ADMIN" diyor ama kullanıcının güncel rolü STUDENT: yönetici ucu reddedilmeli.
        expectError(withToken(get("/api/admin/ping"), DEMOTED, "ADMIN"), 403, SecurityMessages.FORBIDDEN);
        expectError(withToken(get("/test-secure/admin-only"), DEMOTED, "ADMIN"), 403, SecurityMessages.FORBIDDEN);
    }

    // ---------------------------------------------------------------- yardımcılar

    private RequestBuilder withToken(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, long userId, String role) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(userId, role));
    }

    private ResultActions expectError(RequestBuilder request, int status, String message) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors", aMapWithSize(0)));
    }

    @RestController
    static class TestEndpoints {

        @GetMapping("/api/courses")
        String catalog() {
            return "katalog";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "giris";
        }

        @GetMapping("/api/me/ping")
        String me(org.springframework.security.core.Authentication authentication) {
            return authentication.getName();
        }

        @PostMapping("/api/me/ping")
        String mePost() {
            return "ok";
        }

        @GetMapping("/api/admin/ping")
        String admin() {
            return "admin";
        }

        @GetMapping("/test-secure/admin-only")
        @PreAuthorize("hasRole('ADMIN')")
        String adminOnly() {
            return "admin";
        }

        /** docs/architecture.md › Yetki kontrolleri: uç bazlı rol kuralı örneği (#14). */
        @GetMapping("/test-secure/instructor-only")
        @PreAuthorize("hasRole('INSTRUCTOR')")
        String instructorOnly() {
            return "instructor";
        }
    }
}
