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

    /**
     * Kursa kayıtlı herkesin kurs yüzdesine göre dağılımı (bütün kayıtlar, sayfa değil). İlerleme satırı olmayan kayıt
     * 0 kovasına girer; beş kovanın toplamı kayıt sayısına eşittir, kaydı olmayan kursta hepsi 0.
     */
    ProgressDistribution distribution(Long courseId);

    /** Kovalar: yüzde &lt; 25, [25, 50), [50, 75), [75, 100), 100 (yüzde 100'e ulaşanlar). */
    record ProgressDistribution(long below25, long from25, long from50, long from75, long completed) {
    }
}
