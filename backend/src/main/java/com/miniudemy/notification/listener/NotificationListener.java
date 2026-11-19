package com.miniudemy.notification.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.miniudemy.certificate.domain.CertificateIssued;
import com.miniudemy.course.domain.CourseApproved;
import com.miniudemy.course.domain.CourseRejected;
import com.miniudemy.course.domain.LessonPublished;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.notification.service.NotificationService;
import com.miniudemy.quiz.domain.QuizCompleted;

/**
 * Altı domain olayını dinleyip bildirim oluşturur (Observer, ADR-009; #124, FR-43). Yayıncılar bu sınıfı bilmez.
 * Her metot yayıncının işlemi commit edildikten sonra çalışır ve bildirimi kendi işleminde yazar; bildirim yazılamazsa
 * yayıncının işi geri alınmaz.
 */
@Component
public class NotificationListener {

    private final NotificationService notifications;

    public NotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCreated(EnrollmentCreated event) {
        notifications.notifyEnrollmentCreated(event.courseId(), event.studentId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonPublished(LessonPublished event) {
        notifications.notifyLessonPublished(event.courseId(), event.lessonId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCourseApproved(CourseApproved event) {
        notifications.notifyCourseApproved(event.courseId(), event.instructorId());
    }

    @org.springframework.context.event.EventListener /* KIRMIZI K1 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCourseRejected(CourseRejected event) {
        notifications.notifyCourseRejected(event.courseId(), event.instructorId(), event.reason());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onQuizCompleted(QuizCompleted event) {
        notifications.notifyQuizCompleted(event.courseId(), event.quizId(), event.studentId(), event.passed());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCertificateIssued(CertificateIssued event) {
        notifications.notifyCertificateIssued(event.courseId(), event.studentId());
    }
}
