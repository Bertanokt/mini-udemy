package com.miniudemy.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.notification.domain.Notification;
import com.miniudemy.notification.domain.NotificationType;
import com.miniudemy.notification.dto.NotificationPage;
import com.miniudemy.notification.mapper.NotificationMapperImpl;
import com.miniudemy.notification.repository.NotificationRepository;

/**
 * Olaydan bildirime (#124, FR-43): doğru alıcı, doğru tür ve metin, alıcı başına tek kayıt. Kullanıcının kendi
 * bildirimleri (#125): sayfalı liste en yeni önce, okunmamış sayısı, okundu işaretleme; başkasının bildirimi 404.
 */
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final long COURSE = 3L;
    private static final long INSTRUCTOR = 7L;
    private static final long STUDENT = 42L;

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final CourseLookupService courses = mock(CourseLookupService.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final UserDirectoryService users = mock(UserDirectoryService.class);
    private final NotificationService service = new NotificationService(repository, courses, enrollments, users,
            new NotificationMapperImpl(), Clock.fixed(NOW, ZoneOffset.UTC));

    private final List<Notification> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        given(repository.saveAll(any())).willAnswer(inv -> {
            Iterable<Notification> batch = inv.getArgument(0);
            batch.forEach(saved::add);
            return batch;
        });
        given(courses.findSummaries(Set.of(COURSE))).willReturn(Map.of(COURSE, course()));
    }

    private static CourseSummaryResponse course() {
        return new CourseSummaryResponse(COURSE, "Java'ya giriş", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, CourseStatus.PUBLISHED, new UserSummary(INSTRUCTOR, "Zeynep Kaya"), null, 3,
                900, 1L, null, 0);
    }

    // --- kursa kayıt → eğitmen ---

    @Test
    void enrollmentCreated_notifiesTheCourseInstructor_withTheStudentsName() {
        given(users.findSummaries(Set.of(STUDENT))).willReturn(Map.of(STUDENT, new UserSummary(STUDENT, "Ayşe Yılmaz")));

        service.notifyEnrollmentCreated(COURSE, STUDENT);

        assertThat(saved).singleElement().satisfies(n -> {
            assertThat(n.getUserId()).isEqualTo(INSTRUCTOR);
            assertThat(n.getType()).isEqualTo(NotificationType.ENROLLMENT_CREATED);
            assertThat(n.getBody()).isEqualTo("Ayşe Yılmaz \"Java'ya giriş\" kursunuza kaydoldu.");
            assertThat(n.getLink()).isEqualTo("/instructor/courses/3/analytics");
            assertThat(n.getCreatedAt()).isEqualTo(NOW);
            assertThat(n.getReadAt()).as("yeni bildirim okunmamış").isNull();
        });
    }

    @Test
    void enrollmentCreated_studentNameNotFound_stillNotifiesWithAGenericName() {
        given(users.findSummaries(Set.of(STUDENT))).willReturn(Map.of());

        service.notifyEnrollmentCreated(COURSE, STUDENT);

        assertThat(saved).singleElement()
                .satisfies(n -> assertThat(n.getBody()).startsWith(NotificationService.UNKNOWN_STUDENT + " "));
    }

    // --- yeni ders → kayıtlı kursiyerler ---

    @Test
    void lessonPublished_notifiesEveryEnrolledStudentOnce() {
        given(enrollments.findStudentIdsByCourse(COURSE)).willReturn(List.of(42L, 43L, 44L));

        service.notifyLessonPublished(COURSE, 11L);

        assertThat(saved).extracting(Notification::getUserId, Notification::getType, Notification::getLink)
                .containsExactly(
                        tuple(42L, NotificationType.LESSON_PUBLISHED, "/learn/3/11"),
                        tuple(43L, NotificationType.LESSON_PUBLISHED, "/learn/3/11"),
                        tuple(44L, NotificationType.LESSON_PUBLISHED, "/learn/3/11"));
    }

    @Test
    void lessonPublished_noEnrolledStudents_createsNoNotification() {
        given(enrollments.findStudentIdsByCourse(COURSE)).willReturn(List.of());

        service.notifyLessonPublished(COURSE, 11L);

        assertThat(saved).isEmpty();
    }

    // --- kurs onayı / reddi → eğitmen ---

    @Test
    void courseApproved_notifiesTheInstructor() {
        service.notifyCourseApproved(COURSE, INSTRUCTOR);

        assertThat(saved).singleElement().satisfies(n -> {
            assertThat(n.getUserId()).isEqualTo(INSTRUCTOR);
            assertThat(n.getType()).isEqualTo(NotificationType.COURSE_APPROVED);
            assertThat(n.getTitle()).isEqualTo("Kursunuz onaylandı");
        });
    }

    @Test
    void courseRejected_notifiesTheInstructor_withTheReason() {
        service.notifyCourseRejected(COURSE, INSTRUCTOR, "Örnekler yetersiz.");

        assertThat(saved).singleElement().satisfies(n -> {
            assertThat(n.getUserId()).isEqualTo(INSTRUCTOR);
            assertThat(n.getType()).isEqualTo(NotificationType.COURSE_REJECTED);
            assertThat(n.getBody()).contains("Gerekçe: Örnekler yetersiz.");
        });
    }

    // --- sınav sonucu / sertifika → kursiyer ---

    @Test
    void quizCompleted_notifiesTheStudent_passedOrFailed() {
        service.notifyQuizCompleted(COURSE, 8L, STUDENT, true);
        service.notifyQuizCompleted(COURSE, 8L, STUDENT, false);

        assertThat(saved).extracting(Notification::getUserId, Notification::getType, Notification::getTitle)
                .containsExactly(
                        tuple(STUDENT, NotificationType.QUIZ_COMPLETED, "Sınavı geçtiniz"),
                        tuple(STUDENT, NotificationType.QUIZ_COMPLETED, "Sınavı geçemediniz"));
    }

    @Test
    void certificateIssued_notifiesTheStudent() {
        service.notifyCertificateIssued(COURSE, STUDENT);

        assertThat(saved).singleElement().satisfies(n -> {
            assertThat(n.getUserId()).isEqualTo(STUDENT);
            assertThat(n.getType()).isEqualTo(NotificationType.CERTIFICATE_ISSUED);
        });
    }

    // --- kurs bulunamadı ---

    @Test
    void courseNotFound_noNotificationIsWritten_forAnyEvent() {
        given(courses.findSummaries(Set.of(COURSE))).willReturn(Map.of());

        service.notifyEnrollmentCreated(COURSE, STUDENT);
        service.notifyLessonPublished(COURSE, 11L);
        service.notifyCourseApproved(COURSE, INSTRUCTOR);
        service.notifyCourseRejected(COURSE, INSTRUCTOR, "Örnekler yetersiz.");
        service.notifyQuizCompleted(COURSE, 8L, STUDENT, true);
        service.notifyCertificateIssued(COURSE, STUDENT);

        assertThat(saved).isEmpty();
        verify(repository, never()).saveAll(any());
        verify(enrollments, never()).findStudentIdsByCourse(anyLong());
    }

    // --- kullanıcının kendi bildirimleri (#125) ---

    @Test
    void list_returnsOwnNotificationsNewestFirst_asPage() {
        Notification read = stored(5L, STUDENT, NOW.minusSeconds(60));
        read.markRead(NOW);
        Notification unread = stored(4L, STUDENT, NOW.minusSeconds(120));
        PageRequest expected = PageRequest.of(1, 2,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        given(repository.findByUserId(STUDENT, expected))
                .willReturn(new PageImpl<>(List.of(read, unread), expected, 5));

        NotificationPage page = service.list(STUDENT, false, 1, 2);

        assertThat(page.content()).extracting(r -> r.id(), r -> r.read(), r -> r.type(), r -> r.link())
                .containsExactly(tuple(5L, true, NotificationType.COURSE_APPROVED, "/courses/3"),
                        tuple(4L, false, NotificationType.COURSE_APPROVED, "/courses/3"));
        assertThat(page.content().get(0).title()).isEqualTo("Kursunuz onaylandı");
        assertThat(page.content().get(0).createdAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(List.of(page.page(), page.size(), page.totalPages())).containsExactly(1, 2, 3);
        assertThat(page.totalElements()).isEqualTo(5);
        verify(repository, never()).findByUserIdAndReadAtIsNull(anyLong(), any());
    }

    @Test
    void list_unreadOnly_queriesOnlyUnread() {
        Notification unread = stored(4L, STUDENT, NOW);
        given(repository.findByUserIdAndReadAtIsNull(eq(STUDENT), any()))
                .willReturn(new PageImpl<>(List.of(unread), PageRequest.of(0, 20), 1));

        NotificationPage page = service.list(STUDENT, true, 0, 20);

        assertThat(page.content()).singleElement().satisfies(r -> assertThat(r.read()).isFalse());
        verify(repository, never()).findByUserId(anyLong(), any());
    }

    @Test
    void countUnread_returnsTheUsersUnreadCount() {
        given(repository.countByUserIdAndReadAtIsNull(STUDENT)).willReturn(3L);

        assertThat(service.countUnread(STUDENT).count()).isEqualTo(3);
    }

    @Test
    void markRead_ownUnreadNotification_setsReadAtToNow() {
        Notification notification = stored(4L, STUDENT, NOW.minusSeconds(60));
        given(repository.findByIdAndUserId(4L, STUDENT)).willReturn(Optional.of(notification));

        service.markRead(STUDENT, 4L);

        assertThat(notification.getReadAt()).isEqualTo(NOW);
    }

    @Test
    void markRead_alreadyRead_keepsTheFirstReadTime() {
        Notification notification = stored(4L, STUDENT, NOW.minusSeconds(600));
        Instant firstRead = NOW.minusSeconds(300);
        notification.markRead(firstRead);
        given(repository.findByIdAndUserId(4L, STUDENT)).willReturn(Optional.of(notification));

        service.markRead(STUDENT, 4L);

        assertThat(notification.getReadAt()).isEqualTo(firstRead);
    }

    @Test
    void markRead_othersOrMissingNotification_returnsNotFound() {
        given(repository.findByIdAndUserId(4L, STUDENT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(STUDENT, 4L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Bildirim bulunamadı.");
    }

    @Test
    void markAllRead_marksTheUsersUnreadNotificationsWithNow() {
        service.markAllRead(STUDENT);

        verify(repository).markAllRead(STUDENT, NOW);
    }

    private static Notification stored(long id, long userId, Instant createdAt) {
        Notification notification = new Notification(userId,
                NotificationMessages.courseApproved(COURSE, "Java'ya giriş"), createdAt);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
