package com.miniudemy.notification.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.notification.domain.NotificationType;
import com.miniudemy.notification.dto.NotificationPage;
import com.miniudemy.notification.dto.NotificationResponse;
import com.miniudemy.notification.dto.UnreadCountResponse;
import com.miniudemy.notification.service.NotificationService;

/**
 * Bildirim uçları (#125) gerçek güvenlik ayarıyla; servis taklit. Her rol kendi bildirimlerine erişir, kullanıcı
 * token'dan gelir; token yoksa 401, sayfa boyutu sınır dışıysa servise ulaşmadan 400, başkasının bildirimi 404.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void users() {
        user(1L, "Ali Yönetici", Role.ADMIN);
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
    }

    @Test
    void list_asAnyRole_returnsOwnPage_withDefaultPaging() throws Exception {
        for (long userId : List.of(42L, 7L, 1L)) {
            given(notificationService.list(userId, false, 0, 20))
                    .willReturn(new NotificationPage(List.of(response()), 0, 20, 1, 1));

            mockMvc.perform(get("/api/me/notifications").header(HttpHeaders.AUTHORIZATION, bearer(userId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(9))
                    .andExpect(jsonPath("$.content[0].type").value("COURSE_APPROVED"))
                    .andExpect(jsonPath("$.content[0].link").value("/courses/3"))
                    .andExpect(jsonPath("$.content[0].read").value(false))
                    .andExpect(jsonPath("$.content[0].createdAt").value("2026-11-21T09:00:00Z"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Test
    void list_passesUnreadOnlyAndPaging() throws Exception {
        given(notificationService.list(42L, true, 2, 5)).willReturn(new NotificationPage(List.of(), 2, 5, 10, 2));

        mockMvc.perform(get("/api/me/notifications?unreadOnly=true&page=2&size=5")
                        .header(HttpHeaders.AUTHORIZATION, bearer(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void list_withOutOfRangePaging_returns400() throws Exception {
        for (String query : List.of("size=0", "size=101", "page=-1")) {
            mockMvc.perform(get("/api/me/notifications?" + query).header(HttpHeaders.AUTHORIZATION, bearer(42L)))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(notificationService);
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        given(notificationService.countUnread(42L)).willReturn(new UnreadCountResponse(3));

        mockMvc.perform(get("/api/me/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, bearer(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void markRead_ownNotification_returns204() throws Exception {
        mockMvc.perform(post("/api/me/notifications/9/read").header(HttpHeaders.AUTHORIZATION, bearer(42L)))
                .andExpect(status().isNoContent());
        verify(notificationService).markRead(42L, 9L);
    }

    @Test
    void markRead_othersNotification_returns404WithMessage() throws Exception {
        willThrow(new NotFoundException("Bildirim bulunamadı.")).given(notificationService).markRead(7L, 9L);

        mockMvc.perform(post("/api/me/notifications/9/read").header(HttpHeaders.AUTHORIZATION, bearer(7L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Bildirim bulunamadı."));
    }

    @Test
    void markAllRead_returns204() throws Exception {
        mockMvc.perform(post("/api/me/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer(42L)))
                .andExpect(status().isNoContent());
        verify(notificationService).markAllRead(42L);
    }

    @Test
    void allEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/api/me/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/notifications/9/read")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/notifications/read-all")).andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    private static NotificationResponse response() {
        return new NotificationResponse(9L, NotificationType.COURSE_APPROVED, "Kursunuz onaylandı",
                "\"Java'ya giriş\" kursunuz onaylandı ve katalogda yayımlandı.", "/courses/3", false,
                Instant.parse("2026-11-21T09:00:00Z"));
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String bearer(long userId) {
        String role = switch ((int) userId) {
            case 1 -> "ADMIN";
            case 7 -> "INSTRUCTOR";
            default -> "STUDENT";
        };
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
