package com.miniudemy.course.service;

import java.util.Collection;
import java.util.Map;

/**
 * Katalogun ihtiyaç duyduğu kayıt sayıları. Arayüz {@code course}'ta, uygulaması {@code enrollment}'ta: {@code course}
 * {@code enrollment}'a bağımlı olmaz, yön hep {@code enrollment → course} kalır (docs/architecture.md › Modüller arası
 * iletişim, istatistik arayüzü).
 */
public interface EnrollmentCounts {

    /** Kursa kayıtlı kursiyer sayısı. */
    long countByCourse(Long courseId);

    /** Verilen kursların kayıt sayısı tek sorguda; kaydı olmayan kurs haritada 0 olarak yer alır. */
    Map<Long, Long> countByCourses(Collection<Long> courseIds);
}
