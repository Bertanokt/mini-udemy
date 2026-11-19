package com.miniudemy.course.dto;

import jakarta.validation.constraints.Size;

/**
 * Eğitmenin kendi kursunu yayından kaldırma isteği (#123; docs/api/openapi.yaml › UnpublishCourseRequest). Gövde ve
 * gerekçe isteğe bağlıdır (FR-38); boş gerekçe "gerekçe yok" olarak saklanır. Yöneticinin zorunlu gerekçesi
 * {@code admin.dto.ReasonRequest}'tedir.
 */
public record UnpublishCourseRequest(
        @Size(max = 1000, message = "Gerekçe en fazla 1000 karakter olabilir.")
        String reason) {
}
