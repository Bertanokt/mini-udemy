package com.miniudemy.content.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.common.exception.PayloadTooLargeException;
import com.miniudemy.common.exception.UnsupportedMediaTypeException;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.domain.StoredFile;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.content.repository.StoredFileRepository;

/**
 * {@link StorageService}'in yerel disk uygulaması (ADR-004). Dosyalar {@code app.storage.root} altında, sunucunun
 * ürettiği UUID adıyla ve uzantısız saklanır; kullanıcının verdiği ad diskte hiçbir yerde kullanılmaz.
 *
 * Yazma sırası: akış önce {@code .tmp/} altına yazılır (boyut bu sırada sayılır) → tür içerikten tespit edilir →
 * dosya kalıcı adına taşınır → meta veri kaydedilir. Herhangi bir adım başarısız olursa diskte iz kalmaz.
 */
@Service
@EnableConfigurationProperties(StorageProperties.class)
public class LocalDiskStorageService implements StorageService {

    private static final Pattern STORAGE_KEY = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final int MAX_NAME_LENGTH = 255;
    private static final String NOT_FOUND = "Dosya bulunamadı.";

    private final Path root;
    private final Path tmpDir;
    private final StoredFileRepository repository;
    private final FileFormatDetector detector;

    public LocalDiskStorageService(StorageProperties properties, StoredFileRepository repository,
            FileFormatDetector detector) {
        this.root = properties.root();
        this.tmpDir = root.resolve(".tmp");
        this.repository = repository;
        this.detector = detector;
        try {
            Files.createDirectories(tmpDir);
        } catch (IOException e) {
            throw new IllegalStateException("Depolama dizini oluşturulamadı: " + root, e);
        }
    }

    @Override
    @Transactional
    public StoredFileInfo store(String originalName, InputStream content, FilePolicy policy, Long uploaderId) {
        String key = UUID.randomUUID().toString();
        Path tmp = tmpDir.resolve(key + ".part");
        try {
            long size = copyWithLimit(content, tmp, policy);
            FileFormat format = detector.detect(tmp)
                    // KIRMIZI-T1 .filter(policy::allows)
                    .orElseThrow(() -> new UnsupportedMediaTypeException(policy.unsupportedTypeMessage()));
            Path target = pathOf(key);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            try {
                StoredFile saved = repository.save(
                        new StoredFile(uploaderId, key, displayName(originalName), format.mimeType(), size));
                return toInfo(saved);
            } catch (RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Dosya diske yazılamadı.", e);
        } finally {
            deleteQuietly(tmp);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFileInfo get(Long fileId) {
        return toInfo(find(fileId));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource open(Long fileId) {
        Path path = pathOf(find(fileId).getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException(NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        StoredFile file = find(fileId);
        repository.delete(file);
        // Silme hemen veritabanına gönderilir: dosya hâlâ bir kursta ya da derste kullanılıyorsa yabancı anahtar burada
        // reddeder ve diskteki dosyaya dokunulmaz. (flush olmadan ret commit'te gelir, dosya ise çoktan silinmiş olurdu;
        // gerçek PostgreSQL'de ölçüldü.)
        repository.flush();
        try {
            Files.deleteIfExists(pathOf(file.getStorageKey()));
        } catch (IOException e) {
            throw new UncheckedIOException("Dosya diskten silinemedi.", e);
        }
    }

    /** Akışı geçici dosyaya yazar; sınır aşılırsa okumayı keser ve 413 fırlatır. Yazılan bayt sayısını döner. */
    private static long copyWithLimit(InputStream content, Path tmp, FilePolicy policy) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream out = Files.newOutputStream(tmp)) {
            int n;
            while ((n = content.read(buffer)) != -1) {
                total += n;
                if (total > policy.maxBytes() * 2) { // KIRMIZI-T2
                    throw new PayloadTooLargeException(policy.tooLargeMessage());
                }
                out.write(buffer, 0, n);
            }
        }
        return total;
    }

    /** Diskteki yol yalnız sunucunun ürettiği UUID'den kurulur; başka bir değer kök dizinin dışına çıkamaz. */
    private Path pathOf(String storageKey) {
        if (!STORAGE_KEY.matcher(storageKey).matches()) {
            throw new IllegalStateException("Geçersiz depolama anahtarı.");
        }
        return root.resolve(storageKey);
    }

    /** Kullanıcının verdiği addan yalnız son parça (yol kısımları ve kontrol karakterleri atılır); gösterim adı. */
    static String displayName(String originalName) {
        if (originalName == null) {
            return "dosya";
        }
        String name = originalName.substring(Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1)
                .replaceAll("\\p{Cntrl}", "")
                .strip();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "dosya";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(name.length() - MAX_NAME_LENGTH) : name;
    }

    private StoredFile find(Long fileId) {
        return repository.findById(fileId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private static StoredFileInfo toInfo(StoredFile file) {
        return new StoredFileInfo(file.getId(), file.getOriginalName(), file.getMimeType(), file.getSizeBytes());
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Geçici dosya silinemezse bir sonraki yüklemeyi etkilemez; .tmp/ dizini dışarıya açık değil.
        }
    }
}
