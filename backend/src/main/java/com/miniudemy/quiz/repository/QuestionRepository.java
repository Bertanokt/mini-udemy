package com.miniudemy.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.quiz.domain.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByCourseIdOrderByIdAsc(Long courseId);

    /** Soru bir sınavda (quiz_questions) kullanılıyor mu. */
    @Query(value = "select exists (select 1 from quiz_questions where question_id = :questionId)",
            nativeQuery = true)
    boolean isUsedInQuiz(@Param("questionId") Long questionId);

    /** Soru için herhangi bir denemede cevap kaydı (attempt_answers) var mı. */
    @Query(value = "select exists (select 1 from attempt_answers where question_id = :questionId)",
            nativeQuery = true)
    boolean hasAttemptAnswers(@Param("questionId") Long questionId);
}
