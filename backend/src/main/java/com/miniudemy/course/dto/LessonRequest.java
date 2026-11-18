package com.miniudemy.course.dto;

import com.miniudemy.course.domain.LessonContentType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ders ekleme ve güncelleme isteği (docs/api/openapi.yaml › LessonRequest). İçerik dosyası bu istekte değil, ayrı
 * yükleme ucuyla bağlanır; sıra ({@code position}) de istekte değil, ekleme ve taşıma uçlarında belirlenir.
 */
public record LessonRequest(
        @NotBlank(message = "Başlık zorunludur.")
        @Size(max = 200, message = "Başlık en fazla 200 karakter olabilir.")
        String title,

        @NotNull(message = "İçerik türü zorunludur.")
        LessonContentType contentType,

        @NotNull(message = "Zorunlu ders bilgisi zorunludur.")
        Boolean mandatory,

        @NotNull(message = "Süre zorunludur.")
        @Min(value = 0, message = "Süre negatif olamaz.")
        Integer durationSeconds) {
}
