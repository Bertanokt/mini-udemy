package com.miniudemy.common.security;

import java.time.Instant;

/** Verilen içerik token'ı ve son geçerlilik anı (ADR-012). */
public record IssuedContentToken(String token, Instant expiresAt) {
}
