package com.miniudemy.content.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.content.service.StorageService;
import com.miniudemy.course.domain.ContentFileDetached;

/** Bağı kopan dosyanın silinmesi (#112): silme isteği iletilir, hata kullanıcıya yansımaz. */
class StoredFileCleanupListenerTest {

    private final StorageService storage = mock(StorageService.class);
    private final StoredFileCleanupListener listener = new StoredFileCleanupListener(storage);

    @Test
    void onContentFileDetached_deletesTheFile() {
        listener.onContentFileDetached(new ContentFileDetached(500L));

        verify(storage).delete(500L);
    }

    @Test
    void onContentFileDetached_whenTheFileIsAlreadyGone_doesNotFail() {
        willThrow(new NotFoundException("Dosya bulunamadı.")).given(storage).delete(500L);

        assertThatCode(() -> listener.onContentFileDetached(new ContentFileDetached(500L))).doesNotThrowAnyException();
    }

    @Test
    void onContentFileDetached_whenDeletingFails_doesNotFail() {
        willThrow(new IllegalStateException("disk")).given(storage).delete(500L);

        assertThatCode(() -> listener.onContentFileDetached(new ContentFileDetached(500L))).doesNotThrowAnyException();
    }
}
