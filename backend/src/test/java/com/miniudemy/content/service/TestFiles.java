package com.miniudemy.content.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Testlerde kullanılan, gerçek dosyaların ilk baytlarını taşıyan küçük örnekler. */
final class TestFiles {

    private TestFiles() {
    }

    static byte[] mp4() {
        return concat(new byte[] {0, 0, 0, 0x18}, ascii("ftypisom"), new byte[64]);
    }

    static byte[] webm() {
        // EBML başlığı (1A 45 DF A3) + DocType "webm"
        return concat(new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x9F, 0x42, (byte) 0x86, (byte) 0x81,
            0x01, 0x42, (byte) 0x82, (byte) 0x84}, ascii("webm"), new byte[48]);
    }

    static byte[] matroska() {
        return concat(new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0x42, (byte) 0x82, (byte) 0x88},
                ascii("matroska"), new byte[48]);
    }

    static byte[] pdf() {
        return ascii("%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n");
    }

    static byte[] jpeg() {
        return concat(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, new byte[32]);
    }

    static byte[] png() {
        return concat(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, new byte[32]);
    }

    static byte[] webp() {
        return concat(ascii("RIFF"), new byte[] {0x24, 0, 0, 0}, ascii("WEBPVP8 "), new byte[32]);
    }

    static byte[] docx() {
        return zip("[Content_Types].xml", "word/document.xml");
    }

    static byte[] pptx() {
        return zip("[Content_Types].xml", "ppt/presentation.xml");
    }

    static byte[] plainZip() {
        return zip("notlar.txt");
    }

    /** Windows çalıştırılabilir dosyası (MZ başlığı). */
    static byte[] exe() {
        return concat(ascii("MZ"), new byte[62]);
    }

    static byte[] text() {
        return "merhaba dünya".getBytes(StandardCharsets.UTF_8);
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static byte[] zip(String... entries) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (String name : entries) {
                    zip.putNextEntry(new ZipEntry(name));
                    zip.write(ascii("<x/>"));
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
