package com.miniudemy.progress.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.miniudemy.content.domain.LessonOpened;
import com.miniudemy.course.domain.CourseLessonsChanged;
import com.miniudemy.progress.service.ProgressService;

/** Dinleyici olayı ilerleme servisine iletir (#119). Commit'ten sonra teslim kuralı ADR-009 testlerinde. */
class ProgressListenerTest {

    private final ProgressService progressService = mock(ProgressService.class);
    private final ProgressListener listener = new ProgressListener(progressService);

    @Test
    void onLessonOpened_recordsTheLastLesson() {
        Instant opened = Instant.parse("2026-11-18T09:00:00Z");

        listener.onLessonOpened(new LessonOpened(42L, 7L, 12L, opened));

        verify(progressService).recordLessonOpened(42L, 7L, 12L, opened);
    }

    @Test
    void onCourseLessonsChanged_recalculatesEveryEnrollmentOfTheCourse() {
        listener.onCourseLessonsChanged(new CourseLessonsChanged(7L));

        verify(progressService).recalculateCourse(7L);
    }
}
