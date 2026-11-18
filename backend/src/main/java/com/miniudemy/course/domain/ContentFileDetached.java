package com.miniudemy.course.domain;

/**
 * Bir kursun ya da dersin dosya bağı koptu: kapak ya da içerik değişti, ders ya da bölüm silindi, dersin içerik
 * türü değişti (Observer, ADR-009). Yayıncı course servisleridir; dinleyicisi content'tir ve dosyayı diskten siler
 * (#112). Dosya artık hiçbir yerde kullanılmadığı için id yeterlidir.
 */
public record ContentFileDetached(Long fileId) {
}
