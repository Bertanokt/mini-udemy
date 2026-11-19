package com.miniudemy.course.dto;

import java.time.Instant;

/** Eğitmenin kursiyer listesinde bir kayıt: kursiyer, kayıt tarihi ve ilerleme özeti (#134). */
public record RosterEntry(Long enrollmentId, Long studentId, Instant enrolledAt, double progressPercent,
        Instant lastAccessedAt) {
}
