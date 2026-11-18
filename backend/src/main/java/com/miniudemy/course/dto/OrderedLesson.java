package com.miniudemy.course.dto;

/** Kursun bir dersi, kurs içindeki sırasıyla (bölüm sırası, sonra ders sırası) listelenirken (#119). */
public record OrderedLesson(Long lessonId, Long sectionId, String title) {
}
