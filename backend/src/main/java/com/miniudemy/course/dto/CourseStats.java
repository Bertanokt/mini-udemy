package com.miniudemy.course.dto;

/**
 * Kurs kartının başka kaynaklardan gelen sayıları. {@code averageRating} ve {@code reviewCount} review modülünden
 * gelir (#143); değerlendirmesi olmayan kursta null ve 0.
 */
public record CourseStats(
        int lessonCount,
        int totalDurationSeconds,
        long enrollmentCount,
        Double averageRating,
        int reviewCount) {
}
