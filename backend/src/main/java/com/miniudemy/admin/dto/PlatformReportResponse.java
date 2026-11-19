package com.miniudemy.admin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Platform raporu (docs/api/openapi.yaml › PlatformReportResponse, #145). {@code from}/{@code to} her zaman null:
 * tarih aralığı 17.11.2026 İnce kapsam kararıyla uygulanmadı, sayılar tüm zamanlarındır.
 */
public record PlatformReportResponse(
        LocalDate from,
        LocalDate to,
        Map<String, Long> usersByRole,
        Map<String, Long> coursesByStatus,
        long enrollmentCount,
        long completionCount,
        long certificateCount,
        List<TopCourse> topCourses,
        List<CategoryCount> categoryDistribution) {

    /** En çok kayıt alan kurslardan biri. */
    public record TopCourse(Long courseId, String title, long enrollmentCount) {
    }

    /** Bir kategorideki kurs sayısı. */
    public record CategoryCount(Long categoryId, String name, long courseCount) {
    }
}
