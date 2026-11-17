package com.miniudemy.course.service;

import org.springframework.data.domain.Sort;

/**
 * Katalog sıralaması (docs/api/openapi.yaml › `GET /api/courses` › `sort`). Değer sözleşmedeki küçük harfli addır.
 *
 * {@code RATING} puan verisi review modülü gelince (#126, #143) çalışacak; o zamana dek en yeniye düşer.
 * {@code POPULAR} kayıt sayısı gerektirir; sayım başka modülde olduğu için sıralamayı servis kurar
 * ({@link #sorting()} yalnız veritabanında sıralanabilenler içindir).
 */
public enum CatalogSort {
    NEWEST("newest"),
    TITLE("title"),
    RATING("rating"),
    POPULAR("popular");

    private final String value;

    CatalogSort(String value) {
        this.value = value;
    }

    /** Sözleşmedeki ad; boş ya da null varsayılan {@code newest}'tir. Bilinmeyen ad için {@link IllegalArgumentException}. */
    public static CatalogSort fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NEWEST;
        }
        for (CatalogSort sort : values()) {
            if (sort.value.equals(value)) {
                return sort;
            }
        }
        throw new IllegalArgumentException("Bilinmeyen sıralama: " + value);
    }

    /** Veritabanında uygulanan sıralama; eşitlikte id ile sayfalar arasında kararlı sıra. */
    public Sort sorting() {
        if (this == TITLE) {
            return Sort.by(Sort.Order.asc("title").ignoreCase(), Sort.Order.asc("id"));
        }
        return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }
}
