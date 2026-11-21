package com.miniudemy.certificate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.certificate.dto.CertificateResponse;
import com.miniudemy.certificate.dto.CertificateVerificationResponse;
import com.miniudemy.certificate.service.CertificateQueryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;

/** Sertifika uçları gerçek güvenlik ayarıyla (#130): /api/me/** yalnız STUDENT, doğrulama girişsiz. Servis taklit. */
@WebMvcTest(controllers = CertificateController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class CertificateControllerTest {

    private static final Instant ISSUED = Instant.parse("2026-11-18T09:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CertificateQueryService certificates;

    @BeforeEach
    void users() {
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(1L, "Ali Yönetici", Role.ADMIN);
    }

    @Test
    void listMine_asStudent_returnsTheStudentsCertificates() throws Exception {
        given(certificates.listMine(42L)).willReturn(List.of(new CertificateResponse(900L, 7L, "Sıfırdan Python",
                "Ayşe Yılmaz", "Mehmet Kaya", "K7Q2-M9XA-4T", ISSUED)));

        mockMvc.perform(get("/api/me/certificates").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(900))
                .andExpect(jsonPath("$[0].courseTitle").value("Sıfırdan Python"))
                .andExpect(jsonPath("$[0].studentName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$[0].instructorName").value("Mehmet Kaya"))
                .andExpect(jsonPath("$[0].verificationCode").value("K7Q2-M9XA-4T"))
                .andExpect(jsonPath("$[0].issuedAt").value("2026-11-18T09:00:00Z"));
    }

    @Test
    void meEndpoints_asInstructorOrAdmin_return403_withoutToken401() throws Exception {
        for (String path : List.of("/api/me/certificates", "/api/me/certificates/900")) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        verify(certificates, never()).listMine(any());
        verify(certificates, never()).getMine(any(), any());
    }

    @Test
    void getMine_passesTheCertificateAndTheStudentFromTheToken_andMapsNotFoundTo404() throws Exception {
        given(certificates.getMine(900L, 42L)).willThrow(new NotFoundException("Sertifika bulunamadı."));

        mockMvc.perform(get("/api/me/certificates/900").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sertifika bulunamadı."));
        verify(certificates).getMine(900L, 42L);
    }

    @Test
    void verify_withoutLogin_returnsOnlyNameCourseDateAndValidity() throws Exception {
        given(certificates.verify("K7Q2-M9XA-4T"))
                .willReturn(new CertificateVerificationResponse(true, "Sıfırdan Python", "Ayşe Yılmaz", ISSUED));

        mockMvc.perform(get("/api/certificates/verify/K7Q2-M9XA-4T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.courseTitle").value("Sıfırdan Python"))
                .andExpect(jsonPath("$.studentName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.issuedAt").value("2026-11-18T09:00:00Z"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void verify_unknownCode_returns404WithoutLogin() throws Exception {
        given(certificates.verify("YOKK-YOKK-YO")).willThrow(new NotFoundException("Sertifika bulunamadı."));

        mockMvc.perform(get("/api/certificates/verify/YOKK-YOKK-YO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
