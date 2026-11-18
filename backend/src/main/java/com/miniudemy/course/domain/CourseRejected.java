package com.miniudemy.course.domain;

import java.time.Instant;

/** Yönetici bir kursu gerekçeyle reddedince yayınlanır (Observer). Yayıncı: {@code CourseLifecycleService.reject} (#59). */
public record CourseRejected(Long courseId, Long instructorId, String reason, Instant rejectedAt) {
}
