package com.miniudemy.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.miniudemy.enrollment.repository.EnrollmentRepository;
import com.miniudemy.enrollment.repository.EnrollmentRepository.CourseCount;

class EnrollmentStatsServiceTest {

    private final EnrollmentRepository repository = mock(EnrollmentRepository.class);
    private final EnrollmentStatsService service = new EnrollmentStatsService(repository);

    @Test
    void countByCourse_returnsRepositoryCount() {
        given(repository.countByCourseId(5L)).willReturn(41L);

        assertThat(service.countByCourse(5L)).isEqualTo(41L);
    }

    @Test
    void countByCourses_includesCoursesWithoutEnrollmentsAsZero() {
        CourseCount first = row(1L, 30L);
        CourseCount third = row(3L, 2L);
        given(repository.countByCourseIds(List.of(1L, 2L, 3L))).willReturn(List.of(first, third));

        assertThat(service.countByCourses(List.of(1L, 2L, 3L))).isEqualTo(Map.of(1L, 30L, 2L, 0L, 3L, 2L));
    }

    @Test
    void countByCourses_emptyInput_skipsQuery() {
        assertThat(service.countByCourses(List.of())).isEmpty();

        verifyNoInteractions(repository);
    }

    private static CourseCount row(long courseId, long total) {
        CourseCount row = mock(CourseCount.class);
        given(row.getCourseId()).willReturn(courseId);
        given(row.getTotal()).willReturn(total);
        return row;
    }
}
