package com.miniudemy.progress.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.progress.domain.LessonProgress;

/** `LessonProgress` kalıcılığı. */
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    @Query("select lp.lessonId from LessonProgress lp where lp.enrollmentId = :enrollmentId")
    List<Long> findLessonIdsByEnrollmentId(@Param("enrollmentId") Long enrollmentId);
}
