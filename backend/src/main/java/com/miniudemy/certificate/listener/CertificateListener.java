package com.miniudemy.certificate.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.miniudemy.certificate.service.CertificateIssuingService;
import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.quiz.domain.QuizCompleted;

/**
 * Ders tamamlanınca ya da sınav geçilince sertifika koşulunu kontrol eder (Observer, ADR-009; #129). Yayıncının
 * işlemi commit edildikten sonra çalışır (tamamlama ve deneme veritabanında görünür) ve kendi işlemini açar.
 */
@Component
public class CertificateListener {

    private final CertificateIssuingService issuing;

    public CertificateListener(CertificateIssuingService issuing) {
        this.issuing = issuing;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonCompleted(LessonCompleted event) {
        issuing.issueIfEligible(event.studentId(), event.courseId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /** Kalınan deneme koşulu sağlatamaz; yalnız geçen denemede kontrol edilir. */
    public void onQuizCompleted(QuizCompleted event) {
        if (event.passed()) {
            issuing.issueIfEligible(event.studentId(), event.courseId());
        }
    }
}
