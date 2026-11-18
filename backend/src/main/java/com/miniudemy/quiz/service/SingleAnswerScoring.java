package com.miniudemy.quiz.service;

import org.springframework.stereotype.Component;

import com.miniudemy.quiz.domain.ScoringType;

/** Tek doğru şık: yalnız doğru şık seçildiyse tam puan; fazla seçim ya da boş cevap 0. */
@Component
public class SingleAnswerScoring implements ScoringStrategy {

    @Override
    public ScoringType type() {
        return ScoringType.SINGLE_ANSWER;
    }

    @Override
    public QuestionScore score(ScoringInput input) {
        if (input.correctOptionIds().size() != 1) {
            throw new IllegalArgumentException("Tek doğru şıklı puanlamada soruda tam bir doğru şık olmalı.");
        }
        boolean correct = input.selectedOptionIds().containsAll(input.correctOptionIds()); // KIRMIZI-B3
        return QuestionScore.of(correct ? input.points() : 0, input.points());
    }
}
