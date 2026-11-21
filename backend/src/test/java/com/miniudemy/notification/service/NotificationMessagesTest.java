package com.miniudemy.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.miniudemy.notification.domain.NotificationContent;
import com.miniudemy.notification.domain.NotificationType;

/** Bildirim metinleri tek yerde (#124): her olayın türü, başlığı, gövdesi ve bağlantısı. */
class NotificationMessagesTest {

    @Test
    void enrollmentCreated_namesTheStudentAndCourse_linksToCourseAnalytics() {
        NotificationContent content = NotificationMessages.enrollmentCreated(3L, "Java'ya giriş", "Ayşe Yılmaz");

        assertThat(content).isEqualTo(new NotificationContent(NotificationType.ENROLLMENT_CREATED,
                "Kursunuza yeni kayıt", "Ayşe Yılmaz \"Java'ya giriş\" kursunuza kaydoldu.",
                "/instructor/courses/3/analytics"));
    }

    @Test
    void lessonPublished_namesTheCourse_linksToTheLesson() {
        NotificationContent content = NotificationMessages.lessonPublished(3L, "Java'ya giriş", 11L);

        assertThat(content).isEqualTo(new NotificationContent(NotificationType.LESSON_PUBLISHED,
                "Yeni ders eklendi", "\"Java'ya giriş\" kursuna yeni bir ders eklendi.", "/learn/3/11"));
    }

    @Test
    void courseApproved_linksToTheCoursePage() {
        NotificationContent content = NotificationMessages.courseApproved(3L, "Java'ya giriş");

        assertThat(content).isEqualTo(new NotificationContent(NotificationType.COURSE_APPROVED,
                "Kursunuz onaylandı", "\"Java'ya giriş\" kursunuz onaylandı ve katalogda yayımlandı.",
                "/courses/3"));
    }

    @Test
    void courseRejected_carriesTheReason() {
        NotificationContent content = NotificationMessages.courseRejected("Java'ya giriş", "Örnekler yetersiz.");

        assertThat(content).isEqualTo(new NotificationContent(NotificationType.COURSE_REJECTED,
                "Kursunuz reddedildi", "\"Java'ya giriş\" kursunuz reddedildi. Gerekçe: Örnekler yetersiz.",
                "/instructor"));
    }

    @Test
    void quizCompleted_passedAndFailed_haveDifferentTexts_andLinkToAttempts() {
        NotificationContent passed = NotificationMessages.quizCompleted(8L, "Java'ya giriş", true);
        NotificationContent failed = NotificationMessages.quizCompleted(8L, "Java'ya giriş", false);

        assertThat(passed).isEqualTo(new NotificationContent(NotificationType.QUIZ_COMPLETED, "Sınavı geçtiniz",
                "\"Java'ya giriş\" kursundaki sınavı geçtiniz.", "/quizzes/8/attempts"));
        assertThat(failed).isEqualTo(new NotificationContent(NotificationType.QUIZ_COMPLETED, "Sınavı geçemediniz",
                "\"Java'ya giriş\" kursundaki sınavı geçemediniz.", "/quizzes/8/attempts"));
    }

    @Test
    void certificateIssued_namesTheCourse() {
        NotificationContent content = NotificationMessages.certificateIssued("Java'ya giriş");

        assertThat(content).isEqualTo(new NotificationContent(NotificationType.CERTIFICATE_ISSUED,
                "Sertifikanız hazır", "\"Java'ya giriş\" kursunu tamamladınız; sertifikanız oluşturuldu.",
                "/certificates"));
    }
}
