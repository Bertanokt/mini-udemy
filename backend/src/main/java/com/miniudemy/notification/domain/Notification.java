package com.miniudemy.notification.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bir kullanıcıya uygulama içi bildirim (#124, FR-43). Tablo: `notifications`. Alıcı başka modülün kullanıcısı olduğu
 * için yalnız id olarak tutulur. `readAt` boşsa okunmamıştır; okundu işaretleme {@link #markRead} (#125).
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, updatable = false, length = 200)
    private String title;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String body;

    @Column(updatable = false, length = 255)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA için. */
    protected Notification() {
    }

    public Notification(Long userId, NotificationContent content, Instant createdAt) {
        this.userId = userId;
        this.type = content.type();
        this.title = content.title();
        this.body = content.body();
        this.link = content.link();
        this.createdAt = createdAt;
    }

    /** Okunmamışsa okundu işaretler; zaten okunmuşsa ilk okunma zamanı korunur (idempotent, #125). */
    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLink() {
        return link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
