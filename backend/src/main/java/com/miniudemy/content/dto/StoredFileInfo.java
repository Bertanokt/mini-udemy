package com.miniudemy.content.dto;

/**
 * Saklanan bir dosyanın, depolama servisi dışına verilen bilgisi. Diskteki yol ve {@code storageKey} bilerek yok:
 * dosyaya yalnız {@code StorageService} üzerinden erişilir.
 *
 * @param id           {@code stored_files.id}; ders ve kurs bu id'yi tutar
 * @param originalName kullanıcının verdiği ad (yol kısmı atılmış); indirme adı
 * @param mimeType     içerikten tespit edilen tür
 * @param sizeBytes    bayt
 */
public record StoredFileInfo(Long id, String originalName, String mimeType, long sizeBytes) {
}
