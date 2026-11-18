package com.miniudemy.course.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.course.domain.Lesson;

/**
 * `Lesson` kalıcılığı. Yalnız Spring Data arayüzü; sorguları ihtiyaç duyan issue ekler.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Verilen kursların ders sayısı ve toplam süresi tek sorguda; dersi olmayan kurs sonuçta yer almaz. */
    @Query("select s.course.id as courseId, count(l) as lessonCount, coalesce(sum(l.durationSeconds), 0) as totalDurationSeconds "
            + "from Lesson l join l.section s where s.course.id in :courseIds group by s.course.id")
    List<CourseTotals> totalsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    interface CourseTotals {
        Long getCourseId();

        long getLessonCount();

        long getTotalDurationSeconds();
    }

    /** Dersin bağlı olduğu kursun id'si (#41). */
    @Query("select l.section.course.id from Lesson l where l.id = :lessonId")
    Optional<Long> findCourseIdByLessonId(@Param("lessonId") Long lessonId);
}
