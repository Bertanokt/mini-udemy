package com.miniudemy.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

import com.miniudemy.certificate.domain.CertificateIssued;
import com.miniudemy.course.domain.CourseApproved;
import com.miniudemy.course.domain.CourseRejected;
import com.miniudemy.course.domain.LessonPublished;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.notification.listener.NotificationListener;
import com.miniudemy.notification.service.NotificationService;
import com.miniudemy.quiz.domain.QuizCompleted;

/**
 * Bildirim dinleyicisinin teslim kuralı (#137, ADR-009): altı olayın her biri yalnız yayıncının işlemi commit olunca
 * bildirime dönüşür ve bildirim kendi (yeni) işleminde yazılır. Geri alınan işlemden bildirim çıkmaz. Gerçek
 * {@link NotificationListener} ve Spring'in işlem altyapısı kullanılır; {@link NotificationService} sahte, işlem
 * yöneticisi {@link EnrollmentCreatedDeliveryTest.NoOpTransactionManager} (Docker gerekmez).
 *
 * <p>Dinleyicinin {@code @TransactionalEventListener(AFTER_COMMIT)} yerine {@code @EventListener} kullanması ya da
 * {@code REQUIRES_NEW} işlemini kaybetmesi bu testi kırmızıya düşürür; alıcı ve metin kuralları
 * {@code NotificationServiceTest} ve {@code NotificationScenarioIT}'te.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NotificationDeliveryTest.TestConfig.class)
class NotificationDeliveryTest {

    private static final Instant AT = Instant.parse("2026-11-19T12:00:00Z");

    /** Her bildirim çağrısında dinleyicinin işlemi yeni mi (REQUIRES_NEW); işlem yoksa false. */
    private static final List<Boolean> NEW_TRANSACTION = new CopyOnWriteArrayList<>();

    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private NotificationService notifications;

    @BeforeEach
    void setUp() {
        reset(notifications);
        NEW_TRANSACTION.clear();
    }

    @Test
    void sixEvents_committed_eachIsDeliveredOnceAfterCommit_inItsOwnTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publishSixEvents();
            verifyNoInteractions(notifications); // commit öncesi teslim yok
        });

        verify(notifications).notifyEnrollmentCreated(3L, 42L);
        verify(notifications).notifyLessonPublished(3L, 11L);
        verify(notifications).notifyCourseApproved(3L, 7L);
        verify(notifications).notifyCourseRejected(3L, 7L, "Örnekler yetersiz.");
        verify(notifications).notifyQuizCompleted(3L, 8L, 42L, true);
        verify(notifications).notifyCertificateIssued(3L, 42L);
        verifyNoMoreInteractions(notifications);
        assertThat(NEW_TRANSACTION).as("her bildirim yeni işlemde (REQUIRES_NEW)").hasSize(6).containsOnly(true);
    }

    @Test
    void sixEvents_rolledBack_noNotification() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publishSixEvents();
            status.setRollbackOnly();
        });

        verifyNoInteractions(notifications);
    }

    private void publishSixEvents() {
        events.publishEvent(new EnrollmentCreated(99L, 42L, 3L, AT));
        events.publishEvent(new LessonPublished(11L, 3L, AT));
        events.publishEvent(new CourseApproved(3L, 7L, AT));
        events.publishEvent(new CourseRejected(3L, 7L, "Örnekler yetersiz.", AT));
        events.publishEvent(new QuizCompleted(70L, 8L, 3L, 42L, true, AT));
        events.publishEvent(new CertificateIssued(5L, 42L, 3L, AT));
    }

    private static boolean inNewTransaction() {
        try {
            return TransactionAspectSupport.currentTransactionStatus().isNewTransaction();
        } catch (NoTransactionException e) {
            return false;
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new EnrollmentCreatedDeliveryTest.NoOpTransactionManager();
        }

        @Bean
        NotificationService notificationService() {
            return mock(NotificationService.class, invocation -> {
                NEW_TRANSACTION.add(inNewTransaction());
                return null;
            });
        }

        @Bean
        NotificationListener notificationListener(NotificationService notificationService) {
            return new NotificationListener(notificationService);
        }
    }
}
