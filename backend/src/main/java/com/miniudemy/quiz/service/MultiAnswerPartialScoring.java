package com.miniudemy.quiz.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.miniudemy.quiz.domain.ScoringType;

/**
 * Çoklu doğru, kısmi puan: puan = soru puanı × max(0, doğru seçilen − yanlış seçilen) / doğru şık sayısı.
 * Sonuç iki ondalığa (yarım yukarı) yuvarlanır. Her yanlış seçim bir doğru seçimin kazandırdığını geri alır;
 * puan 0'ın altına inmez.
 */
@Component
public class MultiAnswerPartialScoring implements ScoringStrategy {

    @Override
    public ScoringType type() {
        return ScoringType.MULTI_PARTIAL;
    }

    @Override
    public QuestionScore score(ScoringInput input) {
        long net = Math.max(0, input.correctSelectedCount() - input.wrongSelectedCount());
        BigDecimal earned = BigDecimal.valueOf(input.points())
                .multiply(BigDecimal.valueOf(net))
                .divide(BigDecimal.valueOf(input.correctOptionIds().size()), 2, RoundingMode.HALF_UP);
        return new QuestionScore(earned, input.points());
    }
}
