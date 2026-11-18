package com.miniudemy.progress.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;
import com.miniudemy.enrollment.service.EnrollmentProgress;
import com.miniudemy.progress.domain.CourseProgress;
import com.miniudemy.progress.repository.CourseProgressRepository;

/**
 * Kayıtlı kurslar listesine ilerleme özeti (#119): {@code enrollment}'ın tanımladığı {@link EnrollmentProgress}
 * arayüzünün uygulaması; {@code course_progress} satırları tek sorguda.
 */
@Service
public class EnrollmentProgressService implements EnrollmentProgress {

    private final CourseProgressRepository courseProgress;

    public EnrollmentProgressService(CourseProgressRepository courseProgress) {
        this.courseProgress = courseProgress;
    }

    @Override
    public Map<Long, EnrollmentProgressSnapshot> snapshots(Collection<Long> enrollmentIds) {
        if (enrollmentIds.isEmpty()) {
            return Map.of();
        }
        return courseProgress.findAllByEnrollmentIdIn(enrollmentIds).stream()
                .collect(Collectors.toMap(CourseProgress::getEnrollmentId, row -> new EnrollmentProgressSnapshot(
                        row.getProgressPercent().doubleValue(), row.getLastLessonId(), row.getLastAccessedAt())));
    }
}
