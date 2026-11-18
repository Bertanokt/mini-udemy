package com.miniudemy.course.service;

import java.util.List;

import com.miniudemy.course.dto.RosterEntry;

/**
 * Eğitmenin kursiyer listesi için kayıtlar ve ilerleme özetleri. Arayüz {@code course}'ta, uygulaması
 * {@code enrollment}'ta (docs/architecture.md › Modüller arası iletişim, istatistik arayüzü).
 */
public interface CourseRoster {

    /** Kursa kayıtlı kursiyerler, en son kaydolan önce; {@code page} 0'dan başlar. Sorgu sayısı sayfa boyutundan bağımsız. */
    RosterPage page(Long courseId, int page, int size);

    record RosterPage(List<RosterEntry> entries, long totalElements, int totalPages) {
    }
}
