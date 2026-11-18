package com.miniudemy.progress.repository;

import java.time.Instant;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.miniudemy.progress.domain.LessonProgress;

/** `LessonProgress` kalıcılığı. */
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    @Query("select lp.lessonId from LessonProgress lp where lp.enrollmentId = :enrollmentId")
    List<Long> findLessonIdsByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    /**
     * Dersi tamamlandı olarak ekler; zaten varsa hiçbir şey yapmaz (#41). Eklenen satır sayısını döner: 1 = ilk
     * tamamlama, 0 = zaten tamamlanmış. Tekillik veritabanında ({@code uk_lesson_progress_enrollment_lesson}) tek komutla
     * sağlanır: aynı anda gelen iki istekte biri 1, diğeri 0 alır; istisna oluşmadığı için transaction bozulmaz.
     */
    @Modifying
    @Query(value = "insert into lesson_progress (enrollment_id, lesson_id, completed_at) values (:enrollmentId, :lessonId, :completedAt) "
            + "on conflict (enrollment_id, lesson_id) do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("enrollmentId") Long enrollmentId, @Param("lessonId") Long lessonId,
            @Param("completedAt") Instant completedAt);
}
