package com.miniudemy.progress.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bir kaydın (enrollment) tamamladığı ders. Tablo: `lesson_progress`; (enrollment_id, lesson_id) benzersizdir. Kayıt
 * ve ders başka modüllerin entity'leri olduğu için yalnız id olarak tutulur.
 */
@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, updatable = false)
    private Long enrollmentId;

    @Column(name = "lesson_id", nullable = false, updatable = false)
    private Long lessonId;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;

    /** JPA için. */
    protected LessonProgress() {
    }

    public LessonProgress(Long enrollmentId, Long lessonId, Instant completedAt) {
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
