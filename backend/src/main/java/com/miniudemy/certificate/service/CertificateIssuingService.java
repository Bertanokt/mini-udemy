package com.miniudemy.certificate.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.certificate.domain.CertificateIssued;
import com.miniudemy.certificate.repository.CertificateRepository;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.service.ProgressService;
import com.miniudemy.quiz.service.QuizRequirementService;

/**
 * Sertifika verme (#129, proje dokümanı H). Kural: kursun bütün zorunlu dersleri tamamlanmış ve bütün zorunlu
 * sınavları geçilmiş kursiyere, o kurs için tek sertifika.
 */
@Service
public class CertificateIssuingService {

    /** Doğrulama kodu çakışırsa yeni kodla en fazla bu kadar denenir. */
    static final int MAX_CODE_ATTEMPTS = 5;

    private final EnrollmentService enrollments;
    private final CourseStructureService structure;
    private final ProgressService progress;
    private final QuizRequirementService quizzes;
    private final CertificateRepository certificates;
    private final VerificationCodeGenerator codes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CertificateIssuingService(EnrollmentService enrollments, CourseStructureService structure,
            ProgressService progress, QuizRequirementService quizzes, CertificateRepository certificates,
            VerificationCodeGenerator codes, ApplicationEventPublisher events, Clock clock) {
        this.enrollments = enrollments;
        this.structure = structure;
        this.progress = progress;
        this.quizzes = quizzes;
        this.certificates = certificates;
        this.codes = codes;
        this.events = events;
        this.clock = clock;
    }

    /** Koşullar sağlanıyorsa sertifikayı verir ve id'sini döner; sağlanmıyorsa ya da zaten varsa boş. */
    @Transactional
    public Optional<Long> issueIfEligible(Long studentId, Long courseId) {
        Long enrollmentId = enrollments.requireEnrollmentId(studentId, courseId);
        if (certificates.existsByEnrollmentId(enrollmentId) || !eligible(studentId, courseId)) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            if (certificates.insertIfAbsent(enrollmentId, codes.next(), now) == 1) {
                Long certificateId = certificates.findByEnrollmentId(enrollmentId).orElseThrow().getId();
                /* KIRMIZI K1 */ if (false) events.publishEvent(new CertificateIssued(certificateId, studentId, courseId, now));
                return Optional.of(certificateId);
            }
            if (certificates.existsByEnrollmentId(enrollmentId)) {
                // Aynı anda gelen başka bir olay sertifikayı yazdı; olayı o yayınladı.
                return Optional.empty();
            }
            // Kalan tek neden: doğrulama kodu başka bir sertifikada var. Yeni kodla tekrar dene.
        }
        throw new IllegalStateException("Sertifika için benzersiz doğrulama kodu üretilemedi.");
    }

    private boolean eligible(Long studentId, Long courseId) {
        Set<Long> mandatory = structure.mandatoryLessonIds(courseId);
        return progress.completedLessonIds(studentId, courseId).containsAll(mandatory)
                && quizzes.allRequiredQuizzesPassed(studentId, courseId);
    }
}
