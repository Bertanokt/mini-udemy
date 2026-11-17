package com.miniudemy.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniudemy.course.domain.Category;

/**
 * `Category` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
