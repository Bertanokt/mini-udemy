package com.miniudemy.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Parola yenileme token'ı (#115, ER › password_reset_tokens). Token'ın kendisi değil, SHA-256 özeti saklanır.
 *
 * Kurallar: süresi ({@code expiresAt}) geçmemiş ve henüz kullanılmamış ({@code usedAt} boş) token kullanılabilir;
 * kullanılınca bir daha kullanılamaz. Kullanıcı bu modülün kendi entity'si olduğu için id ile tutulur.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA için. */
    protected PasswordResetToken() {
    }

    public PasswordResetToken(Long userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Süresi geçmemiş ve kullanılmamışsa {@code true}. Tam {@code expiresAt} anında artık geçersizdir. */
    public boolean isUsableAt(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
