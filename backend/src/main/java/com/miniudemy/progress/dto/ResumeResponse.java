package com.miniudemy.progress.dto;

import java.time.Instant;

/**
 * Kaldığı yerden devam (docs/api/openapi.yaml › ResumeResponse, #119). Kursta ders yoksa {@code lessonId},
 * {@code lessonTitle} ve {@code sectionId} null; hiç ders açılmamışsa {@code lastAccessedAt} null.
 */
public record ResumeResponse(Long courseId, Long lessonId, String lessonTitle, Long sectionId, double progressPercent,
        Instant lastAccessedAt) {
}
