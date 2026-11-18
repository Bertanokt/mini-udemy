package com.miniudemy.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Sıra değiştirme isteği (docs/api/openapi.yaml › PositionRequest); sıra 0'dan başlar. */
public record PositionRequest(
        @NotNull(message = "Sıra zorunludur.")
        @Min(value = 0, message = "Sıra 0 ya da daha büyük olmalıdır.")
        Integer position) {
}
