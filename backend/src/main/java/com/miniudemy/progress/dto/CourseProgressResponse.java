package com.miniudemy.progress.dto;

import java.util.List;

/** Kurs ilerlemesi (docs/api/openapi.yaml › CourseProgressResponse). Yüzdeler 0-100, iki ondalık. */
public record CourseProgressResponse(Long courseId, double progressPercent, List<Long> completedLessonIds,
        List<SectionProgress> sections) {

    /** Bir bölümün yüzdesi; kurs yüzdesiyle aynı kural. */
    public record SectionProgress(Long sectionId, double progressPercent) {
    }
}
