package com.miniudemy.course.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;

/**
 * `Course` kalıcılığı. Yalnız Spring Data arayüzü.
 *
 * Katalog sorguları yalnız Yayımlandı kursları döndürür; filtreler VE ile birleşir, boş (null) filtre uygulanmaz.
 * {@code pattern} küçük harfe çevrilmiş, `%`/`_`/`\` kaçırılmış ve `%` ile sarılmış arama ifadesidir (boşsa `%`):
 * böylece `like` parametresi hiç null olmaz.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Kategori herhangi bir kursta (durumu ne olursa olsun) kullanılıyor mu; kategori silme kuralı (#118). */
    boolean existsByCategoryId(Long categoryId);

    String CATALOG_FILTER = """
            from Course c
            where c.status = com.miniudemy.course.domain.CourseStatus.PUBLISHED
              and (:categoryId is null or c.category.id = :categoryId)
              and (:level is null or c.level = :level)
              and (:instructorId is null or c.instructorId = :instructorId)
              and (lower(c.title) like :pattern escape '\\' or lower(c.description) like :pattern escape '\\')
            """;

    /** Sıralama ve sayfalama {@code pageable}'dan gelir; kategori aynı sorguda yüklenir (kart başına ek sorgu yok). */
    @EntityGraph(attributePaths = "category")
    @Query(value = "select c " + CATALOG_FILTER, countQuery = "select count(c) " + CATALOG_FILTER)
    Page<Course> searchCatalog(@Param("pattern") String pattern, @Param("categoryId") Long categoryId,
            @Param("level") CourseLevel level, @Param("instructorId") Long instructorId, Pageable pageable);

    /** Aynı filtrenin yalnız id'leri; kayıt sayısına göre sıralamada sıra bellekte kurulduğu için gerekir. */
    @Query("select c.id " + CATALOG_FILTER)
    List<Long> searchCatalogIds(@Param("pattern") String pattern, @Param("categoryId") Long categoryId,
            @Param("level") CourseLevel level, @Param("instructorId") Long instructorId);

    @EntityGraph(attributePaths = "category")
    List<Course> findAllByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = "category")
    /** Yönetici listesi (#59): verilen durumdaki kurslar; durum {@code null} ise hepsi. */
    @Query(value = "select c from Course c where (:status is null or c.status = :status)",
            countQuery = "select count(c) from Course c where (:status is null or c.status = :status)")
    Page<Course> findAllByOptionalStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query("select c from Course c where c.id = :id")
    Optional<Course> findWithCategoryById(@Param("id") Long id);
}
