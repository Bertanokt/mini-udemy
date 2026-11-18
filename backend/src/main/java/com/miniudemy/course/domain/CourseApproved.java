package com.miniudemy.course.domain;

import java.time.Instant;

/** Yönetici bir kursu onaylayınca yayınlanır (Observer). Yayıncısı admin onay issue'sunda eklenir. */
public record CourseApproved(Long courseId, Long instructorId, Instant approvedAt) {
}
