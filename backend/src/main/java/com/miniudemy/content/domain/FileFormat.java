package com.miniudemy.content.domain;

/**
 * Yüklenen dosyanın <b>içerikten</b> (ilk baytlarından, magic bytes) tespit edilen gerçek türü. Dosya adındaki
 * uzantıya ya da istemcinin gönderdiği Content-Type'a güvenilmez (NFR-12, ADR-004).
 *
 * Tespiti {@code FileFormatDetector} yapar. Hangi türün nerede kabul edildiğini bu enum değil, {@link FilePolicy}
 * söyler.
 */
public enum FileFormat {
    MP4("video/mp4"),
    WEBM("video/webm"),
    PDF("application/pdf"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    private final String mimeType;

    FileFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    /** Sunulurken kullanılacak Content-Type; {@code stored_files.mime_type}'a da bu yazılır. */
    public String mimeType() {
        return mimeType;
    }
}
