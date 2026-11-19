package com.miniudemy.admin.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.miniudemy.common.exception.BadRequestException;

/**
 * Bir kullanıcının bir kursu ya da yorumu gerekçeyle şikâyet etmesi (#144, FR-41).
 *
 * Tablo: `complaints` (migration `V202611192319__create_complaints.sql`). Kullanıcı, kurs ve yorum başka modüllerin
 * entity'leri olduğu için yalnız id olarak tutulur. Hedef tam olarak biridir (kurs ya da yorum); kural hem burada hem
 * tablodaki CHECK kısıtındadır.
 */
@Entity
@Table(name = "complaints")
public class Complaint {

    static final String SINGLE_TARGET = "Bir kurs ya da bir yorum şikâyet edilebilir; ikisi birden ya da hiçbiri olmaz.";
    static final String RESOLVE_ONLY = "Şikâyet yalnız incelendi ya da reddedildi olarak işaretlenebilir.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private Long reporterId;

    @Column(name = "course_id", updatable = false)
    private Long courseId;

    @Column(name = "review_id", updatable = false)
    private Long reviewId;

    @Column(name = "resolved_by_id")
    private Long resolvedById;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** JPA için. */
    protected Complaint() {
    }

    /** Açık şikâyet. {@code courseId} ile {@code reviewId}'den tam biri dolu olmalı, yoksa 400. */
    public Complaint(Long reporterId, Long courseId, Long reviewId, String reason) {
        if ((courseId == null) == (reviewId == null)) {
            throw new BadRequestException(SINGLE_TARGET);
        }
        this.reporterId = reporterId;
        this.courseId = courseId;
        this.reviewId = reviewId;
        this.reason = reason;
        this.status = ComplaintStatus.OPEN;
        this.createdAt = Instant.now();
    }

    /**
     * Yönetici şikâyeti sonuçlandırır; çözen yönetici ve zaman yazılır. Sonuçlanmış şikâyetin sonucu değiştirilebilir
     * (ör. İncelendi → Reddedildi), her seferinde çözen ve zaman güncellenir. Şikâyet yeniden açılamaz: OPEN → 400.
     */
    public void resolve(ComplaintStatus result, Long adminId) {
        if (result == ComplaintStatus.OPEN) {
            throw BadRequestException.forField("status", RESOLVE_ONLY);
        }
        this.status = result;
        this.resolvedById = adminId;
        this.resolvedAt = Instant.now();
    }

    public boolean isAboutCourse() {
        return courseId != null;
    }

    public Long getId() {
        return id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Long getResolvedById() {
        return resolvedById;
    }

    public String getReason() {
        return reason;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
