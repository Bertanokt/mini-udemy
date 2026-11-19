package com.miniudemy.notification.domain;

/** Bildirimin hangi olaydan doğduğu (FR-43). OpenAPI `NotificationType` ve `notifications.type` kısıtıyla aynı. */
public enum NotificationType {
    ENROLLMENT_CREATED,
    LESSON_PUBLISHED,
    COURSE_APPROVED,
    COURSE_REJECTED,
    QUIZ_COMPLETED,
    CERTIFICATE_ISSUED
}
