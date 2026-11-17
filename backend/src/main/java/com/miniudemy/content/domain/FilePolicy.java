package com.miniudemy.content.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Bir yüklemenin kabul kuralları: izin verilen gerçek türler, en büyük boyut ve kural çiğnenince kullanıcıya gösterilecek
 * mesajlar. Depolama ({@code StorageService}) türleri bilmez; yalnız kendisine verilen kuralı uygular.
 *
 * <p><b>#110 (Factory Method) ile sözleşme:</b> her ders içerik türünün ürünü ({@code LessonContent}) kendi
 * {@code FilePolicy}'sini verir (ör. video: MP4/WEBM, 200 MB, "Video en fazla 200 MB olabilir."). Yükleme servisi
 * (#112) türü bilmeden {@code storageService.store(ad, akış, content.filePolicy(), yükleyenId)} çağırır. Yeni tür = yeni
 * fabrika + yeni kural; depolama değişmez. Kapak görselinin kuralı (#112) da bir {@code FilePolicy}'dir.
 *
 * @param allowedFormats        kabul edilen türler (boş olamaz)
 * @param maxBytes              en büyük boyut (bayt); aşılırsa 413
 * @param tooLargeMessage       413 mesajı (sözleşmedeki örnek: "Video en fazla 200 MB olabilir.")
 * @param unsupportedTypeMessage 415 mesajı (sözleşmedeki örnek: "Yalnız mp4 ve webm videolar yüklenebilir.")
 */
public record FilePolicy(Set<FileFormat> allowedFormats, long maxBytes, String tooLargeMessage,
        String unsupportedTypeMessage) {

    public FilePolicy {
        Objects.requireNonNull(allowedFormats, "allowedFormats");
        if (allowedFormats.isEmpty()) {
            throw new IllegalArgumentException("En az bir tür kabul edilmeli.");
        }
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes pozitif olmalı.");
        }
        Objects.requireNonNull(tooLargeMessage, "tooLargeMessage");
        Objects.requireNonNull(unsupportedTypeMessage, "unsupportedTypeMessage");
        allowedFormats = Set.copyOf(allowedFormats);
    }

    /** {@code n} megabayt (1 MB = 1024 × 1024 bayt). */
    public static long megabytes(long n) {
        return n * 1024 * 1024;
    }

    public boolean allows(FileFormat format) {
        return format != null && allowedFormats.contains(format);
    }
}
