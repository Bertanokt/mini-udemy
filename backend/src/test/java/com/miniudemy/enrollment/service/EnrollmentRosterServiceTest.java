package com.miniudemy.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.dto.RosterEntry;
import com.miniudemy.course.service.CourseRoster.RosterPage;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

class EnrollmentRosterServiceTest {

    private final EnrollmentRepository repository = mock(EnrollmentRepository.class);
    private final EnrollmentProgress progress = mock(EnrollmentProgress.class);
    private final EnrollmentRosterService service = new EnrollmentRosterService(repository, progress);

    @Test
    void page_returnsEntriesWithProgress_andAsksNewestFirst() {
        Enrollment first = enrollment(11L, 100L, "2026-11-18T10:00:00Z");
        Enrollment second = enrollment(12L, 101L, "2026-11-17T10:00:00Z");
        given(repository.findByCourseId(any(), any())).willReturn(
                new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 5));
        Instant accessed = Instant.parse("2026-11-18T11:00:00Z");
        given(progress.snapshots(anyCollection())).willReturn(
                Map.of(11L, new EnrollmentProgressSnapshot(75.0, 3L, accessed)));

        RosterPage page = service.page(5L, 0, 2);

        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.entries()).containsExactly(
                new RosterEntry(11L, 100L, first.getEnrolledAt(), 75.0, accessed),
                // İlerleme satırı olmayan kayıt: yüzde 0, son erişim yok.
                new RosterEntry(12L, 101L, second.getEnrolledAt(), 0.0, null));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByCourseId(org.mockito.ArgumentMatchers.eq(5L), pageable.capture());
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("enrolledAt: DESC,id: DESC");
        verify(progress).snapshots(List.of(11L, 12L));
    }

    @Test
    void page_courseWithoutEnrollments_returnsEmptyAndSkipsProgressQuery() {
        given(repository.findByCourseId(any(), any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        RosterPage page = service.page(5L, 0, 20);

        assertThat(page.entries()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
        verifyNoInteractions(progress);
    }

    /** #123: kayıtlı kursiyer yayından kaldırılan kursu görmeye devam eder; parametre sırası karışmamalı. */
    @Test
    void isEnrolled_asksForThisStudentInThisCourse() {
        given(repository.existsByStudentIdAndCourseId(42L, 5L)).willReturn(true);

        assertThat(service.isEnrolled(5L, 42L)).isTrue();
        assertThat(service.isEnrolled(42L, 5L)).isFalse();
    }

    private static Enrollment enrollment(long id, long studentId, String enrolledAt) {
        Enrollment enrollment = new Enrollment(studentId, 5L);
        ReflectionTestUtils.setField(enrollment, "id", id);
        ReflectionTestUtils.setField(enrollment, "enrolledAt", Instant.parse(enrolledAt));
        return enrollment;
    }
}
