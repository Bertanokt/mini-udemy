package com.miniudemy.course.dto;

import java.util.List;

import com.miniudemy.course.domain.CourseLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Kurs güncelleme isteği (#23; docs/api/openapi.yaml › UpdateCourseRequest, CreateCourseRequest ile aynı şema).
 * Sınırlar sözleşmedekiyle aynı; ihlal 400 ve alan bazlı {@code errors} döner. Kategorinin var olup olmadığı burada değil serviste denetlenir.
 * Kapak görseli bu istekte değil, ayrı yükleme ucuyla bağlanır.
 */
public record UpdateCourseRequest(
        @NotBlank(message = "Başlık zorunludur.")
        @Size(min = 3, max = 200, message = "Başlık 3–200 karakter olmalıdır.")
        String title,

        @NotBlank(message = "Açıklama zorunludur.")
        @Size(min = 10, max = 5000, message = "Açıklama 10–5000 karakter olmalıdır.")
        String description,

        @NotNull(message = "Kategori zorunludur.")
        Long categoryId,

        @NotNull(message = "Seviye zorunludur.")
        CourseLevel level,

        @Size(max = 20, message = "En fazla 20 öğrenme hedefi girilebilir.")
        List<@NotBlank(message = "Öğrenme hedefi boş olamaz.")
             @Size(max = 300, message = "Öğrenme hedefi en fazla 300 karakter olabilir.") String> learningOutcomes) {
}
