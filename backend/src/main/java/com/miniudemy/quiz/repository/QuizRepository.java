package com.miniudemy.quiz.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.quiz.domain.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    /** Bölümün sınavları; soru id'leri aynı sorguyla gelir, böylece özetlerdeki soru sayısı ek sorgu istemez. */
    @EntityGraph(attributePaths = "questions")
    List<Quiz> findBySectionIdOrderByIdAsc(Long sectionId);

    /** Verilen bölümlerin sınavları, id sırasıyla (soru listesi yüklenmez). */
    List<Quiz> findBySectionIdInOrderByIdAsc(Collection<Long> sectionIds);

    /**
     * Sınavın herhangi bir denemesi (quiz_attempts) var mı. quiz_attempts FK'si silmeyi engeller; ihlal
     * flush'ta 500'e dönmesin diye silmeden önce biz soruyoruz.
     */
    @Query(value = "select exists (select 1 from quiz_attempts where quiz_id = :quizId)", nativeQuery = true)
    boolean hasAttempts(@Param("quizId") Long quizId);
}
