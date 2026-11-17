package com.miniudemy.content.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.miniudemy.content.domain.FileFormat;

/**
 * Tür, dosya adından değil içerikten tespit edilir (NFR-12): her dosya bilerek yanıltıcı bir uzantıyla
 * (".bin") yazılır.
 */
class FileFormatDetectorTest {

    @TempDir
    Path dir;

    private final FileFormatDetector detector = new FileFormatDetector();

    @Test
    void detectsEachSupportedFormatFromContent() throws IOException {
        assertThat(detect(TestFiles.mp4())).contains(FileFormat.MP4);
        assertThat(detect(TestFiles.webm())).contains(FileFormat.WEBM);
        assertThat(detect(TestFiles.pdf())).contains(FileFormat.PDF);
        assertThat(detect(TestFiles.docx())).contains(FileFormat.DOCX);
        assertThat(detect(TestFiles.pptx())).contains(FileFormat.PPTX);
        assertThat(detect(TestFiles.jpeg())).contains(FileFormat.JPEG);
        assertThat(detect(TestFiles.png())).contains(FileFormat.PNG);
        assertThat(detect(TestFiles.webp())).contains(FileFormat.WEBP);
    }

    @Test
    void executableRenamedAsVideo_isNotRecognised() throws IOException {
        Path file = dir.resolve("video.mp4");
        Files.write(file, TestFiles.exe());

        assertThat(detector.detect(file)).isEmpty();
    }

    @Test
    void unknownOrAmbiguousContent_isNotRecognised() throws IOException {
        assertThat(detect(TestFiles.text())).isEmpty();
        assertThat(detect(TestFiles.plainZip())).as("Office dosyası olmayan zip").isEmpty();
        assertThat(detect(TestFiles.matroska())).as("webm olmayan Matroska").isEmpty();
        assertThat(detect(new byte[0])).as("boş dosya").isEmpty();
    }

    private Optional<FileFormat> detect(byte[] content) throws IOException {
        Path file = Files.createTempFile(dir, "dosya", ".bin");
        Files.write(file, content);
        return detector.detect(file);
    }
}
