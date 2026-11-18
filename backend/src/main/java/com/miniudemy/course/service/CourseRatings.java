package com.miniudemy.course.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Katalogun ve eğitmen istatistiklerinin ihtiyaç duyduğu puan verisi. Arayüz {@code course}'ta, uygulaması
 * {@code review}'da: {@code course} {@code review}'a bağımlı olmaz (docs/architecture.md › Modüller arası iletişim).
 */
public interface CourseRatings {

    /** Bir kursun ortalama puanı ve değerlendirme sayısı; yorumsuz kurs: (null, 0). */
    CourseRating ratingOf(Long courseId);

    /** Verilen her kurs için bir kayıt, tek sorguda; yorumsuz kurs (null, 0). */
    Map<Long, CourseRating> ratingsByCourses(Collection<Long> courseIds);

    /** Ortalaması (kartta görünen, bir ondalığa yuvarlanmış değer) {@code minRating}'e ulaşan kursların id'leri. */
    Set<Long> courseIdsWithAverageAtLeast(double minRating);
}
