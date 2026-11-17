package com.miniudemy.quiz.service;

import org.springframework.stereotype.Component;

import com.miniudemy.quiz.domain.ScoringType;

/** Çoklu doğru, tam eşleşme: seçilenler doğru şıklarla birebir aynıysa tam puan, aksi halde 0. */
@Component
public class MultiAnswerAllOrNothing implements ScoringStrategy {

    @Override
    public ScoringType type() {
        return ScoringType.MULTI_ALL_OR_NOTHING;
    }

    @Override
    public QuestionScore score(ScoringInput input) {
        boolean exact = input.selectedOptionIds().equals(input.correctOptionIds());
        return QuestionScore.of(exact ? input.points() : 0, input.points());
    }
}
