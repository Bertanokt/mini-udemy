package com.miniudemy.quiz.domain;

import java.math.BigDecimal;

/** Bir sorunun puanlanmış sonucu: alınan ve en yüksek puan. */
public record GradedQuestion(Long questionId, BigDecimal earnedPoints, int maxPoints) {

    /** Soru tam puan aldıysa doğru sayılır. */
    public boolean isCorrect() {
        return earnedPoints.compareTo(BigDecimal.valueOf(maxPoints)) == 0;
    }
}
