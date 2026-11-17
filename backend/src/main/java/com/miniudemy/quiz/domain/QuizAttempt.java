package com.miniudemy.quiz.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Sınav denemesi. Tablo: `quiz_attempts` (migration `V202611131604__create_quiz_attempts.sql`). Bu hâliyle yalnız
 * okuma içindir; verilen cevaplar (`attempt_answers`) ilişkisini deneme yazan uç ekler.
 */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false, updatable = false)
    private Long quizId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(nullable = false, precision = 7, scale = 2, updatable = false)
    private BigDecimal score;

    @Column(name = "max_score", nullable = false, precision = 7, scale = 2, updatable = false)
    private BigDecimal maxScore;

    @Column(nullable = false, updatable = false)
    private boolean passed;

    @Column(name = "submitted_at", nullable = false, insertable = false, updatable = false)
    private Instant submittedAt;

    protected QuizAttempt() {
    }

    public QuizAttempt(Long quizId, Long studentId, BigDecimal score, BigDecimal maxScore, boolean passed) {
        this.quizId = quizId;
        this.studentId = studentId;
        this.score = score;
        this.maxScore = maxScore;
        this.passed = passed;
    }

    public Long getId() {
        return id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    /** Başarı yüzdesi, bir ondalığa yuvarlanmış; en yüksek puan 0 ise 0. */
    public BigDecimal getPercent() {
        if (maxScore.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 1, RoundingMode.HALF_UP);
    }
}
