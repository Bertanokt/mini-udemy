package com.miniudemy.notification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.notification.dto.NotificationPage;
import com.miniudemy.notification.dto.UnreadCountResponse;
import com.miniudemy.notification.service.NotificationService;

/**
 * Kullanıcının kendi bildirimleri (#125, docs/api/openapi.yaml › Notifications). Giriş yapmış her rol kullanır;
 * kullanıcı token'dan alınır, istekte başka kullanıcı id'si yoktur.
 */
@RestController
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPage listMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size,
            Authentication authentication) {
        return notificationService.list(CurrentUser.id(authentication), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse countMyUnreadNotifications(Authentication authentication) {
        return notificationService.countUnread(CurrentUser.id(authentication));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markNotificationRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead(CurrentUser.id(authentication), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllNotificationsRead(Authentication authentication) {
        notificationService.markAllRead(CurrentUser.id(authentication));
    }
}
