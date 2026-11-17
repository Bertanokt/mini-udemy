package com.miniudemy.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Denemede bir soruya verilen bir şık cevabı. Tablo: `attempt_answers`; denemenin parçasıdır, kendi kimliği yoktur. */
@Embeddable
public class AttemptAnswer {

    @Column(name = "question_id", nullable = false, updatable = false)
    private Long questionId;

    @Column(name = "option_id", nullable = false, updatable = false)
    private Long optionId;

    /** JPA için. */
    protected AttemptAnswer() {
    }

    public AttemptAnswer(Long questionId, Long optionId) {
        this.questionId = questionId;
        this.optionId = optionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public Long getOptionId() {
        return optionId;
    }
}
