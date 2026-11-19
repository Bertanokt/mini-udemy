package com.miniudemy.enrollment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.enrollment.domain.Enrollment;

/** `Enrollment` kalıcılığı: kayıt var mı, kursiyerin kayıtları ve kurs başına sayım. */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByCourseId(Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("select e.id from Enrollment e where e.studentId = :studentId and e.courseId = :courseId")
    Optional<Long> findIdByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("select e.id from Enrollment e where e.courseId = :courseId")
    List<Long> findIdsByCourseId(@Param("courseId") Long courseId);

    @Query("select e.studentId from Enrollment e where e.courseId = :courseId order by e.studentId")
    List<Long> findStudentIdsByCourseId(@Param("courseId") Long courseId);

    /** En son kaydolunan önce; aynı anda oluşanlarda id'ye göre, böylece sıra kararlıdır. */
    List<Enrollment> findAllByStudentIdOrderByEnrolledAtDescIdDesc(Long studentId);

    /** Kursun kayıtları; sıra {@code pageable} ile gelir (eğitmenin kursiyer listesi). */
    Page<Enrollment> findByCourseId(Long courseId, Pageable pageable);

    /** Verilen kursların kayıt sayısı tek sorguda; kaydı olmayan kurs sonuçta yer almaz. */
    @Query("select e.courseId as courseId, count(e) as total from Enrollment e "
            + "where e.courseId in :courseIds group by e.courseId")
    List<CourseCount> countByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    interface CourseCount {
        Long getCourseId();

        long getTotal();
    }

    /** En çok kayıt alan kurslar, çoktan aza; eşitlikte küçük kurs id'si önce (platform raporu, #145). */
    @Query("select e.courseId as courseId, count(e) as total from Enrollment e "
            + "group by e.courseId order by count(e) desc, e.courseId")
    List<CourseCount> findTopCourses(Pageable pageable);
}
