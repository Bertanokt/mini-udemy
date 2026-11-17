package com.miniudemy.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Çoktan seçmeli sorunun şıkkı. Tablo: `options` (migration `V202611131603__create_quizzes.sql`). */
@Entity
@Table(name = "options")
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    /** JPA için. */
    protected Option() {
    }

    public Option(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
