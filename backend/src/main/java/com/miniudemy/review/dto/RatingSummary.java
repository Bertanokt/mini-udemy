package com.miniudemy.review.dto;

import java.util.Map;

/**
 * Ortalama puan ve 1–5 dağılımı (docs/api/openapi.yaml › RatingSummary). Yorum yoksa {@code averageRating} null,
 * dağılımın beş anahtarı da 0'dır. Ortalama bir ondalığa yuvarlanır.
 */
public record RatingSummary(Double averageRating, long reviewCount, Map<String, Long> distribution) {
}
