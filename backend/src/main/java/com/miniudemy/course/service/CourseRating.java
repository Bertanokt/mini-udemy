package com.miniudemy.course.service;

/** Bir kursun görünür yorumlarının ortalaması (bir ondalık) ve sayısı; yorum yoksa ortalama null, sayı 0. */
public record CourseRating(Double averageRating, long reviewCount) {
}
