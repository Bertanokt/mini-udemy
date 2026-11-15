package com.miniudemy.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.course.domain.Section;

/**
 * `Section` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface SectionRepository extends JpaRepository<Section, Long> {
}
