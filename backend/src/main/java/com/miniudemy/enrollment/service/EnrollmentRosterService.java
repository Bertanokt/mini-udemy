package com.miniudemy.enrollment.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.RosterEntry;
import com.miniudemy.course.service.CourseRoster;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

/**
 * Eğitmenin kursiyer listesi (#134): {@code course}'un tanımladığı {@link CourseRoster} arayüzünün uygulaması. Kayıtlar
 * sayfa sorgusuyla, ilerleme özetleri {@link EnrollmentProgress} ile tek sorguda gelir.
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentRosterService implements CourseRoster {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("enrolledAt"), Sort.Order.desc("id"));

    private final EnrollmentRepository repository;
    private final EnrollmentProgress progress;

    public EnrollmentRosterService(EnrollmentRepository repository, EnrollmentProgress progress) {
        this.repository = repository;
        this.progress = progress;
    }

    @Override
    public RosterPage page(Long courseId, int page, int size) {
        Page<Enrollment> result = repository.findByCourseId(courseId, PageRequest.of(page, size, NEWEST_FIRST));
        if (result.isEmpty()) {
            return new RosterPage(List.of(), result.getTotalElements(), result.getTotalPages());
        }
        Map<Long, EnrollmentProgressSnapshot> snapshots = progress.snapshots(
                result.getContent().stream().map(Enrollment::getId).toList());
        List<RosterEntry> entries = result.getContent().stream().map(e -> {
            EnrollmentProgressSnapshot snapshot = snapshots.getOrDefault(e.getId(), EnrollmentProgressSnapshot.NONE);
            return new RosterEntry(e.getId(), e.getStudentId(), e.getEnrolledAt(), snapshot.progressPercent(),
                    snapshot.lastAccessedAt());
        }).toList();
        return new RosterPage(entries, result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public boolean isEnrolled(Long courseId, Long studentId) {
        return repository.existsByStudentIdAndCourseId(studentId, courseId);
    }
}
