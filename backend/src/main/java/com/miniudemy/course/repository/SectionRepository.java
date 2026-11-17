package com.miniudemy.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.course.domain.Section;

/**
 * `Section` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface SectionRepository extends JpaRepository<Section, Long> {

    /** Kursun bölümleri ve dersleri tek sorguda, bölüm ve ders `position` sırasıyla (N+1 yok). */
    @Query("select distinct s from Section s left join fetch s.lessons l "
            + "where s.course.id = :courseId order by s.position, l.position")
    List<Section> findAllWithLessonsByCourseId(@Param("courseId") Long courseId);

    /** Bölümün ait olduğu kursun id'si; bölümü ve kursu yüklemeden. */
    @Query("select s.course.id from Section s where s.id = :sectionId")
    Optional<Long> findCourseIdById(@Param("sectionId") Long sectionId);
}
