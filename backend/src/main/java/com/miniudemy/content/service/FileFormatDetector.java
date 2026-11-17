package com.miniudemy.content.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.springframework.stereotype.Component;

import com.miniudemy.content.domain.FileFormat;

/**
 * Dosyanın gerçek türünü içeriğinden (magic bytes) tespit eder (NFR-12, ADR-004). Dosya adındaki uzantıya ve
 * istemcinin gönderdiği Content-Type'a bakılmaz; uzantısı ".mp4" yapılmış bir çalıştırılabilir dosya tanınmaz.
 *
 * <ul>
 *   <li>PDF: {@code %PDF-} · MP4: 4. bayttan itibaren {@code ftyp} (QuickTime {@code qt  } hariç) · WEBM: EBML başlığı
 *       {@code 1A 45 DF A3} ve DocType {@code webm} (diğer Matroska dosyaları değil)</li>
 *   <li>JPEG: {@code FF D8 FF} · PNG: 8 baytlık imza · WEBP: {@code RIFF....WEBP}</li>
 *   <li>DOCX / PPTX: zip ({@code PK 03 04}) ve içinde {@code word/document.xml} / {@code ppt/presentation.xml};
 *       başka bir zip tanınmaz</li>
 * </ul>
 */
@Component
public class FileFormatDetector {

    private static final int HEAD_BYTES = 64;

    public Optional<FileFormat> detect(Path file) {
        byte[] head = readHead(file);
        if (startsWith(head, ascii("%PDF-"))) {
            return Optional.of(FileFormat.PDF);
        }
        if (at(head, 4, ascii("ftyp")) && !at(head, 8, ascii("qt  "))) {
            return Optional.of(FileFormat.MP4);
        }
        if (startsWith(head, bytes(0x1A, 0x45, 0xDF, 0xA3))) {
            return contains(head, ascii("webm")) ? Optional.of(FileFormat.WEBM) : Optional.empty();
        }
        if (startsWith(head, bytes(0xFF, 0xD8, 0xFF))) {
            return Optional.of(FileFormat.JPEG);
        }
        if (startsWith(head, bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) {
            return Optional.of(FileFormat.PNG);
        }
        if (startsWith(head, ascii("RIFF")) && at(head, 8, ascii("WEBP"))) {
            return Optional.of(FileFormat.WEBP);
        }
        if (startsWith(head, bytes(0x50, 0x4B, 0x03, 0x04))) {
            return officeFormat(file);
        }
        return Optional.empty();
    }

    private static Optional<FileFormat> officeFormat(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            if (zip.getEntry("word/document.xml") != null) {
                return Optional.of(FileFormat.DOCX);
            }
            if (zip.getEntry("ppt/presentation.xml") != null) {
                return Optional.of(FileFormat.PPTX);
            }
            return Optional.empty();
        } catch (IOException e) {
            // Bozuk ya da zip gibi görünen ama okunamayan dosya: tanınmaz.
            return Optional.empty();
        }
    }

    private static byte[] readHead(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return in.readNBytes(HEAD_BYTES);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return at(data, 0, prefix);
    }

    private static boolean at(byte[] data, int offset, byte[] part) {
        return data.length >= offset + part.length
                && Arrays.equals(data, offset, offset + part.length, part, 0, part.length);
    }

    private static boolean contains(byte[] data, byte[] part) {
        for (int i = 0; i + part.length <= data.length; i++) {
            if (at(data, i, part)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }
}
