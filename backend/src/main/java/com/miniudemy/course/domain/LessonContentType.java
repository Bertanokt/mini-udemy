package com.miniudemy.course.domain;

/**
 * Ders içerik türü. Değerler `lessons.content_type` kolonundaki CHECK kısıtıyla
 * ({@code ck_lessons_content_type}) aynıdır; birine değer eklenirse diğeri de aynı PR'da güncellenir.
 *
 * Türe özgü biçim, boyut ve sunuluş kuralları burada değil, content modülündeki Factory Method
 * sınıflarındadır (#110); bu enum yalnız hangi fabrikanın seçileceğini belirleyen saklanan değerdir.
 */
public enum LessonContentType {
    VIDEO,
    PDF,
    DOCUMENT
}
