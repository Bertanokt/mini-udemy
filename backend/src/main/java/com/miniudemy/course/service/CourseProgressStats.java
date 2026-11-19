package com.miniudemy.course.service;

import java.math.BigDecimal;

/**
 * Eğitmen istatistiklerinin ihtiyaç duyduğu ilerleme toplamları. Arayüz {@code course}'ta, uygulaması
 * {@code progress}'te (docs/architecture.md › Modüller arası iletişim, istatistik arayüzü).
 */
public interface CourseProgressStats {

    /**
     * Kursa kayıtlı kursiyerlerin kurs yüzdelerinin toplamı ve yüzdesi %100 olanların sayısı. İlerleme satırı olmayan
     * kayıt toplama 0 katar; kaydı olmayan kursta (0, 0).
     */
    ProgressTotals totals(Long courseId);

    record ProgressTotals(BigDecimal percentSum, long completedCount) {
    }
}
