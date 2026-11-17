package com.miniudemy.content.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Diskte saklanan bir dosyanın meta verisi.
 *
 * Tablo: `stored_files` (ER diyagramı › content). Dosyanın diskteki adı {@code storageKey}'dir (sunucunun ürettiği
 * UUID); kullanıcının verdiği ad yalnız gösterim ve indirme adı için {@code originalName}'de tutulur, diskte yol olarak
 * hiç kullanılmaz. Yükleyen kullanıcı auth modülüne ait olduğu için yalnız id olarak tutulur.
 */
@Entity
@Table(name = "stored_files")
public class StoredFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "storage_key", nullable = false, length = 100, unique = true)
    private String storageKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA için. */
    protected StoredFile() {
    }

    public StoredFile(Long uploaderId, String storageKey, String originalName, String mimeType, long sizeBytes) {
        this.uploaderId = uploaderId;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    public Long getId() {
        return id;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
