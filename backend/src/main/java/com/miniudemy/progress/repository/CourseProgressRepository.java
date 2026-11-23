package com.miniudemy.progress.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /**
     * Son açılan dersi yazar; satır yoksa yüzde 0 ile açar (henüz tamamlanan ders yoksa doğru değer). Yüzdeye ve
     * {@code updated_at}'e dokunmaz. Tek ifade: aynı anda gelen istekler UNIQUE (enrollment_id) kısıtına takılmaz.
     */
    @Modifying
    @Query(value = "insert into course_progress (enrollment_id, progress_percent, last_lesson_id, last_accessed_at, "
            + "updated_at) values (:enrollmentId, 0, :lessonId, :openedAt, now()) "
            + "on conflict (enrollment_id) do update set last_lesson_id = excluded.last_lesson_id, "
            + "last_accessed_at = excluded.last_accessed_at", nativeQuery = true)
    void upsertLastLesson(@Param("enrollmentId") Long enrollmentId, @Param("lessonId") Long lessonId,
            @Param("openedAt") Instant openedAt);

    /** Verilen kayıtların yüzde toplamı ve %100 olanların sayısı tek sorguda; satır yoksa (0, 0). */
    @Query("select coalesce(sum(cp.progressPercent), 0) as percentSum, "
            + "coalesce(sum(case when cp.progressPercent >= 100 then 1 else 0 end), 0) as completedCount "
            + "from CourseProgress cp where cp.enrollmentId in :enrollmentIds")
    ProgressTotalsRow totalsByEnrollmentIds(@Param("enrollmentIds") Collection<Long> enrollmentIds);

    interface ProgressTotalsRow {
        BigDecimal getPercentSum();

        long getCompletedCount();
    }

    /**
     * Verilen kayıtların yüzde kovaları tek sorguda: 25 / 50 / 75'in altı ve 100. 0 kovası (yüzde &lt; 25) satırı
     * olmayan kayıtları da içereceği için servis tarafında kayıt sayısından hesaplanır; burada yalnız satırı olanlar
     * sayılır.
     */
    @Query("select coalesce(sum(case when cp.progressPercent < 25 then 1 else 0 end), 0) as below25, "
            + "coalesce(sum(case when cp.progressPercent >= 25 and cp.progressPercent < 50 then 1 else 0 end), 0) "
            + "as from25, "
            + "coalesce(sum(case when cp.progressPercent >= 50 and cp.progressPercent < 75 then 1 else 0 end), 0) "
            + "as from50, "
            + "coalesce(sum(case when cp.progressPercent >= 75 and cp.progressPercent < 100 then 1 else 0 end), 0) "
            + "as from75, "
            + "coalesce(sum(case when cp.progressPercent >= 100 then 1 else 0 end), 0) as completed "
            + "from CourseProgress cp where cp.enrollmentId in :enrollmentIds")
    ProgressBucketsRow bucketsByEnrollmentIds(@Param("enrollmentIds") Collection<Long> enrollmentIds);

    interface ProgressBucketsRow {
        long getBelow25();

        long getFrom25();

        long getFrom50();

        long getFrom75();

        long getCompleted();
    }

    Optional<CourseProgress> findByEnrollmentId(Long enrollmentId);

    List<CourseProgress> findAllByEnrollmentIdIn(Collection<Long> enrollmentIds);

    /** Kursu tamamlamış (%100) kayıt sayısı (platform raporu, #145). */
    @Query("select count(cp) from CourseProgress cp where cp.progressPercent >= 100")
    long countCompleted();
}
