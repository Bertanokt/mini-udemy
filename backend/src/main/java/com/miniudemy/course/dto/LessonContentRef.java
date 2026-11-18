package com.miniudemy.course.dto;

import com.miniudemy.course.domain.LessonContentType;

/**
 * Bir dersin içeriğine erişim ve sunum için başka modüllerin (content, #113) ihtiyaç duyduğu bilgiler; entity değil.
 *
 * @param contentFileId içeriğin {@code stored_files} id'si; içerik yüklenmemişse {@code null}
 */
public record LessonContentRef(Long lessonId, Long courseId, Long instructorId, LessonContentType contentType,
        Long contentFileId) {
}
