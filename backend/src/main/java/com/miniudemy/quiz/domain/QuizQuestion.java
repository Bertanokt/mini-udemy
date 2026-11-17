package com.miniudemy.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Sınavın bir sorusu ve sınavdaki sırası. Tablo: `quiz_questions`; sınavın parçasıdır (Quiz içindeki
 * koleksiyon satırı), kendi kimliği yoktur.
 */
@Embeddable
public class QuizQuestion {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Sınavdaki sıra (0'dan başlar). */
    @Column(nullable = false)
    private int position;

    /** JPA için. */
    protected QuizQuestion() {
    }

    public QuizQuestion(Long questionId, int position) {
        this.questionId = questionId;
        this.position = position;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public int getPosition() {
        return position;
    }
}
