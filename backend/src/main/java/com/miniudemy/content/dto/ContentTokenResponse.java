package com.miniudemy.content.dto;

import java.time.Instant;

/**
 * Ders içeriği adresi için kısa ömürlü token (docs/api/openapi.yaml › ContentTokenResponse, #45, ADR-012). İçerik
 * {@code GET /api/lessons/{id}/content?token=…} ile açılır; süresi dolunca yenisi alınır.
 */
public record ContentTokenResponse(String token, Instant expiresAt) {
}
