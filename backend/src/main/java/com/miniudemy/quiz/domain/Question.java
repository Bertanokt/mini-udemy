package com.miniudemy.quiz.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Kursun soru bankasındaki bir soru. Tablo: `questions`. Kurs başka modülün entity'si olduğu için yalnız
 * `courseId` taşınır. Şıkları sorunun parçasıdır: soru silinince ya da şıklar değişince birlikte gider.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false, updatable = false)
    private Long courseId;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(nullable = false)
    private int points;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "question_id", nullable = false)
    @OrderBy("id")
    private List<Option> options = new ArrayList<>();

    /** JPA için. */
    protected Question() {
    }

    public Question(Long courseId, String text, int points) {
        this.courseId = courseId;
        this.text = text;
        this.points = points;
    }

    public void update(String text, int points) {
        this.text = text;
        this.points = points;
    }

    /** Şıkları verilen listeyle tamamen değiştirir. */
    public void replaceOptions(List<Option> newOptions) {
        options.clear();
        options.addAll(newOptions);
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getText() {
        return text;
    }

    public int getPoints() {
        return points;
    }

    public List<Option> getOptions() {
        return options;
    }
}
