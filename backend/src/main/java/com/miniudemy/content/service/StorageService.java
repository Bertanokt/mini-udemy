package com.miniudemy.content.service;

import java.io.InputStream;

import org.springframework.core.io.Resource;

import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.dto.StoredFileInfo;

/**
 * Dosya depolama (ADR-004): dosyalar backend'in diskinde (Docker volume) durur ve yalnız bu arayüz üzerinden
 * yazılır, okunur, silinir. Uygulaması: {@link LocalDiskStorageService}.
 *
 * Tür ve boyut kuralını çağıran verir ({@link FilePolicy}; ders içerikleri için #110'daki fabrikalar, kapak için
 * #112). Depolama türleri bilmez.
 */
public interface StorageService {

    /**
     * Akışı diske yazar ve meta veriyi kaydeder.
     * <ul>
     *   <li>Boyut akış okunurken sayılır (istemcinin bildirdiği boyuta güvenilmez); sınır aşılırsa
     *       {@code PayloadTooLargeException} (413) ve diske hiçbir şey kalmaz.</li>
     *   <li>Tür içerikten tespit edilir; kurala uymuyorsa {@code UnsupportedMediaTypeException} (415) ve diske hiçbir
     *       şey kalmaz.</li>
     *   <li>Diskteki ad sunucunun ürettiği UUID'dir; {@code originalName} yalnız gösterim adı olarak saklanır.</li>
     * </ul>
     *
     * @param originalName kullanıcının verdiği dosya adı (null olabilir); yol kısmı atılır
     * @param content      dosyanın içeriği; bu metot kapatmaz
     * @param policy       kabul kuralı
     * @param uploaderId   yükleyen kullanıcı ({@code users.id})
     */
    StoredFileInfo store(String originalName, InputStream content, FilePolicy policy, Long uploaderId);

    /** Dosyanın bilgisi; yoksa {@code NotFoundException} (404). */
    StoredFileInfo get(Long fileId);

    /** Dosyanın içeriği (#113: akış ve Range ile sunulur); yoksa {@code NotFoundException} (404). */
    Resource open(Long fileId);

    /** Dosyayı diskten ve meta veriyi veritabanından siler (#112: eski dosya değiştirilince). */
    void delete(Long fileId);
}
