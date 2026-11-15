package com.miniudemy.course.domain;

/**
 * Kursun yayın durumu. Değerler `courses.status` kolonundaki CHECK kısıtıyla ({@code ck_courses_status})
 * aynıdır; birine değer eklenirse diğeri de aynı PR'da güncellenir.
 *
 * Bu enum yalnız veritabanında saklanan değerdir. Hangi durumda hangi işlemin izinli olduğuna ve geçişlerin
 * hedefine State deseninin durum nesneleri karar verir (#85); serviste bu enum üzerinde if/switch yazılmaz.
 */
public enum CourseStatus {
    /** Taslak: eğitmen düzenliyor, katalogda görünmez. */
    DRAFT,
    /** İncelemede: onaya gönderildi, yönetici kararı bekleniyor. */
    IN_REVIEW,
    /** Yayımlandı: katalogda görünür, kayıt alınır. */
    PUBLISHED,
    /** Yayından kaldırıldı: yönetici kaldırdı, yeniden incelemeye gönderilebilir. */
    UNPUBLISHED
}
