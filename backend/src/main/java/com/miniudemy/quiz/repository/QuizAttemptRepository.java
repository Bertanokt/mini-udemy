package com.miniudemy.quiz.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.quiz.domain.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Öğrencinin sınavdaki denemeleri, en yeni önce (ix_quiz_attempts_quiz_student); tek sorgu. */
    List<QuizAttempt> findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(Long quizId, Long studentId);

    /** Sınav başına deneme sayısı ve geçen deneme sayısı tek sorguda; denemesi olmayan sınav sonuçta yer almaz. */
    @Query("select a.quizId as quizId, count(a) as attemptCount, "
            + "sum(case when a.passed = true then 1 else 0 end) as passedCount "
            + "from QuizAttempt a where a.quizId in :quizIds group by a.quizId")
    List<QuizCountRow> countByQuizIds(@Param("quizIds") Collection<Long> quizIds);

    /**
     * Kursiyer ve sınav başına en yüksek yüzde ve herhangi bir denemenin geçip geçmediği (1/0), tek sorguda.
     * Yüzde, {@link QuizAttempt#getPercent()} ile aynı kural: en yüksek puan 0 ise 0.
     */
    @Query(value = "select student_id as studentId, quiz_id as quizId, "
            + "max(case when max_score = 0 then 0 else score * 100 / max_score end) as bestPercent, "
            + "max(case when passed then 1 else 0 end) as passed "
            + "from quiz_attempts where quiz_id in (:quizIds) and student_id in (:studentIds) "
            + "group by student_id, quiz_id order by student_id, quiz_id", nativeQuery = true)
    List<BestResultRow> bestResults(@Param("quizIds") Collection<Long> quizIds,
            @Param("studentIds") Collection<Long> studentIds);

    interface QuizCountRow {
        Long getQuizId();

        long getAttemptCount();

        long getPassedCount();
    }

    interface BestResultRow {
        Long getStudentId();

        Long getQuizId();

        BigDecimal getBestPercent();

        Integer getPassed();
    }
}
