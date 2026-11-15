package com.miniudemy.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Platform kullanıcısı (kursiyer, eğitmen ya da yönetici).
 *
 * Tablo: `users` (migration `V202611131442__create_users.sql`, ER diyagramı › auth). Şema yalnız Flyway ile
 * değişir; bu sınıf `ddl-auto=validate` ile tabloya karşı doğrulanır, bu yüzden kolon adları ve tipleri
 * migration'la birebir aynı tutulur.
 *
 * Entity API'ye çıkmaz; dışarıya `UserResponse` DTO'su ile verilir (#9). `passwordHash` hiçbir DTO'ya
 * taşınmaz.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Veritabanında `lower(email)` üzerinde tekil index var; aynı adres büyük/küçük harf farkıyla ikinci kez
     * kaydedilemez. Kayıt sırasında küçük harfe çevrilerek saklanır (#12).
     */
    @Column(nullable = false, length = 254)
    private String email;

    /** BCrypt özeti; düz parola hiçbir yerde saklanmaz (NFR-04). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(columnDefinition = "text")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Veritabanı güncellemede bu kolonu kendiliğinden değiştirmez; Hibernate her güncellemede yazar. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA için. */
    protected User() {
    }

    public User(String email, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBio() {
        return bio;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
