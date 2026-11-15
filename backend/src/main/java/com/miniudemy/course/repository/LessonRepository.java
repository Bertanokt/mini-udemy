package com.miniudemy.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.course.domain.Lesson;

/**
 * `Lesson` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {
}
