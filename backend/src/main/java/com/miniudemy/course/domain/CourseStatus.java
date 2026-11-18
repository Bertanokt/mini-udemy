package com.miniudemy.course.domain;

/**
 * Kursun yayın durumu. Değerler `courses.status` kolonundaki CHECK kısıtıyla ({@code ck_courses_status})
 * aynıdır; birine değer eklenirse diğeri de aynı PR'da güncellenir.
 *
 * Bu enum veritabanında saklanan değerdir. Hangi durumda hangi işlemin izinli olduğuna ve geçişlerin hedefine
 * State deseninin durum nesneleri karar verir (#85, ADR-010). Değer → durum nesnesi eşlemesi yalnız burada,
 * her sabitin kendi durum nesnesiyle; serviste bu enum üzerinde if/switch yazılmaz.
 */
public enum CourseStatus {
    /** Taslak: eğitmen düzenliyor, katalogda görünmez. */
    DRAFT(DraftState.INSTANCE),
    /** İncelemede: onaya gönderildi, yönetici kararı bekleniyor. */
    IN_REVIEW(InReviewState.INSTANCE),
    /** Yayımlandı: katalogda görünür, kayıt alınır. */
    PUBLISHED(PublishedState.INSTANCE),
    /** Yayından kaldırıldı: yönetici kaldırdı, yeniden incelemeye gönderilebilir. */
    UNPUBLISHED(UnpublishedState.INSTANCE);

    private final CourseState state;

    CourseStatus(CourseState state) {
        this.state = state;
    }

    /** Bu değerin durum nesnesi; yalnız {@link Course} kullanır. */
    CourseState state() {
        return state;
    }
}
