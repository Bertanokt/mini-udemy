package com.miniudemy.course.dto;

import java.util.List;

/** Bir bölümün ders id'leri (ders `position` sırasıyla); başka modüller yüzde gibi hesapları için kullanır. */
public record SectionLessons(Long sectionId, List<Long> lessonIds) {
}
