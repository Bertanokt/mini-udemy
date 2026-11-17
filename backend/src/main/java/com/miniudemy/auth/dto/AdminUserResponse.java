package com.miniudemy.auth.dto;

import java.time.Instant;

import com.miniudemy.auth.domain.Role;

/** Yönetici görünümü (docs/api/openapi.yaml › AdminUserResponse); parola özeti yoktur. */
public record AdminUserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt) {
}
