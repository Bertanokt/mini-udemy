package com.miniudemy.course.domain;

/**
 * Kursun ders sayısı değişince yayınlanır: ders eklendi, ders silindi ya da dersleriyle bir bölüm silindi (#249,
 * Observer). Dinleyen: {@code progress} (kayıtlıların {@code course_progress} yüzde önbelleğini yeniden hesaplar).
 * Ders taşıma ve güncelleme sayıyı değiştirmediği için yayınlanmaz.
 */
public record CourseLessonsChanged(Long courseId) {
}
