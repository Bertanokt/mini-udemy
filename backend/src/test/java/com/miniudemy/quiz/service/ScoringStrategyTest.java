package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ScoringStrategyTest {

    private final SingleAnswerScoring single = new SingleAnswerScoring();
    private final MultiAnswerAllOrNothing allOrNothing = new MultiAnswerAllOrNothing();
    private final MultiAnswerPartialScoring partial = new MultiAnswerPartialScoring();

    private static ScoringInput in(int points, Set<Long> correct, Set<Long> selected) {
        return new ScoringInput(points, correct, selected);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    // SingleAnswerScoring

    @Test
    void single_correctOption_givesFullPoints() {
        QuestionScore s = single.score(in(10, Set.of(1L), Set.of(1L)));
        assertThat(s.earned()).isEqualByComparingTo(bd("10"));
        assertThat(s.max()).isEqualTo(10);
    }

    @Test
    void single_wrongOption_givesZero() {
        assertThat(single.score(in(10, Set.of(1L), Set.of(2L))).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void single_emptyAnswer_givesZero() {
        assertThat(single.score(in(10, Set.of(1L), Set.of())).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void single_extraSelection_givesZero() {
        assertThat(single.score(in(10, Set.of(1L), Set.of(1L, 2L))).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void single_questionWithTwoCorrectOptions_isRejected() {
        assertThatThrownBy(() -> single.score(in(10, Set.of(1L, 2L), Set.of(1L))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // MultiAnswerAllOrNothing

    @Test
    void allOrNothing_allCorrectOnly_givesFullPoints() {
        assertThat(allOrNothing.score(in(6, Set.of(1L, 2L), Set.of(1L, 2L))).earned()).isEqualByComparingTo(bd("6"));
    }

    @Test
    void allOrNothing_missingOneCorrect_givesZero() {
        assertThat(allOrNothing.score(in(6, Set.of(1L, 2L), Set.of(1L))).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void allOrNothing_allCorrectPlusExtra_givesZero() {
        assertThat(allOrNothing.score(in(6, Set.of(1L, 2L), Set.of(1L, 2L, 3L))).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void allOrNothing_emptyAnswer_givesZero() {
        assertThat(allOrNothing.score(in(6, Set.of(1L, 2L), Set.of())).earned()).isEqualByComparingTo(bd("0"));
    }

    // MultiAnswerPartialScoring

    @Test
    void partial_allCorrect_givesFullPoints() {
        assertThat(partial.score(in(6, Set.of(1L, 2L, 3L), Set.of(1L, 2L, 3L))).earned()).isEqualByComparingTo(bd("6"));
    }

    @Test
    void partial_oneOfThreeCorrect_givesProportionalPoints() {
        assertThat(partial.score(in(6, Set.of(1L, 2L, 3L), Set.of(1L))).earned()).isEqualByComparingTo(bd("2"));
    }

    @Test
    void partial_roundsToTwoDecimals() {
        assertThat(partial.score(in(10, Set.of(1L, 2L, 3L), Set.of(1L))).earned()).isEqualByComparingTo(bd("3.33"));
    }

    @Test
    void partial_wrongSelectionReducesScore() {
        // 2 doğru, 1 yanlış, 3 doğrudan: (2-1)/3 * 6 = 2
        assertThat(partial.score(in(6, Set.of(1L, 2L, 3L), Set.of(1L, 2L, 9L))).earned()).isEqualByComparingTo(bd("2"));
    }

    @Test
    void partial_moreWrongThanCorrect_isFlooredAtZero() {
        assertThat(partial.score(in(6, Set.of(1L, 2L), Set.of(1L, 8L, 9L))).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void partial_emptyAnswer_givesZero() {
        assertThat(partial.score(in(6, Set.of(1L, 2L), Set.of())).earned()).isEqualByComparingTo(bd("0"));
    }

    @Test
    void partial_allCorrectPlusExtra_isReducedNotFull() {
        // 2 doğru, 1 yanlış, 2 doğrudan: (2-1)/2 * 6 = 3
        assertThat(partial.score(in(6, Set.of(1L, 2L), Set.of(1L, 2L, 9L))).earned()).isEqualByComparingTo(bd("3"));
    }

    // ScoringInput

    @Test
    void input_withoutCorrectOption_isRejected() {
        assertThatThrownBy(() -> in(5, Set.of(), Set.of(1L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void input_nonPositivePoints_isRejected() {
        assertThatThrownBy(() -> in(0, Set.of(1L), Set.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
