package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.miniudemy.quiz.domain.ScoringType;

class ScoringStrategyResolverTest {

    private final SingleAnswerScoring single = new SingleAnswerScoring();
    private final MultiAnswerAllOrNothing allOrNothing = new MultiAnswerAllOrNothing();
    private final MultiAnswerPartialScoring partial = new MultiAnswerPartialScoring();

    @Test
    void resolve_returnsMatchingStrategyForEveryScoringType() {
        ScoringStrategyResolver resolver = new ScoringStrategyResolver(List.of(partial, single, allOrNothing));

        assertThat(resolver.resolve(ScoringType.SINGLE_ANSWER)).isSameAs(single);
        assertThat(resolver.resolve(ScoringType.MULTI_ALL_OR_NOTHING)).isSameAs(allOrNothing);
        assertThat(resolver.resolve(ScoringType.MULTI_PARTIAL)).isSameAs(partial);
    }

    @Test
    void construct_withMissingStrategy_failsAtStartup() {
        assertThatThrownBy(() -> new ScoringStrategyResolver(List.of(single, allOrNothing)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MULTI_PARTIAL");
    }

    @Test
    void construct_withDuplicateStrategy_failsAtStartup() {
        assertThatThrownBy(() -> new ScoringStrategyResolver(List.of(single, new SingleAnswerScoring(), allOrNothing, partial)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SINGLE_ANSWER");
    }
}
