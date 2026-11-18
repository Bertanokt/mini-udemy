package com.miniudemy.content.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.miniudemy.content.domain.CoverImage;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.service.ContentLinkService;

/**
 * Ders içeriği ve kapak yükleme (#112, ADR-004). Sıra: izin denetimi (404, 403, 409; dosya yazılmadan) → tür ve
 * boyut kuralıyla diske yazma (413, 415) → dosyayı derse/kursa bağlama (course'un {@link ContentLinkService}'i, bağ
 * orada kurulur ve eski dosya için olay yayınlanır) → eski dosyayı silme (dinleyici). Bağ kurulamazsa yeni dosya
 * silinir, diskte sahipsiz dosya kalmaz.
 *
 * Türe özgü kural burada aranmaz: dersin türünden {@link LessonContentFactories} ürünü verir, ürün de kuralı
 * ({@code filePolicy()}); bu sınıfta tür için if/switch yoktur (Factory Method, ADR-011).
 *
 * Bilerek {@code @Transactional} değil: dosya yazma uzun sürebilir (200 MB) ve her adımın kendi işlemi vardır.
 */
@Service
public class ContentUploadService {

    private final ContentLinkService links;
    private final StorageService storage;
    private final LessonContentFactories factories;

    public ContentUploadService(ContentLinkService links, StorageService storage, LessonContentFactories factories) {
        this.links = links;
        this.storage = storage;
        this.factories = factories;
    }

    /** Dersin içeriğini yükler; eski dosya varsa diskten silinir. İçeriği bağlanmış dersi döner. */
    public LessonResponse uploadLessonContent(Long instructorId, Long lessonId, String originalName,
            InputStream content) {
        LessonContentRef lesson = links.assertLessonContentUploadable(instructorId, lessonId);
        StoredFileInfo file = storage.store(originalName, content,
                factories.forType(lesson.contentType()).create().filePolicy(), instructorId);
        try {
            return links.attachLessonContent(instructorId, lessonId, file.id());
        } catch (RuntimeException e) {
            discard(file, e);
            throw e;
        }
    }

    /** Kursun kapağını yükler; eski kapak varsa diskten silinir. Kapağı bağlanmış kursu döner. */
    public CourseResponse uploadCourseCover(Long instructorId, Long courseId, String originalName,
            InputStream content) {
        links.assertCoverUploadable(instructorId, courseId);
        StoredFileInfo file = storage.store(originalName, content, CoverImage.POLICY, instructorId);
        try {
            return links.attachCover(instructorId, courseId, file.id());
        } catch (RuntimeException e) {
            discard(file, e);
            throw e;
        }
    }

    private void discard(StoredFileInfo file, RuntimeException cause) {
        try {
            storage.delete(file.id());
        } catch (RuntimeException suppressed) {
            cause.addSuppressed(suppressed);
        }
    }
}
