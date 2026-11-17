package com.miniudemy.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.miniudemy.review.dto.RatingSummary;

/** Ortalama ve dağılım hesabı. Gizli yorumlar sayıma hiç girmez (repository yalnız görünürleri sayar). */
class RatingMathTest {

    @Test
    void summarize_mixedRatings_computesAverageCountAndDistribution() {
        RatingSummary s = RatingMath.summarize(Map.of(1, 0L, 2, 1L, 3, 1L, 4, 3L, 5, 7L));

        assertThat(s.reviewCount()).isEqualTo(12);
        assertThat(s.averageRating()).isEqualTo(4.3); // 52 / 12 = 4.33
        assertThat(s.distribution()).containsExactly(
                Map.entry("1", 0L), Map.entry("2", 1L), Map.entry("3", 1L), Map.entry("4", 3L), Map.entry("5", 7L));
    }

    @Test
    void summarize_missingRatings_countAsZero() {
        RatingSummary s = RatingMath.summarize(Map.of(4, 2L));

        assertThat(s.averageRating()).isEqualTo(4.0);
        assertThat(s.distribution()).containsEntry("1", 0L).containsEntry("5", 0L).containsEntry("4", 2L);
    }

    @Test
    void summarize_empty_hasNullAverage() {
        RatingSummary s = RatingMath.summarize(Map.of());

        assertThat(s.averageRating()).isNull();
        assertThat(s.reviewCount()).isZero();
    }

    @Test
    void summarize_roundsHalfUpToOneDecimal() {
        // (5 + 4) / 2 = 4.5 ; (5*2 + 4) / 3 = 4.666 -> 4.7
        assertThat(RatingMath.summarize(Map.of(5, 1L, 4, 1L)).averageRating()).isEqualTo(4.5);
        assertThat(RatingMath.summarize(Map.of(5, 2L, 4, 1L)).averageRating()).isEqualTo(4.7);
    }
}
