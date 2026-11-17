package com.miniudemy.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.LessonLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.repository.LessonProgressRepository;

/**
 * Ders tamamlama (#41): kayıtlı öğrenci; ilk tamamlamada kayıt + LessonCompleted, tekrarında ikisi de yok (idempotent).
 * "Zaten tamamlanmış mı" kararını veritabanının ON CONFLICT DO NOTHING ekleme komutu verir (eklenen satır 1 ya da 0).
 */
class LessonCompletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-17T20:00:00Z");

    private final LessonLookupService lessons = mock(LessonLookupService.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final LessonProgressRepository lessonProgress = mock(LessonProgressRepository.class);
    private final ProgressService progress = mock(ProgressService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private LessonCompletionService service;

    private final CourseProgressResponse current = new CourseProgressResponse(3L, 33.33, List.of(11L), List.of());

    @BeforeEach
    void setUp() {
        service = new LessonCompletionService(lessons, enrollments, lessonProgress, progress, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
        given(lessons.courseIdOfLesson(11L)).willReturn(3L);
        given(enrollments.requireEnrollmentId(42L, 3L)).willReturn(500L);
        given(progress.getMyProgress(42L, 3L)).willReturn(current);
    }

    @Test
    void complete_firstTime_recordsLesson_publishesLessonCompleted_andReturnsProgress() {
        given(lessonProgress.insertIfAbsent(500L, 11L, NOW)).willReturn(1);

        assertThat(service.complete(42L, 11L)).isEqualTo(current);
        verify(events).publishEvent(new LessonCompleted(42L, 3L, 11L, NOW));
    }

    @Test
    void complete_again_isIdempotent_noEventIsPublished_butProgressIsReturned() {
        given(lessonProgress.insertIfAbsent(500L, 11L, NOW)).willReturn(0);

        assertThat(service.complete(42L, 11L)).isEqualTo(current);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void complete_notEnrolled_returns403_andRecordsNothing() {
        given(enrollments.requireEnrollmentId(42L, 3L)).willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        assertThatThrownBy(() -> service.complete(42L, 11L)).isInstanceOf(ForbiddenException.class);
        verify(lessonProgress, never()).insertIfAbsent(anyLong(), anyLong(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void complete_unknownLesson_returns404_andRecordsNothing() {
        given(lessons.courseIdOfLesson(99L)).willThrow(new NotFoundException("Ders bulunamadı."));

        assertThatThrownBy(() -> service.complete(42L, 99L)).isInstanceOf(NotFoundException.class);
        verify(lessonProgress, never()).insertIfAbsent(anyLong(), anyLong(), any());
    }
}
