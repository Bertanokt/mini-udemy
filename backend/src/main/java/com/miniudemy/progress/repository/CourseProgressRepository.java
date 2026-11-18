package com.miniudemy.progress.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.progress.domain.CourseProgress;

/** `CourseProgress` kalıcılığı. */
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    /**
     * Kaydın kurs yüzdesini yazar; satır yoksa açar. Tek ifade olduğu için aynı anda gelen iki tamamlama
     * UNIQUE (enrollment_id) kısıtına takılmaz. Son izlenen ders alanlarına dokunmaz.
     */
    @Modifying
    @Query(value = "insert into course_progress (enrollment_id, progress_percent, updated_at) "
            + "values (:enrollmentId, :percent, now()) "
            + "on conflict (enrollment_id) do update set progress_percent = excluded.progress_percent, "
            + "updated_at = now()", nativeQuery = true)
    void upsertPercent(@Param("enrollmentId") Long enrollmentId, @Param("percent") BigDecimal percent);
}
