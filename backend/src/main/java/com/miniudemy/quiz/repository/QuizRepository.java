package com.miniudemy.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.quiz.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /**
     * Sınavın herhangi bir denemesi (quiz_attempts) var mı. quiz_attempts FK'si silmeyi engeller; ihlal
     * flush'ta 500'e dönmesin diye silmeden önce biz soruyoruz.
     */
    @Query(value = "select exists (select 1 from quiz_attempts where quiz_id = :quizId)", nativeQuery = true)
    boolean hasAttempts(@Param("quizId") Long quizId);
}
