package com.miniudemy.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.miniudemy.certificate.domain.Certificate;
import com.miniudemy.certificate.domain.CertificateIssued;
import com.miniudemy.certificate.repository.CertificateRepository;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.service.ProgressService;
import com.miniudemy.quiz.service.QuizRequirementService;

/**
 * Sertifika verme (#129): bütün zorunlu dersler tamamlanmış ve bütün zorunlu sınavlar geçilmişse kayıt başına tek
 * sertifika, tahmin edilemez kod ve {@link CertificateIssued}. Tekillik veritabanının ON CONFLICT DO NOTHING ekleme
 * komutundadır (eklenen satır 1 ya da 0).
 */
class CertificateIssuingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final long STUDENT = 42L;
    private static final long COURSE = 3L;
    private static final long ENROLLMENT = 500L;

    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final CourseStructureService structure = mock(CourseStructureService.class);
    private final ProgressService progress = mock(ProgressService.class);
    private final QuizRequirementService quizzes = mock(QuizRequirementService.class);
    private final CertificateRepository certificates = mock(CertificateRepository.class);
    private final VerificationCodeGenerator codes = mock(VerificationCodeGenerator.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private CertificateIssuingService service;

    @BeforeEach
    void setUp() {
        service = new CertificateIssuingService(enrollments, structure, progress, quizzes, certificates, codes, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willReturn(ENROLLMENT);
        given(structure.mandatoryLessonIds(COURSE)).willReturn(Set.of(11L, 12L));
        given(codes.next()).willReturn("K7Q2-M9XA-4T", "ABCD-EFGH-JK", "MNPQ-RSTU-VW");
    }

    private void allConditionsMet() {
        given(progress.completedLessonIds(STUDENT, COURSE)).willReturn(Set.of(11L, 12L, 13L));
        given(quizzes.allRequiredQuizzesPassed(STUDENT, COURSE)).willReturn(true);
    }

    private static Certificate saved(long id) {
        Certificate certificate = mock(Certificate.class);
        given(certificate.getId()).willReturn(id);
        return certificate;
    }

    @Test
    void allMandatoryLessonsAndRequiredQuizzesDone_issuesCertificate_andPublishesCertificateIssued() {
        allConditionsMet();
        given(certificates.insertIfAbsent(ENROLLMENT, "K7Q2-M9XA-4T", NOW)).willReturn(1);
        Certificate certificate = saved(900L);
        given(certificates.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.of(certificate));

        assertThat(service.issueIfEligible(STUDENT, COURSE)).contains(900L);

        verify(certificates).insertIfAbsent(ENROLLMENT, "K7Q2-M9XA-4T", NOW);
        verify(events).publishEvent(new CertificateIssued(900L, STUDENT, COURSE, NOW));
    }

    @Test
    void mandatoryLessonNotCompleted_issuesNothing() {
        given(progress.completedLessonIds(STUDENT, COURSE)).willReturn(Set.of(11L, 13L));
        given(quizzes.allRequiredQuizzesPassed(STUDENT, COURSE)).willReturn(true);

        assertThat(service.issueIfEligible(STUDENT, COURSE)).isEmpty();

        verify(certificates, never()).insertIfAbsent(anyLong(), anyString(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void optionalLessonsAreNotNeeded() {
        // 13 isteğe bağlı: zorunlu 11 ve 12 yeter.
        given(progress.completedLessonIds(STUDENT, COURSE)).willReturn(Set.of(11L, 12L));
        given(quizzes.allRequiredQuizzesPassed(STUDENT, COURSE)).willReturn(true);
        given(certificates.insertIfAbsent(ENROLLMENT, "K7Q2-M9XA-4T", NOW)).willReturn(1);
        Certificate certificate = saved(900L);
        given(certificates.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.of(certificate));

        assertThat(service.issueIfEligible(STUDENT, COURSE)).contains(900L);
    }

    @Test
    void requiredQuizNotPassed_issuesNothing() {
        given(progress.completedLessonIds(STUDENT, COURSE)).willReturn(Set.of(11L, 12L));
        given(quizzes.allRequiredQuizzesPassed(STUDENT, COURSE)).willReturn(false);

        assertThat(service.issueIfEligible(STUDENT, COURSE)).isEmpty();

        verify(certificates, never()).insertIfAbsent(anyLong(), anyString(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void alreadyHasCertificate_issuesNoSecondOne_andPublishesNothing() {
        given(certificates.existsByEnrollmentId(ENROLLMENT)).willReturn(true);
        allConditionsMet();

        assertThat(service.issueIfEligible(STUDENT, COURSE)).isEmpty();

        verify(certificates, never()).insertIfAbsent(anyLong(), anyString(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void concurrentIssueWonByTheOtherEvent_insertsNothing_andPublishesNothing() {
        allConditionsMet();
        // Kontrolden sonra başka bir olay sertifikayı yazdı: ekleme 0 döner, kayıt artık sertifikalı.
        given(certificates.existsByEnrollmentId(ENROLLMENT)).willReturn(false, true);
        given(certificates.insertIfAbsent(ENROLLMENT, "K7Q2-M9XA-4T", NOW)).willReturn(0);

        assertThat(service.issueIfEligible(STUDENT, COURSE)).isEmpty();

        verify(certificates, times(1)).insertIfAbsent(anyLong(), anyString(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void verificationCodeAlreadyUsed_retriesWithANewCode() {
        allConditionsMet();
        given(certificates.insertIfAbsent(ENROLLMENT, "K7Q2-M9XA-4T", NOW)).willReturn(0);
        given(certificates.insertIfAbsent(ENROLLMENT, "ABCD-EFGH-JK", NOW)).willReturn(1);
        Certificate certificate = saved(901L);
        given(certificates.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.of(certificate));

        assertThat(service.issueIfEligible(STUDENT, COURSE)).contains(901L);

        verify(certificates).insertIfAbsent(ENROLLMENT, "ABCD-EFGH-JK", NOW);
        verify(events).publishEvent(new CertificateIssued(901L, STUDENT, COURSE, NOW));
    }

    @Test
    void verificationCodeKeepsColliding_givesUpAfterTheLimit() {
        allConditionsMet();
        given(certificates.insertIfAbsent(anyLong(), anyString(), any())).willReturn(0);

        assertThatThrownBy(() -> service.issueIfEligible(STUDENT, COURSE)).isInstanceOf(IllegalStateException.class);

        verify(certificates, times(CertificateIssuingService.MAX_CODE_ATTEMPTS))
                .insertIfAbsent(anyLong(), anyString(), any());
        verify(events, never()).publishEvent(any(Object.class));
    }
}
