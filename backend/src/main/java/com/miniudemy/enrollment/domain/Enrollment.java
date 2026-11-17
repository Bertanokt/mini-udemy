package com.miniudemy.enrollment.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bir kursiyerin bir kursa kaydı.
 *
 * Tablo: `enrollments` (migration `V202611131600__create_enrollments.sql`). Kursiyer ve kurs başka modüllerin
 * entity'leri olduğu için yalnız id olarak tutulur. (student_id, course_id) çifti veritabanında benzersizdir.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    /** JPA için. */
    protected Enrollment() {
    }

    /** Kayıt zamanı burada atanır; böylece kayıt anında yanıtta ve olayda veritabanına gidilmeden bilinir. */
    public Enrollment(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrolledAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
