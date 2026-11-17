package com.miniudemy.review.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import com.miniudemy.review.dto.RatingSummary;

/** Puan hesapları: dağılımdan ortalama ve özet. Saf fonksiyonlar, veritabanına dokunmaz. */
final class RatingMath {

    private RatingMath() {
    }

    static Double round1(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** {@code countByRating}: puan (1–5) -> görünür yorum sayısı; eksik puanlar 0 sayılır. */
    static RatingSummary summarize(Map<Integer, Long> countByRating) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        long total = 0;
        long weighted = 0;
        for (int stars = 1; stars <= 5; stars++) {
            long count = countByRating.getOrDefault(stars, 0L);
            distribution.put(String.valueOf(stars), count);
            total += count;
            weighted += count * stars;
        }
        Double average = total == 0 ? null : round1((double) weighted / total);
        return new RatingSummary(average, total, distribution);
    }
}
