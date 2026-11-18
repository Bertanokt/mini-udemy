package com.miniudemy.content.domain;

import java.time.Instant;

/**
 * Kursa kayıtlı bir kursiyer bir dersin içeriğini açtığında yayınlanır (Observer, #119). Dinleyen: {@code progress}
 * (son izlenen ders, kaldığı yerden devam). Kursun eğitmeni ve yönetici açtığında yayınlanmaz.
 */
public record LessonOpened(Long studentId, Long courseId, Long lessonId, Instant openedAt) {
}
