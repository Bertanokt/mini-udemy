package com.miniudemy.course.dto;

/** Bir kategorideki kurs sayısı (platform raporu, #145). */
public record CategoryCourseCount(Long categoryId, String name, long courseCount) {
}
