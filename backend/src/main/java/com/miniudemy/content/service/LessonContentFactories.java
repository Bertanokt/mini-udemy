package com.miniudemy.content.service;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.miniudemy.course.domain.LessonContentType;

/**
 * Ders içerik türü → fabrika seçimi (#110, ADR-011): Spring'in bulduğu bütün {@link LessonContentFactory} bean'leri
 * kendi {@code type()} değeriyle bir {@link EnumMap}'e konur; {@link #forType} if/switch olmadan okur. Bir türün
 * fabrikası yoksa ya da iki fabrika aynı türü iddia ederse kurucu {@link IllegalStateException} fırlatır, yani
 * uygulama başlamaz.
 */
@Service
public class LessonContentFactories {

    private final Map<LessonContentType, LessonContentFactory> byType = new EnumMap<>(LessonContentType.class);

    public LessonContentFactories(List<LessonContentFactory> factories) {
        for (LessonContentFactory factory : factories) {
            LessonContentFactory previous = byType.putIfAbsent(factory.type(), factory);
            if (previous != null) {
                throw new IllegalStateException(factory.type() + " için iki fabrika var: "
                        + previous.getClass().getSimpleName() + ", " + factory.getClass().getSimpleName());
            }
        }
        List<LessonContentType> missing = Arrays.stream(LessonContentType.values())
                .filter(type -> !byType.containsKey(type)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Fabrikası olmayan ders içerik türleri: " + missing);
        }
    }

    /** Türün fabrikası; tür boşsa {@link IllegalArgumentException}. */
    public LessonContentFactory forType(LessonContentType type) {
        if (type == null) {
            throw new IllegalArgumentException("Ders içerik türü boş olamaz.");
        }
        return byType.get(type);
    }
}
