package com.miniudemy.enrollment.domain;

import java.time.Instant;

/**
 * Bir kursiyer kursa kaydolunca yayınlanır (Observer). Dinleyiciler (eğitmene bildirim vb.) #42 ve #124 ile bağlanır;
 * yayınlayan modül dinleyenleri bilmez.
 */
public record EnrollmentCreated(Long enrollmentId, Long studentId, Long courseId, Instant enrolledAt) {
}
