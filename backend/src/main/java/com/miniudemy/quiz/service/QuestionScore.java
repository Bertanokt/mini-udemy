package com.miniudemy.quiz.service;

import java.math.BigDecimal;

/** Bir sorudan alınan puan ve sorunun en yüksek puanı. */
public record QuestionScore(BigDecimal earned, int max) {

    public static QuestionScore of(int earned, int max) {
        return new QuestionScore(BigDecimal.valueOf(earned), max);
    }
}
