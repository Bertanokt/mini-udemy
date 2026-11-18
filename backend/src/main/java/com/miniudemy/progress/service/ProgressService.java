package com.miniudemy.progress.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.CourseProgressResponse.SectionProgress;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.LessonProgressRepository;

/**
 * Kurs ve bölüm ilerleme yüzdesi (#43). Kural: yüzde = tamamlanan ders / toplam ders × 100, iki ondalığa
 * yuvarlanır (örn. 20 dersin 15'i 75.00); dersi olmayan kurs ya da bölümde 0. Yalnız kursta hâlâ var olan dersler
 * sayılır. Okuma her zaman güncel yapıdan hesaplanır; {@code course_progress} satırı liste ekranları için tutulan
 * önbellektir, tamamlama olayında ya da ders silinince {@link #recalculateCourse} ile yenilenir.
 */
@Service
public class ProgressService {

    private final CourseStructureService structure;
    private final EnrollmentService enrollments;
    private final LessonProgressRepository lessonProgress;
    private final CourseProgressRepository courseProgress;

    public ProgressService(CourseStructureService structure, EnrollmentService enrollments,
            LessonProgressRepository lessonProgress, CourseProgressRepository courseProgress) {
        this.structure = structure;
        this.enrollments = enrollments;
        this.lessonProgress = lessonProgress;
        this.courseProgress = courseProgress;
    }

    /** Kursiyerin kurstaki ilerlemesi. Kayıtlı değilse 403. */
    @Transactional(readOnly = true)
    public CourseProgressResponse getMyProgress(Long studentId, Long courseId) {
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        List<SectionLessons> sections = structure.sectionLessons(courseId);
        Set<Long> done = new HashSet<>(lessonProgress.findLessonIdsByEnrollmentId(enrollmentId));

        List<Long> completedIds = sections.stream().flatMap(s -> s.lessonIds().stream())
                .filter(done::contains).toList();
        List<SectionProgress> sectionPercents = sections.stream()
                .map(s -> new SectionProgress(s.sectionId(), percent(countDone(s.lessonIds(), done),
                        s.lessonIds().size())))
                .toList();
        int total = sections.stream().mapToInt(s -> s.lessonIds().size()).sum();
        return new CourseProgressResponse(courseId, percent(completedIds.size(), total), completedIds,
                sectionPercents);
    }

    /** Bir kaydın kurs yüzdesini hesaplayıp {@code course_progress}'e yazar (ders tamamlanınca). */
    @Transactional
    public void refreshCourseProgress(Long studentId, Long courseId) {
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        write(enrollmentId, totalLessonIds(courseId));
    }

    /** Kursa kayıtlı herkesin yüzdesini yeniden yazar; ders silinince çağrılır. */
    @Transactional
    public void recalculateCourse(Long courseId) {
        Set<Long> lessonIds = totalLessonIds(courseId);
        enrollments.findEnrollmentIdsByCourse(courseId).forEach(id -> write(id, lessonIds));
    }

    private void write(Long enrollmentId, Set<Long> courseLessonIds) {
        long done = lessonProgress.findLessonIdsByEnrollmentId(enrollmentId).stream()
                .filter(courseLessonIds::contains).count();
        courseProgress.upsertPercent(enrollmentId, BigDecimal.valueOf(percent(done, courseLessonIds.size())));
    }

    private Set<Long> totalLessonIds(Long courseId) {
        Set<Long> ids = new HashSet<>();
        structure.sectionLessons(courseId).forEach(s -> ids.addAll(s.lessonIds()));
        return ids;
    }

    private static long countDone(List<Long> lessonIds, Set<Long> done) {
        return lessonIds.stream().filter(done::contains).count();
    }

    /** Sıfıra bölme yok: ders yoksa 0. */
    static double percent(long done, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(done * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
