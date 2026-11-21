package com.miniudemy.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.notification.domain.Notification;
import com.miniudemy.notification.domain.NotificationContent;
import com.miniudemy.notification.dto.NotificationPage;
import com.miniudemy.notification.dto.UnreadCountResponse;
import com.miniudemy.notification.mapper.NotificationMapper;
import com.miniudemy.notification.repository.NotificationRepository;

/**
 * Domain olaylarından bildirim kaydı oluşturur (#124, FR-43). Alıcıyı bulur, metni {@link NotificationMessages}'tan
 * alır ve her alıcıya tek bildirim yazar. Çağıranı `listener` paketindeki dinleyicidir; yayıncı modüller bu servisi
 * bilmez (Observer, ADR-009).
 *
 * <p>Kurs bulunamazsa (olaydan sonra silinmişse) bildirim yazılmaz; ilgili bir alıcı ya da metin kalmamıştır.
 *
 * <p>Kullanıcının kendi bildirimlerini listeleme, okunmamış sayısı ve okundu işaretleme de buradadır (#125); çağıranı
 * `NotificationController`'dır.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Kursiyerin adı bulunamazsa metinde kullanılır. */
    static final String UNKNOWN_STUDENT = "Bir kursiyer";

    static final String NOT_FOUND = "Bildirim bulunamadı.";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationRepository notifications;
    private final CourseLookupService courses;
    private final EnrollmentService enrollments;
    private final UserDirectoryService users;
    private final NotificationMapper mapper;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, CourseLookupService courses,
            EnrollmentService enrollments, UserDirectoryService users, NotificationMapper mapper, Clock clock) {
        this.notifications = notifications;
        this.courses = courses;
        this.enrollments = enrollments;
        this.users = users;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Kursa kayıt → kursun eğitmenine. */
    @Transactional
    public void notifyEnrollmentCreated(Long courseId, Long studentId) {
        course(courseId).ifPresent(course -> {
            String studentName = Optional.ofNullable(users.findSummaries(Set.of(studentId)).get(studentId))
                    .map(UserSummary::fullName).orElse(UNKNOWN_STUDENT);
            save(List.of(course.instructor().id()),
                    NotificationMessages.enrollmentCreated(courseId, course.title(), studentName));
        });
    }

    /** Yayımlanmış kursa yeni ders → kursa kayıtlı her kursiyere. */
    @Transactional
    public void notifyLessonPublished(Long courseId, Long lessonId) {
        course(courseId).ifPresent(course -> save(enrollments.findStudentIdsByCourse(courseId),
                NotificationMessages.lessonPublished(courseId, course.title(), lessonId)));
    }

    /** Kurs onayı → eğitmene. */
    @Transactional
    public void notifyCourseApproved(Long courseId, Long instructorId) {
        course(courseId).ifPresent(course -> save(List.of(instructorId),
                NotificationMessages.courseApproved(courseId, course.title())));
    }

    /** Kurs reddi → eğitmene, gerekçesiyle. */
    @Transactional
    public void notifyCourseRejected(Long courseId, Long instructorId, String reason) {
        course(courseId).ifPresent(course -> save(List.of(instructorId),
                NotificationMessages.courseRejected(course.title(), reason)));
    }

    /** Sınav sonucu (geçti ya da kaldı) → kursiyere. */
    @Transactional
    public void notifyQuizCompleted(Long courseId, Long quizId, Long studentId, boolean passed) {
        course(courseId).ifPresent(course -> save(List.of(studentId),
                NotificationMessages.quizCompleted(quizId, course.title(), passed)));
    }

    /** Sertifika oluşturuldu → kursiyere. */
    @Transactional
    public void notifyCertificateIssued(Long courseId, Long studentId) {
        course(courseId).ifPresent(course -> save(List.of(studentId),
                NotificationMessages.certificateIssued(course.title())));
    }

    // --- Kullanıcının kendi bildirimleri (#125). Her sorgu alıcıya göre süzülür; başkasının bildirimi görünmez. ---

    /** Kullanıcının bildirimleri, en yeni önce; {@code unreadOnly} ise yalnız okunmamışlar. */
    @Transactional(readOnly = true)
    public NotificationPage list(Long userId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Notification> result = unreadOnly
                ? notifications.findByUserIdAndReadAtIsNull(userId, pageable)
                : notifications.findByUserId(userId, pageable);
        return new NotificationPage(result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Kullanıcının okunmamış bildirim sayısı. */
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(Long userId) {
        return new UnreadCountResponse(notifications.countByUserIdAndReadAtIsNull(userId));
    }

    /**
     * Bildirimi okundu işaretler; zaten okunmuşsa bir şey değişmez. Bildirim yoksa ya da başka kullanıcınınsa 404:
     * ikisi aynı yanıtı verir, böylece başkasının bildiriminin varlığı sızmaz.
     */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND))
                .markRead(clock.instant());
    }

    /** Kullanıcının okunmamış bildirimlerinin hepsini okundu işaretler. */
    @Transactional
    public void markAllRead(Long userId) {
        notifications.markAllRead(userId, clock.instant());
    }

    private Optional<CourseSummaryResponse> course(Long courseId) {
        Optional<CourseSummaryResponse> course = Optional.ofNullable(courses.findSummaries(Set.of(courseId))
                .get(courseId));
        if (course.isEmpty()) {
            log.warn("Bildirim yazılmadı: kurs {} bulunamadı.", courseId);
        }
        return course;
    }

    private void save(Collection<Long> recipients, NotificationContent content) {
        Instant now = clock.instant();
        notifications.saveAll(recipients.stream().distinct()
                .map(userId -> new Notification(userId, content, now)).toList());
    }
}
