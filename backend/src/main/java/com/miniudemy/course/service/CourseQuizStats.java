package com.miniudemy.course.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.miniudemy.course.dto.QuizAttemptStats;
import com.miniudemy.course.dto.QuizBestResult;

/**
 * Eğitmen istatistiklerinin ihtiyaç duyduğu sınav verisi. Arayüz {@code course}'ta, uygulaması {@code quiz}'de
 * (docs/architecture.md › Modüller arası iletişim, istatistik arayüzü).
 */
public interface CourseQuizStats {

    /** Kursun her sınavı için tüm denemelerin sayısı ve geçenlerin sayısı; denemesiz sınav (0, 0). Sınav id sırasıyla. */
    List<QuizAttemptStats> attemptStats(Long courseId);

    /**
     * Verilen kursiyerlerin sınav başına en iyi sonucu. Hiç denemesi olmayan sınav ya da kursiyer haritada yer almaz.
     */
    Map<Long, List<QuizBestResult>> bestResults(Long courseId, Collection<Long> studentIds);
}
