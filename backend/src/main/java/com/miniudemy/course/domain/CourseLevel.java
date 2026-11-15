package com.miniudemy.course.domain;

/**
 * Kurs seviyesi. Değerler `courses.level` kolonundaki CHECK kısıtıyla ({@code ck_courses_level}) aynıdır;
 * birine değer eklenirse diğeri de aynı PR'da güncellenir.
 */
public enum CourseLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
