package com.miniudemy.progress.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.OrderedLesson;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.domain.CourseProgress;
import com.miniudemy.progress.dto.CourseProgressResponse.SectionProgress;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.ResumeResponse;
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

    /** Kursiyerin son açtığı dersi yazar ({@code LessonOpened} dinleyicisi, #119). */
    @Transactional
    public void recordLessonOpened(Long studentId, Long courseId, Long lessonId, Instant openedAt) {
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        courseProgress.upsertLastLesson(enrollmentId, lessonId, openedAt);
    }

    /**
     * Kaldığı yerden devam (#119): son açılan ders; yoksa ilk tamamlanmamış ders; hepsi tamamlanmışsa kursun ilk
     * dersi; kursta ders yoksa ders alanları null. Kayıtlı değilse 403.
     */
    @Transactional(readOnly = true)
    public ResumeResponse getMyResumePoint(Long studentId, Long courseId) {
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        List<OrderedLesson> lessons = structure.orderedLessons(courseId);
        Set<Long> done = new HashSet<>(lessonProgress.findLessonIdsByEnrollmentId(enrollmentId));
        Optional<CourseProgress> row = courseProgress.findByEnrollmentId(enrollmentId);
        Long lastLessonId = row.map(CourseProgress::getLastLessonId).orElse(null);
        Instant lastAccessedAt = row.map(CourseProgress::getLastAccessedAt).orElse(null);

        Optional<OrderedLesson> resume = lessons.stream().filter(l -> l.lessonId().equals(lastLessonId)).findFirst()
                // KIRMIZI-2 "ilk tamamlanmamış ders" adımı kapatıldı
                .or(() -> lessons.stream().findFirst());
        long completed = lessons.stream().filter(l -> done.contains(l.lessonId())).count();
        double percent = percent(completed, lessons.size());
        return resume
                .map(l -> new ResumeResponse(courseId, l.lessonId(), l.title(), l.sectionId(), percent, lastAccessedAt))
                .orElse(new ResumeResponse(courseId, null, null, null, percent, lastAccessedAt));
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
