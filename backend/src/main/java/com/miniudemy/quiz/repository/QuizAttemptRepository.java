package com.miniudemy.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.quiz.domain.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    /** Öğrencinin sınavdaki denemeleri, en yeni önce (ix_quiz_attempts_quiz_student); tek sorgu. */
    List<QuizAttempt> findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(Long quizId, Long studentId);
}
