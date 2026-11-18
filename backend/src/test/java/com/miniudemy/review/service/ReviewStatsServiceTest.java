package com.miniudemy.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.miniudemy.course.service.CourseRating;
import com.miniudemy.review.repository.ReviewRepository;

class ReviewStatsServiceTest {

    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final ReviewStatsService service = new ReviewStatsService(repository);

    private static ReviewRepository.CourseRatingRow row(long courseId, double average, long total) {
        return new ReviewRepository.CourseRatingRow() {
            @Override
            public Long getCourseId() {
                return courseId;
            }

            @Override
            public Double getAverage() {
                return average;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void ratingsByCourses_singleQuery_fillsMissingCoursesWithEmptyRating() {
        given(repository.aggregateVisibleByCourses(anyCollection())).willReturn(List.of(row(5L, 4.333333, 3)));

        Map<Long, CourseRating> ratings = service.ratingsByCourses(List.of(5L, 6L));

        assertThat(ratings.get(5L)).isEqualTo(new CourseRating(4.3, 3));
        assertThat(ratings.get(6L)).isEqualTo(new CourseRating(null, 0));
        verify(repository).aggregateVisibleByCourses(anyCollection());
    }

    @Test
    void ratingsByCourses_emptyInput_doesNotQuery() {
        assertThat(service.ratingsByCourses(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void ratingOf_courseWithoutReviews_returnsNullAndZero() {
        given(repository.aggregateVisibleByCourses(anyCollection())).willReturn(List.of());

        assertThat(service.ratingOf(9L)).isEqualTo(new CourseRating(null, 0));
    }

    @Test
    void ratingOf_courseWithReviews_returnsRoundedAverageAndCount() {
        given(repository.aggregateVisibleByCourses(anyCollection())).willReturn(List.of(row(9L, 4.25, 4)));

        assertThat(service.ratingOf(9L)).isEqualTo(new CourseRating(4.3, 4));
    }

    @Test
    void courseIdsWithAverageAtLeast_comparesRoundedAverageAndSkipsCoursesWithoutReviews() {
        given(repository.aggregateAllVisible()).willReturn(List.of(
                row(1L, 4.46, 10), row(2L, 4.44, 10), row(3L, 5.0, 1), row(4L, 3.0, 2)));

        // 4.46 kartta 4.5 görünür, bu yüzden 4.5 filtresine girer; 4.44 (4.4) girmez.
        assertThat(service.courseIdsWithAverageAtLeast(4.5)).containsExactlyInAnyOrder(1L, 3L);
        assertThat(service.courseIdsWithAverageAtLeast(1)).isEqualTo(Set.of(1L, 2L, 3L, 4L));
    }

    @Test
    void courseIdsWithAverageAtLeast_noReviewsAnywhere_returnsEmptySet() {
        given(repository.aggregateAllVisible()).willReturn(List.of());

        assertThat(service.courseIdsWithAverageAtLeast(1)).isEmpty();
    }

    @Test
    void distributionOf_fillsMissingStarsWithZero() {
        ReviewRepository.RatingCount five = mock(ReviewRepository.RatingCount.class);
        given(five.getRating()).willReturn(5);
        given(five.getTotal()).willReturn(7L);
        ReviewRepository.RatingCount two = mock(ReviewRepository.RatingCount.class);
        given(two.getRating()).willReturn(2);
        given(two.getTotal()).willReturn(1L);
        given(repository.countVisibleByRating(9L)).willReturn(List.of(five, two));

        assertThat(service.distributionOf(9L))
                .containsExactly(Map.entry("1", 0L), Map.entry("2", 1L), Map.entry("3", 0L), Map.entry("4", 0L),
                        Map.entry("5", 7L));
    }

    @Test
    void distributionOf_courseWithoutReviews_isAllZero() {
        given(repository.countVisibleByRating(9L)).willReturn(List.of());

        assertThat(service.distributionOf(9L).values()).containsOnly(0L).hasSize(5);
    }
}
