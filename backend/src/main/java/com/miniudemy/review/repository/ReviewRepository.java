package com.miniudemy.review.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.review.domain.Review;

/** `Review` kalıcılığı: tekil değerlendirme kontrolü, görünür yorumlar ve puan toplamları. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    /** Gizli olmayan yorumlar; sıra {@code pageable} ile gelir. */
    Page<Review> findByCourseIdAndHiddenFalse(Long courseId, Pageable pageable);

    /** Bir kursun görünür yorumlarında puan başına sayı tek sorguda; hiç yorumu olmayan puan sonuçta yer almaz. */
    @Query("select r.rating as rating, count(r) as total from Review r "
            + "where r.courseId = :courseId and r.hidden = false group by r.rating")
    List<RatingCount> countVisibleByRating(@Param("courseId") Long courseId);

    /** Verilen kursların görünür yorum ortalaması ve sayısı tek sorguda; yorumu olmayan kurs sonuçta yer almaz. */
    @Query("select r.courseId as courseId, avg(r.rating) as average, count(r) as total from Review r "
            + "where r.courseId in :courseIds and r.hidden = false group by r.courseId")
    List<CourseRatingRow> aggregateVisibleByCourses(@Param("courseIds") Collection<Long> courseIds);

    /** Görünür yorumu olan tüm kursların ortalaması ve sayısı; {@code minRating} filtresi için. */
    @Query("select r.courseId as courseId, avg(r.rating) as average, count(r) as total from Review r "
            + "where r.hidden = false group by r.courseId")
    List<CourseRatingRow> aggregateAllVisible();

    interface RatingCount {
        int getRating();

        long getTotal();
    }

    interface CourseRatingRow {
        Long getCourseId();

        Double getAverage();

        long getTotal();
    }
}
