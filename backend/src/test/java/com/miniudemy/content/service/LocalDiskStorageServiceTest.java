package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.content.domain.FileFormat;
import com.miniudemy.content.domain.FilePolicy;
import com.miniudemy.content.domain.StoredFile;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.content.repository.StoredFileRepository;

/**
 * Yerel disk depolaması (#111): dosya adı sunucuda üretilir, tür içerikten ve boyut akıştan denetlenir, reddedilen
 * dosyadan diskte iz kalmaz. Gerçek tür tespiti ve gerçek disk (geçici dizin) kullanılır; yalnız veritabanı taklit.
 */
class LocalDiskStorageServiceTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final FilePolicy PDF_10_BYTES_MAX = new FilePolicy(Set.of(FileFormat.PDF), 10,
            "PDF en fazla 10 bayt olabilir.", "Yalnız PDF yüklenebilir.");
    private static final FilePolicy VIDEO = new FilePolicy(Set.of(FileFormat.MP4, FileFormat.WEBM),
            FilePolicy.megabytes(200), "Video en fazla 200 MB olabilir.", "Yalnız mp4 ve webm videolar yüklenebilir.");
    private static final FilePolicy PDF = new FilePolicy(Set.of(FileFormat.PDF), FilePolicy.megabytes(20),
            "PDF en fazla 20 MB olabilir.", "Yalnız PDF yüklenebilir.");

    /** Testin geçici dizini; depolama kökü bunun iki seviye altında, dışarı yazılan dosya burada görünür. */
    @TempDir
    Path base;

    private Path root;

    private final StoredFileRepository repository = mock(StoredFileRepository.class);
    private LocalDiskStorageService service;

    @BeforeEach
    void setUp() {
        root = base.resolve("a/b/uploads");
        service = new LocalDiskStorageService(new StorageProperties(root), repository, new FileFormatDetector());
        given(repository.save(any(StoredFile.class))).willAnswer(inv -> {
            StoredFile saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });
    }

    @Test
    void store_validFile_writesItUnderServerGeneratedUuid_andSavesMetadata() throws IOException {
        byte[] pdf = TestFiles.pdf();

        StoredFileInfo info = service.store("Ders 1 notları.pdf", in(pdf), PDF, 42L);

        List<Path> files = filesUnder(root);
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFileName().toString()).matches(UUID_PATTERN);
        assertThat(Files.readAllBytes(files.get(0))).isEqualTo(pdf);
        assertThat(info).isEqualTo(new StoredFileInfo(7L, "Ders 1 notları.pdf", "application/pdf", pdf.length));

        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUploaderId()).isEqualTo(42L);
        assertThat(captor.getValue().getStorageKey()).isEqualTo(files.get(0).getFileName().toString());
    }

    @Test
    void store_userSuppliedPathIsNeverUsedOnDisk_onlyItsLastSegmentIsKeptAsDisplayName() throws IOException {
        StoredFileInfo info = service.store("../../etc/passwd.pdf", in(TestFiles.pdf()), PDF, 42L);

        assertThat(filesUnder(root)).singleElement()
                .satisfies(p -> assertThat(p.getFileName().toString()).matches(UUID_PATTERN));
        assertThat(filesUnder(base)).as("kök dizinin dışına yazılmadı").allMatch(p -> p.startsWith(root))
                .noneMatch(p -> p.endsWith("passwd.pdf"));
        assertThat(info.originalName()).isEqualTo("passwd.pdf");

        assertThat(service.store("C:\\Users\\a\\rapor.pdf", in(TestFiles.pdf()), PDF, 42L).originalName())
                .isEqualTo("rapor.pdf");
        assertThat(service.store(null, in(TestFiles.pdf()), PDF, 42L).originalName()).isEqualTo("dosya");
    }

    @Test
    void store_fileExactlyAtLimit_isAccepted() {
        byte[] tenBytes = TestFiles.concat(TestFiles.ascii("%PDF-"), new byte[5]);

        assertThat(service.store("a.pdf", in(tenBytes), PDF_10_BYTES_MAX, 42L).sizeBytes()).isEqualTo(10);
    }

    @Test
    void store_fileOverLimit_returns413WithPolicyMessage_andLeavesNothingOnDisk() throws IOException {
        byte[] elevenBytes = TestFiles.concat(TestFiles.ascii("%PDF-"), new byte[6]);

        assertThatThrownBy(() -> service.store("a.pdf", in(elevenBytes), PDF_10_BYTES_MAX, 42L))
                .isInstanceOf(ApiException.class)
                .hasMessage("PDF en fazla 10 bayt olabilir.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(filesUnder(root)).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void store_executableRenamedAsVideo_returns415WithPolicyMessage_andLeavesNothingOnDisk() throws IOException {
        assertThatThrownBy(() -> service.store("ders.mp4", in(TestFiles.exe()), VIDEO, 42L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Yalnız mp4 ve webm videolar yüklenebilir.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(filesUnder(root)).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void store_knownFormatNotAllowedByPolicy_returns415() throws IOException {
        assertThatThrownBy(() -> service.store("video.mp4", in(TestFiles.pdf()), VIDEO, 42L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(filesUnder(root)).isEmpty();
    }

    @Test
    void store_whenMetadataCannotBeSaved_removesTheFileFromDisk() throws IOException {
        given(repository.save(any(StoredFile.class))).willThrow(new IllegalStateException("veritabanı yok"));

        assertThatThrownBy(() -> service.store("a.pdf", in(TestFiles.pdf()), PDF, 42L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(filesUnder(root)).isEmpty();
    }

    @Test
    void open_returnsStoredContent_andDeleteRemovesFileAndMetadata() throws IOException {
        byte[] pdf = TestFiles.pdf();
        service.store("a.pdf", in(pdf), PDF, 42L);
        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).save(captor.capture());
        StoredFile saved = captor.getValue();
        given(repository.findById(7L)).willReturn(Optional.of(saved));

        Resource resource = service.open(7L);
        try (InputStream content = resource.getInputStream()) {
            assertThat(content.readAllBytes()).isEqualTo(pdf);
        }
        assertThat(service.get(7L)).isEqualTo(new StoredFileInfo(7L, "a.pdf", "application/pdf", pdf.length));

        service.delete(7L);
        assertThat(filesUnder(root)).isEmpty();
        verify(repository).delete(saved);
    }

    @Test
    void delete_whenFileIsStillReferenced_keepsTheFileOnDisk() throws IOException {
        // Gerçek PostgreSQL'de ölçüldü: kursun kapağı olan dosya silinmek istenince yabancı anahtar satırı korudu ama
        // dosya diskten silinmişti (satır var, dosya yok). Veritabanı reddederse disk de dokunulmadan kalmalı.
        service.store("kapak.pdf", in(TestFiles.pdf()), PDF, 42L);
        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(repository).save(captor.capture());
        given(repository.findById(7L)).willReturn(Optional.of(captor.getValue()));
        doThrow(new DataIntegrityViolationException("fk_courses_cover_file")).when(repository).flush();

        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(filesUnder(root)).hasSize(1);
    }

    @Test
    void get_open_delete_unknownFile_return404() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        for (Runnable call : List.<Runnable>of(() -> service.get(99L), () -> service.open(99L),
                () -> service.delete(99L))) {
            assertThatThrownBy(call::run)
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Dosya bulunamadı.")
                    .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    private static InputStream in(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static List<Path> filesUnder(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }
}
