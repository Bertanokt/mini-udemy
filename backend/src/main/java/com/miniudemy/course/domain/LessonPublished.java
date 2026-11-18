package com.miniudemy.course.domain;

import java.time.Instant;

/** Bir ders yayımlanınca yayınlanır (Observer). Yayıncısı ders yayımlama issue'sunda eklenir; dinleyicisi `notification` olacak. */
public record LessonPublished(Long lessonId, Long courseId, Instant publishedAt) {
}
