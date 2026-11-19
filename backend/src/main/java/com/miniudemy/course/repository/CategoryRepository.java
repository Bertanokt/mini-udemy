package com.miniudemy.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.miniudemy.course.domain.Category;

/**
 * `Category` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Her kategori ve kurs sayısı (platform raporu, #145); kursu olmayan kategori 0 ile gelir. */
    @Query("select cat.id as categoryId, cat.name as name, "
            + "(select count(c) from Course c where c.category = cat) as total from Category cat")
    List<CategoryCount> countCourses();

    interface CategoryCount {
        Long getCategoryId();

        String getName();

        long getTotal();
    }
}
