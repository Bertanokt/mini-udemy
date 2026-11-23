package com.miniudemy.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.miniudemy.course.service.CourseProgressStats.ProgressDistribution;
import com.miniudemy.course.service.CourseProgressStats.ProgressTotals;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.CourseProgressRepository.ProgressBucketsRow;
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

    @Test
    void distribution_enrollmentsWithoutProgressRowGoToZeroBucket() {
        List<Long> ids = LongStream.rangeClosed(1, 25).boxed().toList();
        given(enrollments.findEnrollmentIdsByCourse(5L)).willReturn(ids);
        // 25 kayıttan 15'inin satırı var: 3 + 4 + 2 + 3 + 3; 10 kaydın satırı yok (yüzde 0 sayılır).
        given(courseProgress.bucketsByEnrollmentIds(anyCollection())).willReturn(buckets(3, 4, 2, 3, 3));

        ProgressDistribution d = service.distribution(5L);

        assertThat(d).isEqualTo(new ProgressDistribution(13, 4, 2, 3, 3));
        assertThat(d.below25() + d.from25() + d.from50() + d.from75() + d.completed()).isEqualTo(25);
        verify(courseProgress).bucketsByEnrollmentIds(ids);
    }

    @Test
    void distribution_allEnrollmentsWithoutProgressRow_areAllInZeroBucket() {
        given(enrollments.findEnrollmentIdsByCourse(5L)).willReturn(List.of(1L, 2L, 3L));
        given(courseProgress.bucketsByEnrollmentIds(anyCollection())).willReturn(buckets(0, 0, 0, 0, 0));

        assertThat(service.distribution(5L)).isEqualTo(new ProgressDistribution(3, 0, 0, 0, 0));
    }

    @Test
    void distribution_courseWithoutEnrollments_isZeroAndSkipsQuery() {
        given(enrollments.findEnrollmentIdsByCourse(5L)).willReturn(List.of());

        assertThat(service.distribution(5L)).isEqualTo(new ProgressDistribution(0, 0, 0, 0, 0));
        verifyNoInteractions(courseProgress);
    }

    private static ProgressBucketsRow buckets(long below25, long from25, long from50, long from75, long completed) {
        return new ProgressBucketsRow() {
            @Override
            public long getBelow25() {
                return below25;
            }

            @Override
            public long getFrom25() {
                return from25;
            }

            @Override
            public long getFrom50() {
                return from50;
            }

            @Override
            public long getFrom75() {
                return from75;
            }

            @Override
            public long getCompleted() {
                return completed;
            }
        };
    }
}
