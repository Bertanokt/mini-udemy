package com.miniudemy.enrollment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.enrollment.domain.Enrollment;

/** `Enrollment` kalıcılığı; şimdilik yalnız sayım sorguları (#30 genişletir). */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByCourseId(Long courseId);

    /** Verilen kursların kayıt sayısı tek sorguda; kaydı olmayan kurs sonuçta yer almaz. */
    @Query("select e.courseId as courseId, count(e) as total from Enrollment e "
            + "where e.courseId in :courseIds group by e.courseId")
    List<CourseCount> countByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    interface CourseCount {
        Long getCourseId();

        long getTotal();
    }
}
