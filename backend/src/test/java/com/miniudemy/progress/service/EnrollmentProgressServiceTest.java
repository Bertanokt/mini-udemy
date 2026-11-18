package com.miniudemy.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;
import com.miniudemy.progress.domain.CourseProgress;
import com.miniudemy.progress.repository.CourseProgressRepository;

/** Kayıtlı kurslar listesine ilerleme özeti (#119): course_progress satırları tek sorguda, kayıt id'sine göre. */
class EnrollmentProgressServiceTest {

    private final CourseProgressRepository repository = mock(CourseProgressRepository.class);
    private final EnrollmentProgressService service = new EnrollmentProgressService(repository);

    @Test
    void snapshots_mapsRowsByEnrollmentId() {
        Instant opened = Instant.parse("2026-11-18T09:00:00Z");
        CourseProgress row = mock(CourseProgress.class);
        given(row.getEnrollmentId()).willReturn(1L);
        given(row.getProgressPercent()).willReturn(new BigDecimal("66.67"));
        given(row.getLastLessonId()).willReturn(12L);
        given(row.getLastAccessedAt()).willReturn(opened);
        given(repository.findAllByEnrollmentIdIn(List.of(1L, 2L))).willReturn(List.of(row));

        assertThat(service.snapshots(List.of(1L, 2L)))
                .containsExactlyEntriesOf(java.util.Map.of(1L, new EnrollmentProgressSnapshot(66.67, 12L, opened)));
    }

    @Test
    void snapshots_noEnrollments_queriesNothing() {
        assertThat(service.snapshots(List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
