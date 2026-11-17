package com.miniudemy.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
