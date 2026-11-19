package com.miniudemy.notification.service;

import com.miniudemy.notification.domain.NotificationContent;
import com.miniudemy.notification.domain.NotificationType;

/**
 * Bildirim metinlerinin tek yeri (#124). Her olay için başlık, gövde ve ön yüz bağlantısı burada üretilir; servis ve
 * dinleyiciler metin yazmaz. Bağlantılar ön yüzdeki sayfa yollarıdır.
 */
public final class NotificationMessages {

    private NotificationMessages() {
    }

    /** Kursa kayıt → eğitmene. */
    public static NotificationContent enrollmentCreated(Long courseId, String courseTitle, String studentName) {
        return new NotificationContent(NotificationType.ENROLLMENT_CREATED, "Kursunuza yeni kayıt",
                studentName + " \"" + courseTitle + "\" kursunuza kaydoldu.",
                "/instructor/courses/" + courseId + "/analytics");
    }

    /** Yayımlanmış kursa yeni ders → kayıtlı kursiyerlere. */
    public static NotificationContent lessonPublished(Long courseId, String courseTitle, Long lessonId) {
        return new NotificationContent(NotificationType.LESSON_PUBLISHED, "Yeni ders eklendi",
                "\"" + courseTitle + "\" kursuna yeni bir ders eklendi.",
                "/learn/" + courseId + "/" + lessonId);
    }

    /** Kurs onayı → eğitmene. */
    public static NotificationContent courseApproved(Long courseId, String courseTitle) {
        return new NotificationContent(NotificationType.COURSE_APPROVED, "Kursunuz onaylandı",
                "\"" + courseTitle + "\" kursunuz onaylandı ve katalogda yayımlandı.",
                "/courses/" + courseId);
    }

    /** Kurs reddi → eğitmene; yöneticinin gerekçesi gövdede. */
    public static NotificationContent courseRejected(String courseTitle, String reason) {
        return new NotificationContent(NotificationType.COURSE_REJECTED, "Kursunuz reddedildi",
                "\"" + courseTitle + "\" kursunuz reddedildi. Gerekçe: " + reason,
                "/instructor");
    }

    /** Sınav sonucu → kursiyere. */
    public static NotificationContent quizCompleted(Long quizId, String courseTitle, boolean passed) {
        String title = passed ? "Sınavı geçtiniz" : "Sınavı geçemediniz";
        String body = passed
                ? "\"" + courseTitle + "\" kursundaki sınavı geçtiniz."
                : "\"" + courseTitle + "\" kursundaki sınavı geçemediniz.";
        return new NotificationContent(NotificationType.QUIZ_COMPLETED, title, body,
                "/quizzes/" + quizId + "/attempts");
    }

    /** Sertifika oluşturuldu → kursiyere. */
    public static NotificationContent certificateIssued(String courseTitle) {
        return new NotificationContent(NotificationType.CERTIFICATE_ISSUED, "Sertifikanız hazır",
                "\"" + courseTitle + "\" kursunu tamamladınız; sertifikanız oluşturuldu.",
                "/my-courses");
    }
}
