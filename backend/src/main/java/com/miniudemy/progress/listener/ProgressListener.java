package com.miniudemy.progress.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.miniudemy.progress.domain.LessonCompleted;
import com.miniudemy.progress.service.ProgressService;

/** Ders tamamlanınca kaydın kurs yüzdesini günceller (Observer, ADR-009). */
@Component
public class ProgressListener {

    private final ProgressService progressService;

    public ProgressListener(ProgressService progressService) {
        this.progressService = progressService;
    }

    /** AFTER_COMMIT'te yayıncının işlemi bitmiştir; yazma için kendi işlemini açar. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLessonCompleted(LessonCompleted event) {
        progressService.refreshCourseProgress(event.studentId(), event.courseId());
    }
}
