package com.miniudemy.content.domain;

/**
 * Factory Method'un ürünü (#110, ADR-011): bir ders içerik türünün yükleme kuralı ({@link FilePolicy}) ve sunuluş
 * şekli. Yükleme (#112) yalnız {@link #filePolicy()}'yi, yetki kontrollü sunum (#113) yalnız {@link #deliveryMode()}
 * ve {@link #supportsRangeRequests()}'i kullanır; ikisi de hangi tür olduğunu bilmez.
 *
 * Ürünler course modülünü bilmez; ders türü → ürün seçimi {@code content/service}'teki fabrikalardadır.
 */
public abstract sealed class LessonContent permits VideoContent, PdfContent, DocumentContent {

    /** Yüklemenin kabul kuralı: izin verilen gerçek türler, en büyük boyut, 413/415 mesajları. */
    public abstract FilePolicy filePolicy();

    /** Tarayıcıda açılır mı (inline) yoksa indirilir mi (attachment). */
    public abstract DeliveryMode deliveryMode();

    /** HTTP {@code Range} istekleri (206, ileri sarma) desteklenir mi. */
    public abstract boolean supportsRangeRequests();
}
