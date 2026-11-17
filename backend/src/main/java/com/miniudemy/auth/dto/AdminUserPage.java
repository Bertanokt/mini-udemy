package com.miniudemy.auth.dto;

import java.util.List;

/** Sayfalı kullanıcı listesi (docs/api/openapi.yaml › AdminUserPage). {@code page} 0'dan başlar. */
public record AdminUserPage(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
