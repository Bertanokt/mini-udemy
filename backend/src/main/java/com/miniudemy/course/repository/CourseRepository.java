package com.miniudemy.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.course.domain.Course;

/**
 * `Course` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {
}
