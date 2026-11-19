package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.PayloadTooLargeException;
import com.miniudemy.common.exception.UnsupportedMediaTypeException;
import com.miniudemy.content.domain.CoverImage;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.service.ContentLinkService;

/**
 * Yükleme akışı (#112): izin denetimi dosya yazılmadan önce, kural dersin türünün fabrikasından, bağ kurulamazsa yeni
 * dosya silinir. Dosya türü/boyut denetiminin kendisi {@code LocalDiskStorageServiceTest}'te ve uçtan uca
 * {@code ContentUploadIT}'te; burada doğru kuralın depolamaya verildiği sınanır.
 */
class ContentUploadServiceTest {

    private static final long INSTRUCTOR = 7L;

    private final ContentLinkService links = mock(ContentLinkService.class);
    private final StorageService storage = mock(StorageService.class);
    private final ContentUploadService service = new ContentUploadService(links, storage,
            new LessonContentFactories(List.of(new VideoContentFactory(), new PdfContentFactory(),
                    new DocumentContentFactory())));
    private final InputStream content = new ByteArrayInputStream(new byte[] {1});
    private final LessonResponse linked = new LessonResponse(31L, 20L, "Kurulum", LessonContentType.VIDEO, true, 60, 0,
            true);

    private void lessonOfType(LessonContentType type) {
        given(links.assertLessonContentUploadable(INSTRUCTOR, 31L))
                .willReturn(new LessonContentRef(31L, 5L, INSTRUCTOR, type, null));
        given(storage.store(any(), any(), any(), anyLong())).willReturn(new StoredFileInfo(500L, "a", "video/mp4", 1));
        given(links.attachLessonContent(INSTRUCTOR, 31L, 500L)).willReturn(linked);
    }

    @ParameterizedTest(name = "{0} dersi → {1} MB, {2}")
    @CsvSource({"VIDEO, 200, MP4", "PDF, 20, PDF", "DOCUMENT, 20, DOCX"})
    void uploadLessonContent_givesTheStorageTheLessonTypesPolicy_andLinksTheStoredFile(LessonContentType type, long mb,
            FileFormat sample) {
        lessonOfType(type);

        LessonResponse response = service.uploadLessonContent(INSTRUCTOR, 31L, "Kurulum.mp4", content);

        assertThat(response).isSameAs(linked);
        ArgumentCaptor<FilePolicy> policy = ArgumentCaptor.forClass(FilePolicy.class);
        verify(storage).store(eq("Kurulum.mp4"), eq(content), policy.capture(), eq(INSTRUCTOR));
        assertThat(policy.getValue().maxBytes()).isEqualTo(FilePolicy.megabytes(mb));
        assertThat(policy.getValue().allows(sample)).isTrue();
        verify(storage, never()).delete(anyLong());
    }

