package com.miniudemy.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.enrollment.mapper.EnrollmentMapperImpl;
import com.miniudemy.enrollment.repository.EnrollmentRepository;
import com.miniudemy.enrollment.service.EnrollmentService;

/**
 * Observer teslim kuralı (#42, ADR-009): olay yalnız işlem commit olunca dinleyiciye ulaşır. Gerçek
 * EnrollmentService, gerçek ApplicationEventPublisher ve Spring'in işlem altyapısı kullanılır; veritabanı yerine
 * işlemi yalnızca kaydeden bir transaction manager vardır, bu yüzden Docker gerekmez.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EnrollmentCreatedDeliveryTest.TestConfig.class)
class EnrollmentCreatedDeliveryTest {

    private static final Instant ENROLLED_AT = Instant.parse("2026-11-17T10:00:00Z");

    @Autowired
    private EnrollmentService service;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RecordingListener listener;
    @Autowired
    private EnrollmentRepository repository;

    @BeforeEach
    void setUp() {
        reset(repository);
        listener.received.clear();
        given(repository.saveAndFlush(any(Enrollment.class))).willAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 99L);
            ReflectionTestUtils.setField(e, "enrolledAt", ENROLLED_AT);
            return e;
        });
    }

    @Test
    void enroll_deliversEventOnlyAfterCommit() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.enroll(5L, 7L);
            assertThat(listener.received).as("commit öncesi teslim yok").isEmpty();
        });

        assertThat(listener.received).containsExactly(new EnrollmentCreated(99L, 7L, 5L, ENROLLED_AT));
    }

    @Test
    void enroll_rolledBackByCaller_doesNotDeliverEvent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.enroll(5L, 7L);
            status.setRollbackOnly();
        });

        assertThat(listener.received).isEmpty();
    }

    @Test
    void enroll_failure_doesNotDeliverEvent() {
        given(repository.existsByStudentIdAndCourseId(7L, 5L)).willReturn(true);

        assertThatThrownBy(() -> service.enroll(5L, 7L)).isInstanceOf(ConflictException.class);

        assertThat(listener.received).isEmpty();
    }

    /** Test dinleyicisi: üretimdeki dinleyiciler de aynı anotasyonu kullanır. */
    static class RecordingListener {
        final List<EnrollmentCreated> received = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void on(EnrollmentCreated event) {
            received.add(event);
        }
    }

    /** İşlemi yalnızca başlatıp bitiren, senkronizasyonları (AFTER_COMMIT dahil) çalıştıran sahte yönetici. */
    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        private static final Object ACTIVE = new Object();
        private static final Object NONE = new Object();
        private final ThreadLocal<Object> active = ThreadLocal.withInitial(() -> NONE);

        @Override
        protected Object doGetTransaction() {
            return active.get();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return transaction == ACTIVE;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active.set(ACTIVE);
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active.remove();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        EnrollmentRepository enrollmentRepository() {
            return mock(EnrollmentRepository.class);
        }

        @Bean
        CourseLookupService courseLookupService() {
            CourseLookupService lookup = mock(CourseLookupService.class);
            given(lookup.findSummaries(any())).willReturn(Map.of());
            return lookup;
        }

        @Bean
        EnrollmentService enrollmentService(EnrollmentRepository repository, CourseLookupService lookup,
                ApplicationEventPublisher events) {
            return new EnrollmentService(repository, lookup, new EnrollmentMapperImpl(), events,
                    enrollmentIds -> java.util.Map.of());
        }

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }
}
