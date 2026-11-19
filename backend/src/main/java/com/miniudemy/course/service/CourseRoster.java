package com.miniudemy.course.service;

import java.util.List;

import com.miniudemy.course.dto.RosterEntry;

/**
 * Eğitmenin kursiyer listesi için kayıtlar ve ilerleme özetleri; ayrıca tek bir kaydın varlığı. Arayüz {@code course}'ta, uygulaması
 * {@code enrollment}'ta (docs/architecture.md › Modüller arası iletişim, istatistik arayüzü).
 */
public interface CourseRoster {

    /** Kursa kayıtlı kursiyerler, en son kaydolan önce; {@code page} 0'dan başlar. Sorgu sayısı sayfa boyutundan bağımsız. */
    RosterPage page(Long courseId, int page, int size);

    /** Kursiyer kursa kayıtlı mı; yayından kaldırılan kursu kayıtlı kursiyerin görmesi için ({@link CourseVisibility}, #123). */
    boolean isEnrolled(Long courseId, Long studentId);

    record RosterPage(List<RosterEntry> entries, long totalElements, int totalPages) {
    }
}
