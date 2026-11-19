package com.miniudemy.review.domain;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.miniudemy.common.exception.BadRequestException;

/**
 * Bir kullanıcının bir kursa puanı ve yorumu.
 *
 * Tablo: `reviews` (migration `V202611171801__create_reviews.sql`). Kullanıcı ve kurs başka modüllerin entity'leri
 * olduğu için yalnız id olarak tutulur. (user_id, course_id) çifti benzersizdir; puan 1–5 aralığındadır.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    /** Kolon smallint (ER); Java tarafında int kalır. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "moderation_reason", columnDefinition = "text")
    private String moderationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA için. */
    protected Review() {
    }

    public Review(Long userId, Long courseId, int rating, String comment) {
        this.userId = userId;
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Sahibinin düzenlemesi: puan ve yorum değişir, gizlilik durumu ve moderasyon gerekçesi korunur. */
    public void edit(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    /**
     * Yönetici moderasyonu (#142): yorum herkese açık listeden ve ortalamadan çıkar, gerekçe saklanır. Zaten gizliyse
     * gerekçe güncellenir. Gerekçe boşsa 400 ({@code errors.reason}). {@code updatedAt} yazarın düzenleme zamanıdır,
     * moderasyonla değişmez.
     */
    public void hide(String reason) {
        if (reason == null || reason.isBlank()) {
            throw BadRequestException.forField("reason", "Gizleme gerekçesi zorunludur.");
        }
        this.hidden = true;
        this.moderationReason = reason.strip();
    }

    /** Yorumu geri açar; moderasyon geçmişi tutulmadığı için gerekçe de silinir (#142 ince kapsam). */
    public void unhide() {
        this.hidden = false;
        this.moderationReason = null;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
