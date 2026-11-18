package com.miniudemy.course.domain;

import java.time.Instant;

/** Yönetici bir kursu gerekçeyle reddedince yayınlanır (Observer). Yayıncısı admin red issue'sunda eklenir. */
public record CourseRejected(Long courseId, Long instructorId, String reason, Instant rejectedAt) {
}
