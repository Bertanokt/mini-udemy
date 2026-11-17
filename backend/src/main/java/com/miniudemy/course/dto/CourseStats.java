package com.miniudemy.course.dto;

/**
 * Kurs kartının başka kaynaklardan gelen sayıları. {@code averageRating} ve {@code reviewCount} review modülü gelince
 * (#126, #143) doldurulur; o zamana dek null ve 0.
 */
public record CourseStats(
        int lessonCount,
        int totalDurationSeconds,
        long enrollmentCount,
        Double averageRating,
        int reviewCount) {
}
