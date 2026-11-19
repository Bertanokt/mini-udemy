package com.miniudemy.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.miniudemy.admin.domain.ComplaintStatus;
import com.miniudemy.admin.dto.ComplaintPage;
import com.miniudemy.admin.dto.ComplaintResponse;
import com.miniudemy.admin.dto.CreateComplaintRequest;
import com.miniudemy.admin.service.ComplaintService;
import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;

/**
 * Şikâyet uçları (#144) gerçek güvenlik ayarıyla; servis taklit. Oluşturma her role açık, yönetici uçları yalnız
 * ADMIN; gerekçe ve durum doğrulaması servise ulaşmadan 400 döner.
 */
@WebMvcTest(controllers = {ComplaintController.class, AdminComplaintController.class})
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ComplaintService complaintService;

    @BeforeEach
    void users() {
        user(1L, "Ali Yönetici", Role.ADMIN);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
    }

    // --- oluşturma

    @Test
    void create_asAnyRole_returns201WithOpenComplaint() throws Exception {
        given(complaintService.create(any(), any())).willReturn(response(ComplaintStatus.OPEN));

        for (String token : List.of(bearer(42L, "STUDENT"), bearer(7L, "INSTRUCTOR"), bearer(1L, "ADMIN"))) {
            mockMvc.perform(create("{\"courseId\":5,\"reason\":\"Kurs içeriği yanıltıcı.\"}")
                            .header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.courseId").value(5))
                    .andExpect(jsonPath("$.reporter.fullName").value("Ayşe Yılmaz"));
        }
        verify(complaintService).create(42L, new CreateComplaintRequest(5L, null, "Kurs içeriği yanıltıcı."));
    }

    @Test
    void create_withMissingBlankOrShortReason_returns400WithReasonError_andCreatesNothing() throws Exception {
        for (String body : List.of("{\"courseId\":5}", "{\"courseId\":5,\"reason\":\"   \"}",
                "{\"courseId\":5,\"reason\":\"Kötü\"}")) {
            mockMvc.perform(create(body).header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        verifyNoInteractions(complaintService);
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(create("{\"courseId\":5,\"reason\":\"Kurs içeriği yanıltıcı.\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(complaintService);
    }

    // --- yönetici

    @Test
    void list_asAdmin_passesStatusAndPaging() throws Exception {
        given(complaintService.list(ComplaintStatus.OPEN, 1, 10))
                .willReturn(new ComplaintPage(List.of(response(ComplaintStatus.OPEN)), 1, 10, 11, 2));

        mockMvc.perform(get("/api/admin/complaints?status=OPEN&page=1&size=10")
                        .header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    void setStatus_asAdmin_returnsResolvedComplaint_withAdminAsResolver() throws Exception {
        given(complaintService.setStatus(3L, ComplaintStatus.REVIEWED, 1L))
                .willReturn(response(ComplaintStatus.REVIEWED));

        mockMvc.perform(setStatus("{\"status\":\"REVIEWED\"}").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));
    }

    @Test
    void setStatus_withMissingOrUnknownStatus_returns400() throws Exception {
        for (String body : List.of("{}", "{\"status\":\"CLOSED\"}")) {
            mockMvc.perform(setStatus(body).header(HttpHeaders.AUTHORIZATION, admin()))
                    .andExpect(status().isBadRequest());
        }
        verify(complaintService, never()).setStatus(any(), any(), eq(1L));
    }

    @Test
    void adminEndpoints_asInstructorOrStudent_return403_andWithoutToken_return401() throws Exception {
        for (String token : List.of(bearer(7L, "INSTRUCTOR"), bearer(42L, "STUDENT"))) {
            mockMvc.perform(get("/api/admin/complaints").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(setStatus("{\"status\":\"REVIEWED\"}").header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/complaints")).andExpect(status().isUnauthorized());
        mockMvc.perform(setStatus("{\"status\":\"REVIEWED\"}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(complaintService);
    }

    private static MockHttpServletRequestBuilder create(String body) {
        return post("/api/complaints").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder setStatus(String body) {
        return put("/api/admin/complaints/3/status").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static ComplaintResponse response(ComplaintStatus status) {
        return new ComplaintResponse(3L, new UserSummary(42L, "Ayşe Yılmaz"), 5L, null, "Kurs içeriği yanıltıcı.",
                status, Instant.parse("2026-11-19T10:00:00Z"),
                status == ComplaintStatus.OPEN ? null : Instant.parse("2026-11-19T11:00:00Z"));
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String admin() {
        return bearer(1L, "ADMIN");
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
