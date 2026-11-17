package com.miniudemy.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Bölüm sonu sınavı. Tablo: `quizzes` (migration `V202611131603__create_quizzes.sql`). Bölüm course
 * modülünün entity'si olduğu için yalnız `sectionId` taşınır. Soruları sınavın parçasıdır ve `position`
 * sırasıyla yüklenir.
 */
@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false, updatable = false)
    private Long sectionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_type", nullable = false, length = 30)
    private ScoringType scoringType;

    /** Geçme eşiği (yüzde, 0–100). */
    @Column(name = "pass_threshold", nullable = false)
    private int passThreshold;

    /** Sertifika için geçilmesi zorunlu mu. */
    @Column(nullable = false)
    private boolean required;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "quiz_questions", joinColumns = @JoinColumn(name = "quiz_id"))
    @OrderBy("position ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    /** JPA için. */
    protected Quiz() {
    }

    public Quiz(Long sectionId, String title, ScoringType scoringType, int passThreshold, boolean required) {
        this.sectionId = sectionId;
        this.title = title;
        this.scoringType = scoringType;
        this.passThreshold = passThreshold;
        this.required = required;
    }

    /** Başlık, eşik ve zorunluluk; denemesi olan sınavda da değişebilir. */
    public void updateSettings(String title, int passThreshold, boolean required) {
        this.title = title;
        this.passThreshold = passThreshold;
        this.required = required;
    }

    public void changeScoringType(ScoringType scoringType) {
        this.scoringType = scoringType;
    }

    /** Soru listesini verilen sırayla (position 0..n-1) tamamen değiştirir. */
    public void replaceQuestions(List<Long> questionIds) {
        questions.clear();
        IntStream.range(0, questionIds.size()).forEach(i -> questions.add(new QuizQuestion(questionIds.get(i), i)));
    }

    /** Soru id'leri sınavdaki sırayla. */
    public List<Long> getQuestionIds() {
        return questions.stream().map(QuizQuestion::getQuestionId).toList();
    }

    public Long getId() {
        return id;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getTitle() {
        return title;
    }

    public ScoringType getScoringType() {
        return scoringType;
    }

    public int getPassThreshold() {
        return passThreshold;
    }

    public boolean isRequired() {
        return required;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
