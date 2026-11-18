package com.miniudemy.course.domain;

/**
 * State deseni (#85, ADR-010): kursun bir durumunda hangi işlemin izinli olduğuna ve geçişin hedefine karar verir.
 * Geçiş tablosu: docs/architecture.md › Kurs durumları (FR-09, FR-10, FR-38).
 *
 * Varsayılan olarak her işlem reddedilir (409, {@link CourseStateException}); her durum sınıfı yalnız kendi izinli
 * işlemlerini ezer. Böylece yeni bir işlem eklendiğinde, onu açıkça izinli yapmayan her durumda reddedilir.
 *
 * Durum nesnelerinin verisi yoktur; değişen tek şey kursun {@link CourseStatus} alanıdır. Eşleme yalnız
 * {@link CourseStatus#state()}'tedir. Dışarıdan kullanılmaz: işlemler {@link Course} üzerinden çağrılır.
 */
sealed interface CourseState permits DraftState, InReviewState, PublishedState, UnpublishedState {

    /** Bu durum nesnesinin karşılık geldiği saklanan değer. */
    CourseStatus status();

    /** Eğitmen kursu (ve bölüm, ders, sınav gibi yapısını) değiştirebilir mi; değilse 409. */
    default void assertEditable() {
        throw new CourseStateException(
                "Kurs bu durumdayken düzenlenemez; yalnız taslak ve yayından kaldırılmış kurslar düzenlenebilir.");
    }

    /**
     * Kursa yeni ders eklenebilir mi; değilse 409. Düzenlenebilir durumlara ek olarak Yayımlandı'da da izinlidir
     * (FR-43: yayımlanmış kursa yeni ders eklenir ve kayıtlı kursiyerlere bildirilir); mevcut dersi değiştirmek,
     * silmek ve sıralamak yine {@link #assertEditable()} kuralına bağlıdır.
     */
    default void assertLessonAddable() {
        throw new CourseStateException("Kurs bu durumdayken ders eklenemez; yalnız taslak, yayımlanmış ve "
                + "yayından kaldırılmış kurslara ders eklenebilir.");
    }

    /**
     * Dersin içerik dosyası yüklenebilir mi; değilse 409 (#112). İçeriği henüz olmayan derse yükleme, ders eklemeyle
     * aynı kuraldır (yayımlanmış kursa FR-43 ile eklenen ders içeriksiz gelir ve içeriği yüklenebilmelidir); mevcut
     * içeriği değiştirmek dersi değiştirmektir ve {@link #assertEditable()} kuralına bağlıdır.
     *
     * @param replacing derste zaten bir içerik dosyası var mı
     */
    default void assertContentUploadable(boolean replacing) {
        if (replacing) {
            assertEditable();
        } else {
            assertLessonAddable();
        }
    }

    /** Kurs silinebilir mi; değilse 409. */
    default void assertDeletable() {
        throw new CourseStateException("Kurs bu durumdayken silinemez; yalnız taslak kurslar silinebilir.");
    }

    /** Onaya gönder: hedef durum. */
    default CourseStatus submit() {
        throw new CourseStateException("Kurs bu durumdayken onaya gönderilemez; "
                + "yalnız taslak ve yayından kaldırılmış kurslar onaya gönderilebilir.");
    }

    /** Yönetici onayı: hedef durum. */
    default CourseStatus approve() {
        throw new CourseStateException("Kurs bu durumdayken onaylanamaz; yalnız incelemedeki kurslar onaylanabilir.");
    }

    /** Yönetici reddi: hedef durum. */
    default CourseStatus reject() {
        throw new CourseStateException("Kurs bu durumdayken reddedilemez; yalnız incelemedeki kurslar reddedilebilir.");
    }

    /** Yayından kaldır: hedef durum. */
    default CourseStatus unpublish() {
        throw new CourseStateException(
                "Kurs bu durumdayken yayından kaldırılamaz; yalnız yayımlanmış kurslar yayından kaldırılabilir.");
    }

    /** Kurs katalogda görünür mü (FR-09: yalnız Yayımlandı). */
    default boolean isPublished() {
        return false;
    }
}
