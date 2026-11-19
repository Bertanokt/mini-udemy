package com.miniudemy.progress.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.service.CourseProgressStats;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.CourseProgressRepository.ProgressTotalsRow;

/**
 * Eğitmen istatistikleri için ilerleme toplamları (#133): {@code course}'un tanımladığı {@link CourseProgressStats}
 * arayüzünün uygulaması. Kayıt id'leri {@code enrollment}'tan alınır, toplam {@code course_progress} üzerinde tek
 * sorguyla hesaplanır.
 */
@Service
@Transactional(readOnly = true)
public class CourseProgressStatsService implements CourseProgressStats {

    private final EnrollmentService enrollments;
    private final CourseProgressRepository courseProgress;

    public CourseProgressStatsService(EnrollmentService enrollments, CourseProgressRepository courseProgress) {
        this.enrollments = enrollments;
        this.courseProgress = courseProgress;
    }

    @Override
    public ProgressTotals totals(Long courseId) {
        List<Long> enrollmentIds = enrollments.findEnrollmentIdsByCourse(courseId);
        if (enrollmentIds.isEmpty()) {
            return new ProgressTotals(BigDecimal.ZERO, 0);
        }
        ProgressTotalsRow row = courseProgress.totalsByEnrollmentIds(enrollmentIds);
        return new ProgressTotals(row.getPercentSum(), row.getCompletedCount());
    }

    /** Kursu tamamlamış (%100) kayıt sayısı, bütün kurslarda (platform raporu, #145). */
    public long countCompletedEnrollments() {
        return courseProgress.countCompleted();
    }
}
