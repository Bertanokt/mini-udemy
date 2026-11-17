package com.miniudemy.auth.dto;

/** Başka bir kullanıcının herkese gösterilebilen bilgisi, e-posta yok (docs/api/openapi.yaml › UserSummary). */
public record UserSummary(Long id, String fullName) {
}
