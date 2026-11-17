package com.miniudemy.course.dto;

import com.miniudemy.course.domain.LessonContentType;

/** Ders (docs/api/openapi.yaml › LessonResponse). İçerik dosyası yanıtta yok; yalnız yüklü olup olmadığı. */
public record LessonResponse(
        Long id,
        Long sectionId,
        String title,
        LessonContentType contentType,
        boolean mandatory,
        int durationSeconds,
        int position,
        boolean hasContent) {
}
