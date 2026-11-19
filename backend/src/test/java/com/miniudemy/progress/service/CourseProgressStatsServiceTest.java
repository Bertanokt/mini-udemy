package com.miniudemy.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.miniudemy.course.service.CourseProgressStats.ProgressTotals;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.CourseProgressRepository.ProgressTotalsRow;

class CourseProgressStatsServiceTest {

    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final CourseProgressRepository courseProgress = mock(CourseProgressRepository.class);
    private final CourseProgressStatsService service = new CourseProgressStatsService(enrollments, courseProgress);

    @Test
    void totals_sumsPercentsOfEnrollmentsInOneQuery() {
        given(enrollments.findEnrollmentIdsByCourse(5L)).willReturn(List.of(1L, 2L, 3L));
        ProgressTotalsRow row = mock(ProgressTotalsRow.class);
        given(row.getPercentSum()).willReturn(new BigDecimal("150.00"));
        given(row.getCompletedCount()).willReturn(1L);
        given(courseProgress.totalsByEnrollmentIds(anyCollection())).willReturn(row);

        ProgressTotals totals = service.totals(5L);

        assertThat(totals.percentSum()).isEqualByComparingTo("150");
        assertThat(totals.completedCount()).isEqualTo(1);
        verify(courseProgress).totalsByEnrollmentIds(List.of(1L, 2L, 3L));
    }

    @Test
    void totals_courseWithoutEnrollments_isZeroAndSkipsQuery() {
        given(enrollments.findEnrollmentIdsByCourse(5L)).willReturn(List.of());

        ProgressTotals totals = service.totals(5L);

        assertThat(totals.percentSum()).isEqualByComparingTo("0");
        assertThat(totals.completedCount()).isZero();
        verifyNoInteractions(courseProgress);
    }
}
