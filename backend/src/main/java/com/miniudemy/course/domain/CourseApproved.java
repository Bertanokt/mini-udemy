package com.miniudemy.course.domain;

import java.time.Instant;

/** Yönetici bir kursu onaylayınca yayınlanır (Observer). Yayıncı: {@code CourseLifecycleService.approve} (#59). */
public record CourseApproved(Long courseId, Long instructorId, Instant approvedAt) {
}
