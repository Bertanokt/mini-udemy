package com.miniudemy.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.dto.AdminUserPage;
import com.miniudemy.auth.dto.AdminUserResponse;
import com.miniudemy.auth.dto.UserStatusRequest;
import com.miniudemy.auth.service.AdminUserService;
import com.miniudemy.common.security.CurrentUser;

/** Yönetici kullanıcı uçları (docs/api/openapi.yaml › Admin). {@code /api/admin/**} yalnız ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserPage adminListUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @Size(max = 100, message = "En fazla 100 karakter olabilir.") String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return adminUserService.list(role, active, q, page, size);
    }

    @PutMapping("/{id}/status")
    public AdminUserResponse adminSetUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request,
            Authentication authentication) {
        return adminUserService.setStatus(id, request.active(), CurrentUser.id(authentication));
    }
}
