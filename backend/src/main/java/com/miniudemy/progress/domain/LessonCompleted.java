package com.miniudemy.progress.domain;

import java.time.Instant;

/** Kursiyer bir dersi tamamlayınca yayınlanır (Observer). Yayıncısı ilerleme issue'sunda eklenir. */
public record LessonCompleted(Long studentId, Long courseId, Long lessonId, Instant completedAt) {
}
