package com.miniudemy.review.dto;

import java.util.List;

/** Sayfalı yorumlar, yönetici listesi için (docs/api/openapi.yaml › ReviewPage). {@code page} 0'dan başlar. */
public record ReviewPage(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
