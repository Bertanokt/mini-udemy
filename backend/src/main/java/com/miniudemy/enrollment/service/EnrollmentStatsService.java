package com.miniudemy.enrollment.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.service.EnrollmentCounts;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

/** Kayıt sayıları: katalog kartındaki {@code enrollmentCount} ve "popüler" sıralaması buradan beslenir. */
@Service
@Transactional(readOnly = true)
public class EnrollmentStatsService implements EnrollmentCounts {

    private final EnrollmentRepository repository;

    public EnrollmentStatsService(EnrollmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countByCourse(Long courseId) {
        return repository.countByCourseId(courseId);
    }

    @Override
    public Map<Long, Long> countByCourses(Collection<Long> courseIds) {
        Map<Long, Long> counts = new HashMap<>();
        courseIds.forEach(id -> counts.put(id, 0L));
        if (!courseIds.isEmpty()) {
            repository.countByCourseIds(courseIds).forEach(row -> counts.put(row.getCourseId(), row.getTotal()));
        }
        return counts;
    }
}
