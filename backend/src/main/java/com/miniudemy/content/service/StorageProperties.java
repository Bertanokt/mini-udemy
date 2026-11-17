package com.miniudemy.content.service;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Depolama ayarları ({@code app.storage.*}).
 *
 * @param root dosyaların yazıldığı dizin. Docker'da {@code /data/uploads} (uploads volume'u), yerelde varsayılan
 *             {@code ./data/uploads}. Uygulama açılırken yoksa oluşturulur.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Path root) {

    public StorageProperties {
        if (root == null) {
            throw new IllegalStateException("app.storage.root tanımlı değil (STORAGE_ROOT).");
        }
        root = root.toAbsolutePath().normalize();
    }
}
