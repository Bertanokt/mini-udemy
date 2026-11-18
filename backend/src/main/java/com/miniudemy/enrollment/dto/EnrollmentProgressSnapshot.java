package com.miniudemy.enrollment.dto;

import java.time.Instant;

/** Bir kaydın ilerleme özeti (#119): kurs yüzdesi ve son izlenen ders. İlerleme kaydı yoksa yüzde 0, ders yok. */
public record EnrollmentProgressSnapshot(double progressPercent, Long lastLessonId, Instant lastAccessedAt) {

    public static final EnrollmentProgressSnapshot NONE = new EnrollmentProgressSnapshot(0.0, null, null);
}
