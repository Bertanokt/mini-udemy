package com.miniudemy.certificate.listener;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.miniudemy.certificate.service.CertificateIssuingService;
import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.quiz.domain.QuizCompleted;

/** Dinleyici olayı sertifika servisine iletir (#129). Commit'ten sonra teslim kuralı ADR-009 testlerinde. */
class CertificateListenerTest {

    private static final Instant AT = Instant.parse("2026-11-19T12:00:00Z");

    private final CertificateIssuingService issuing = mock(CertificateIssuingService.class);
    private final CertificateListener listener = new CertificateListener(issuing);

    @Test
    void lessonCompleted_checksTheCertificateCondition() {
        listener.onLessonCompleted(new LessonCompleted(42L, 3L, 11L, AT));

        verify(issuing).issueIfEligible(42L, 3L);
    }

    @Test
    void passedQuiz_checksTheCertificateCondition() {
        listener.onQuizCompleted(new QuizCompleted(70L, 8L, 3L, 42L, true, AT));

        verify(issuing).issueIfEligible(42L, 3L);
    }

    @Test
    void failedQuiz_cannotMakeTheStudentEligible_soNothingIsChecked() {
        listener.onQuizCompleted(new QuizCompleted(70L, 8L, 3L, 42L, false, AT));

        verify(issuing, never()).issueIfEligible(anyLong(), anyLong());
    }
}
