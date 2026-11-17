package com.miniudemy.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Kategori ekleme / yeniden adlandırma isteği (docs/api/openapi.yaml › CategoryRequest).
 * Ad baştaki ve sondaki boşluklardan arındırılır; uzunluk sınırı arındırılmış ad üzerinden denetlenir.
 */
public record CategoryRequest(
        @NotBlank(message = "Kategori adı zorunludur.")
        @Size(min = 2, max = 100, message = "Kategori adı 2–100 karakter olmalıdır.")
        String name) {

    public CategoryRequest {
        name = name == null ? null : name.strip();
    }
}
