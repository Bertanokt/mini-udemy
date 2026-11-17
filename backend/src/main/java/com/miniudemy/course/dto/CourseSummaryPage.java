package com.miniudemy.course.dto;

import java.util.List;

/** Sayfalı kurs özeti listesi (docs/api/openapi.yaml › CourseSummaryPage). {@code page} 0'dan başlar. */
public record CourseSummaryPage(
        List<CourseSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
