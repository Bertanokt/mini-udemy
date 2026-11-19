package com.miniudemy.progress.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.service.LessonLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.repository.LessonProgressRepository;

/**
 * Dersi tamamlandı işaretleme (#41). Yalnız kursa kayıtlı kursiyer; tekrar çağrı bir şey değiştirmez (idempotent).
 *
 * "İlk tamamlama mı" kararı {@link LessonProgressRepository#insertIfAbsent}'in (ON CONFLICT DO NOTHING) döndürdüğü
 * satır sayısıdır. Eşzamanlı iki istekte biri 1, diğeri 0 alır; tekillik ihlali istisnası oluşmadığı için transaction
 * rollback-only olmaz ve aynı transaction'daki ilerleme sorgusu güvenle çalışır. {@link LessonCompleted} yalnız ilk
 * tamamlamada yayınlanır; dinleyici (ProgressListener) commit'ten sonra kurs ilerlemesini günceller.
 */
@Service
public class LessonCompletionService {

    private final LessonLookupService lessons;
    private final EnrollmentService enrollments;
    private final LessonProgressRepository lessonProgress;
    private final ProgressService progress;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LessonCompletionService(LessonLookupService lessons, EnrollmentService enrollments,
            LessonProgressRepository lessonProgress, ProgressService progress, ApplicationEventPublisher events,
            Clock clock) {
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.lessonProgress = lessonProgress;
        this.progress = progress;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public CourseProgressResponse complete(Long studentId, Long lessonId) {
        Long courseId = lessons.courseIdOfLesson(lessonId);
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        Instant now = clock.instant();
        if (lessonProgress.insertIfAbsent(enrollmentId, lessonId, now) == 1) {
            events.publishEvent(new LessonCompleted(studentId, courseId, lessonId, now));
        }
        return progress.getMyProgress(studentId, courseId);
    }
}