    @Test
    void uploadLessonContent_whenNotAllowed_failsBeforeAnythingIsWritten() {
        given(links.assertLessonContentUploadable(INSTRUCTOR, 31L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        assertThatThrownBy(() -> service.uploadLessonContent(INSTRUCTOR, 31L, "a.mp4", content))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(storage);
        verify(links, never()).attachLessonContent(anyLong(), anyLong(), anyLong());
    }

    @Test
    void uploadLessonContent_whenStorageRejects_413or415_linksNothing() {
        lessonOfType(LessonContentType.VIDEO);
        org.mockito.BDDMockito.willThrow(new PayloadTooLargeException("Video en fazla 200 MB olabilir."))
                .given(storage).store(any(), any(), any(), anyLong());

        assertThatThrownBy(() -> service.uploadLessonContent(INSTRUCTOR, 31L, "a.mp4", content))
                .isInstanceOf(PayloadTooLargeException.class);
        org.mockito.BDDMockito.willThrow(new UnsupportedMediaTypeException("Yalnız mp4 ve webm videolar yüklenebilir."))
                .given(storage).store(any(), any(), any(), anyLong());
        assertThatThrownBy(() -> service.uploadLessonContent(INSTRUCTOR, 31L, "a.mp4", content))
                .isInstanceOf(UnsupportedMediaTypeException.class);

        verify(links, never()).attachLessonContent(anyLong(), anyLong(), anyLong());
        verify(storage, never()).delete(anyLong());
    }

    @Test
    void uploadLessonContent_whenLinkingFails_deletesTheNewFile() {
        lessonOfType(LessonContentType.VIDEO);
        given(links.attachLessonContent(INSTRUCTOR, 31L, 500L)).willThrow(new ConflictException("Kurs kilitlendi."));

        assertThatThrownBy(() -> service.uploadLessonContent(INSTRUCTOR, 31L, "a.mp4", content))
                .isInstanceOf(ConflictException.class);

        verify(storage).delete(500L);
    }

    @Test
    void uploadLessonContent_whenDeletingTheNewFileAlsoFails_stillThrowsTheOriginalError() {
        lessonOfType(LessonContentType.VIDEO);
        ConflictException original = new ConflictException("Kurs kilitlendi.");
        given(links.attachLessonContent(INSTRUCTOR, 31L, 500L)).willThrow(original);
        org.mockito.BDDMockito.willThrow(new IllegalStateException("disk")).given(storage).delete(500L);

        assertThatThrownBy(() -> service.uploadLessonContent(INSTRUCTOR, 31L, "a.mp4", content)).isSameAs(original);
        assertThat(original.getSuppressed()).hasSize(1);
    }

    @Test
    void uploadCourseCover_usesTheCoverPolicy_andLinksTheStoredFile() {
        CourseResponse course = mock(CourseResponse.class);
        given(storage.store(any(), any(), any(), anyLong())).willReturn(new StoredFileInfo(500L, "k", "image/png", 1));
        given(links.attachCover(INSTRUCTOR, 5L, 500L)).willReturn(course);

        CourseResponse response = service.uploadCourseCover(INSTRUCTOR, 5L, "kapak.png", content);

        assertThat(response).isSameAs(course);
        verify(links).assertCoverUploadable(INSTRUCTOR, 5L);
        verify(storage).store("kapak.png", content, CoverImage.POLICY, INSTRUCTOR);
    }

    @Test
    void uploadCourseCover_whenNotAllowed_failsBeforeAnythingIsWritten() {
        org.mockito.BDDMockito.willThrow(new ConflictException("Kurs kilitli.")).given(links)
                .assertCoverUploadable(INSTRUCTOR, 5L);

        assertThatThrownBy(() -> service.uploadCourseCover(INSTRUCTOR, 5L, "kapak.png", content))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(storage);
    }

    @Test
    void uploadCourseCover_whenLinkingFails_deletesTheNewFile() {
        given(storage.store(any(), any(), any(), anyLong())).willReturn(new StoredFileInfo(500L, "k", "image/png", 1));
        given(links.attachCover(INSTRUCTOR, 5L, 500L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));

        assertThatThrownBy(() -> service.uploadCourseCover(INSTRUCTOR, 5L, "kapak.png", content))
                .isInstanceOf(ForbiddenException.class);

        verify(storage).delete(500L);
    }

    @Test
    void coverPolicy_allowsOnlyJpgPngWebp_upTo2MB_withItsMessages() {
        FilePolicy policy = CoverImage.POLICY;

        assertThat(policy.allowedFormats()).isEqualTo(Set.of(FileFormat.JPEG, FileFormat.PNG, FileFormat.WEBP));
        assertThat(policy.maxBytes()).isEqualTo(FilePolicy.megabytes(2));
        assertThat(policy.tooLargeMessage()).isEqualTo("Kapak görseli en fazla 2 MB olabilir.");
        assertThat(policy.unsupportedTypeMessage()).isEqualTo("Yalnız jpg, png ve webp görseller yüklenebilir.");
    }
}
