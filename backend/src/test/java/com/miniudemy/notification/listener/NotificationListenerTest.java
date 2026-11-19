package com.miniudemy.notification.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.miniudemy.certificate.domain.CertificateIssued;
import com.miniudemy.course.domain.CourseApproved;
import com.miniudemy.course.domain.CourseRejected;
import com.miniudemy.course.domain.LessonPublished;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.notification.service.NotificationService;
import com.miniudemy.quiz.domain.QuizCompleted;

/** Her olay doğru bildirim işine iletilir (#124). Commit'ten sonra teslim kuralı `NotificationScenarioIT`'de. */
class NotificationListenerTest {

    private static final Instant AT = Instant.parse("2026-11-19T12:00:00Z");

    private final NotificationService notifications = mock(NotificationService.class);
    private final NotificationListener listener = new NotificationListener(notifications);

    @Test
    void enrollmentCreated_notifiesAboutTheEnrollment() {
        listener.onEnrollmentCreated(new EnrollmentCreated(99L, 42L, 3L, AT));

        verify(notifications).notifyEnrollmentCreated(3L, 42L);
    }

    @Test
    void lessonPublished_notifiesAboutTheLesson() {
        listener.onLessonPublished(new LessonPublished(11L, 3L, AT));

        verify(notifications).notifyLessonPublished(3L, 11L);
    }

    @Test
    void courseApproved_notifiesTheInstructor() {
        listener.onCourseApproved(new CourseApproved(3L, 7L, AT));

        verify(notifications).notifyCourseApproved(3L, 7L);
    }

    @Test
    void courseRejected_passesTheReason() {
        listener.onCourseRejected(new CourseRejected(3L, 7L, "Örnekler yetersiz.", AT));

        verify(notifications).notifyCourseRejected(3L, 7L, "Örnekler yetersiz.");
    }

    @Test
    void quizCompleted_passesTheResult() {
        listener.onQuizCompleted(new QuizCompleted(70L, 8L, 3L, 42L, false, AT));

        verify(notifications).notifyQuizCompleted(3L, 8L, 42L, false);
    }

    @Test
    void certificateIssued_notifiesTheStudent() {
        listener.onCertificateIssued(new CertificateIssued(5L, 42L, 3L, AT));

        verify(notifications).notifyCertificateIssued(3L, 42L);
    }
}
