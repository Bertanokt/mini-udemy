package com.miniudemy.enrollment.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
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

    /** Bütün kayıtların sayısı (platform raporu, #145). */
    public long countAll() {
        return repository.count();
    }

    /** En çok kayıt alan {@code limit} kurs: kurs id → kayıt sayısı, çoktan aza (platform raporu, #145). */
    public Map<Long, Long> topCourses(int limit) {
        Map<Long, Long> top = new LinkedHashMap<>();
        repository.findTopCourses(PageRequest.of(0, limit))
                .forEach(row -> top.put(row.getCourseId(), row.getTotal()));
        return top;
    }
}
