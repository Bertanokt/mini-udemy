package com.miniudemy.course.dto;

import java.util.List;

/** Sayfalı kursiyer listesi (docs/api/openapi.yaml › CourseStudentPage). {@code page} 0'dan başlar. */
public record CourseStudentPage(
        List<CourseStudentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
