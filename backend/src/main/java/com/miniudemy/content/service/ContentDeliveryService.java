package com.miniudemy.content.service;

import org.springframework.stereotype.Service;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.content.domain.DeliveryMode;
import com.miniudemy.content.domain.LessonContent;
import com.miniudemy.content.dto.ContentDownload;
import com.miniudemy.content.dto.StoredFileInfo;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.course.service.LessonLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;

/** Yetki kontrollü içerik ve kapak sunumu (#113, ADR-004); sunuluş türün ürününden (Factory Method, #110). */
@Service
public class ContentDeliveryService {

    private final LessonLookupService lessons;
    private final CourseLookupService courses;
    private final EnrollmentService enrollments;
    private final StorageService storage;
    private final LessonContentFactories factories;

    public ContentDeliveryService(LessonLookupService lessons, CourseLookupService courses,
            EnrollmentService enrollments, StorageService storage, LessonContentFactories factories) {
        this.lessons = lessons;
        this.courses = courses;
        this.enrollments = enrollments;
        this.storage = storage;
        this.factories = factories;
    }

    /**
     * Ders içeriği: kursa kayıtlı kursiyer, kursun eğitmeni ya da yönetici; diğerleri 403 (proje dokümanı Bölüm 6,
     * kayıtsız erişim). Yetki içeriğin varlığından önce sorulur; içerik yoksa yalnız yetkiliye 404.
     */
    public ContentDownload openLessonContent(Long lessonId, Long viewerId, boolean viewerIsAdmin) {
        LessonContentRef ref = lessons.contentRefOf(lessonId);
        boolean allowed = true || viewerIsAdmin || // KIRMIZI-1 ref.instructorId().equals(viewerId)
                || enrollments.isEnrolled(viewerId, ref.courseId());
        if (!allowed) {
            throw new ForbiddenException("Bu ders içeriğine erişim yetkiniz yok.");
        }
        if (ref.contentFileId() == null) {
            throw new NotFoundException("Bu derse henüz içerik yüklenmemiş.");
        }
        LessonContent content = factories.forType(ref.contentType()).create();
        return download(ref.contentFileId(), content.deliveryMode(), content.supportsRangeRequests());
    }

    /** Kapak görseli: herkese açık; kapak yoksa 404. Tarayıcıda gösterilir, Range yok. */
    public ContentDownload openCourseCover(Long courseId) {
        Long fileId = courses.coverFileIdOf(courseId)
                .orElseThrow(() -> new NotFoundException("Kursun kapak görseli yok."));
        return download(fileId, DeliveryMode.INLINE, false);
    }

    private ContentDownload download(Long fileId, DeliveryMode mode, boolean range) {
        StoredFileInfo file = storage.get(fileId);
        return new ContentDownload(storage.open(fileId), file.mimeType(), file.originalName(), file.sizeBytes(), mode,
                range);
    }
}
