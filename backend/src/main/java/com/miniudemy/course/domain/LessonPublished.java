package com.miniudemy.course.domain;

import java.time.Instant;

/** Yayımlanmış kursa yeni ders eklenince `LessonService.create` yayınlar (Observer, #25); dinleyicisi `notification` olacak. */
public record LessonPublished(Long lessonId, Long courseId, Instant publishedAt) {
}
