package com.miniudemy.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bir kaydın önceden hesaplanmış kurs yüzdesi. Tablo: `course_progress`; kayıt başına tek satır. Yazma
 * `CourseProgressRepository#upsertPercent` ile yapılır; son izlenen ders alanları henüz eşlenmedi.
 */
@Entity
@Table(name = "course_progress")
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private Long enrollmentId;

    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Son açılan ders (#119); ders silinirse veritabanı boşaltır (ON DELETE SET NULL). */
    @Column(name = "last_lesson_id")
    private Long lastLessonId;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    /** JPA için. */
    protected CourseProgress() {
    }

    public Long getId() {
        return id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public Long getLastLessonId() {
        return lastLessonId;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
