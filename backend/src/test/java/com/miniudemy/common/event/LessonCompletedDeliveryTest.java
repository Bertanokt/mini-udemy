package com.miniudemy.common.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.progress.listener.ProgressListener;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.LessonProgressRepository;
import com.miniudemy.progress.service.ProgressService;

/**
 * ProgressListener (#43): LessonCompleted elle yayınlanır (ders tamamlama ucu #41'de). Gerçek dinleyici ve
 * ProgressService, Spring'in işlem altyapısıyla çalışır; repository'ler taklittir, Docker gerekmez. 20 dersin 15'i
 * tamamlandığında course_progress'e 75 yazılır; yayıncı rollback olursa hiçbir şey yazılmaz.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = LessonCompletedDeliveryTest.TestConfig.class)
class LessonCompletedDeliveryTest {

    private static final LessonCompleted EVENT = new LessonCompleted(42L, 7L, 205L,
            Instant.parse("2026-11-17T10:00:00Z"));

    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CourseProgressRepository courseProgress;
    @Autowired
    private LessonProgressRepository lessonProgress;
    @Autowired
    private EnrollmentService enrollments;
    @Autowired
    private CourseStructureService structure;

    @BeforeEach
    void setUp() {
        reset(courseProgress, lessonProgress, enrollments, structure);
        given(enrollments.requireEnrollmentId(42L, 7L)).willReturn(9L);
        given(structure.sectionLessons(7L)).willReturn(List.of(
                new SectionLessons(1L, ids(101, 110)), new SectionLessons(2L, ids(201, 210))));
        given(lessonProgress.findLessonIdsByEnrollmentId(9L)).willReturn(
                List.of(101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 201L, 202L, 203L, 204L, 205L));
    }

    private static List<Long> ids(long from, long to) {
        return LongStream.rangeClosed(from, to).boxed().toList();
    }

    @Test
    void lessonCompleted_afterCommit_updatesCourseProgressTo75() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(EVENT);
            verify(courseProgress, never()).upsertPercent(anyLong(), any());
        });

        verify(courseProgress).upsertPercent(9L, BigDecimal.valueOf(75.0));
    }

    @Test
    void lessonCompleted_publisherRolledBack_writesNothing() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(EVENT);
            status.setRollbackOnly();
        });

        verify(courseProgress, never()).upsertPercent(anyLong(), any());
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new SuspendableTransactionManager();
        }

        @Bean
        CourseProgressRepository courseProgressRepository() {
            return mock(CourseProgressRepository.class);
        }

        @Bean
        LessonProgressRepository lessonProgressRepository() {
            return mock(LessonProgressRepository.class);
        }

        @Bean
        EnrollmentService enrollmentService() {
            return mock(EnrollmentService.class);
        }

        @Bean
        CourseStructureService courseStructureService() {
            return mock(CourseStructureService.class);
        }

        @Bean
        ProgressService progressService(CourseStructureService structure, EnrollmentService enrollments,
                LessonProgressRepository lessonProgress, CourseProgressRepository courseProgress) {
            return new ProgressService(structure, enrollments, lessonProgress, courseProgress);
        }

        @Bean
        ProgressListener progressListener(ProgressService service) {
            return new ProgressListener(service);
        }
    }

    /** Dinleyici REQUIRES_NEW açtığı için işlemi askıya alabilen sahte yönetici. */
    static class SuspendableTransactionManager extends EnrollmentCreatedDeliveryTest.NoOpTransactionManager {
        @Override
        protected Object doSuspend(Object transaction) {
            return transaction;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
        }
    }
}
