package com.miniudemy.course.dto;

import java.util.List;

/** Bölüm ve dersleri, `position` sırasıyla (docs/api/openapi.yaml › SectionResponse). */
public record SectionResponse(Long id, Long courseId, String title, int position, List<LessonResponse> lessons) {
}
