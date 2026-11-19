package com.miniudemy.content.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.content.service.StorageService;
import com.miniudemy.course.domain.ContentFileDetached;

/**
 * Kursun ya da dersin dosya bağı kopunca dosyayı diskten ve {@code stored_files}'tan siler (Observer, ADR-009; #112).
 * AFTER_COMMIT: bağı koparan işlem tamamlanmadan dosya silinmez, işlem geri alınırsa dosya yerinde kalır. Silme başarısız
 * olursa yalnız loglanır (kullanıcının isteği zaten tamamlandı); dosya yok sayılır.
 */
@Component
public class StoredFileCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(StoredFileCleanupListener.class);

    private final StorageService storage;

    public StoredFileCleanupListener(StorageService storage) {
        this.storage = storage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onContentFileDetached(ContentFileDetached event) {
        try {
            storage.delete(event.fileId());
        } catch (NotFoundException alreadyGone) {
            log.debug("Dosya zaten yok: {}", event.fileId());
        } catch (RuntimeException e) {
            log.warn("Sahipsiz dosya silinemedi: {}", event.fileId(), e);
        }
    }
}
