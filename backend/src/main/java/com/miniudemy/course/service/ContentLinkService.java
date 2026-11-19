package com.miniudemy.course.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.ContentFileDetached;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.LessonRepository;

/**
 * Yüklenen dosyayı derse ya da kursa bağlar (#112). content modülü dosyayı depolar, bu servis bağı kurar ve izinleri
 * denetler: ders/kurs yoksa 404, kullanıcı kursun eğitmeni değilse 403, kursun durumu izin vermiyorsa 409 (durum
 * kararı State nesnesindedir, #85). Denetim iki kez yapılır: dosya diske yazılmadan önce ({@code assert...} metotları,
 * gereksiz 200 MB'ı yazdırmamak için) ve bağ kurulurken (durum arada değişmiş olabilir).
 *
 * Bağ koparsa eski dosya için {@link ContentFileDetached} yayınlanır; diskten silmeyi content dinleyicisi yapar.
 */
@Service
public class ContentLinkService {

    private final LessonRepository lessonRepository;
    private final CourseOwnershipService ownership;
    private final UserDirectoryService userDirectory;
    private final CourseMapper courseMapper;
    private final ApplicationEventPublisher events;

    public ContentLinkService(LessonRepository lessonRepository, CourseOwnershipService ownership,
            UserDirectoryService userDirectory, CourseMapper courseMapper, ApplicationEventPublisher events) {
        this.lessonRepository = lessonRepository;
        this.ownership = ownership;
        this.userDirectory = userDirectory;
        this.courseMapper = courseMapper;
        this.events = events;
    }

    /** Derse içerik yüklenebilir mi (404, 403, 409); yüklenecek dosyanın kuralı için dersin içerik türünü verir. */
    @Transactional(readOnly = true)
    public LessonContentRef assertLessonContentUploadable(Long instructorId, Long lessonId) {
        Lesson lesson = uploadableLesson(instructorId, lessonId);
        Course course = lesson.getSection().getCourse();
        return new LessonContentRef(lesson.getId(), course.getId(), course.getInstructorId(), lesson.getContentType(),
                lesson.getContentFileId());
    }

    /** Dosyayı derse bağlar; eski dosya varsa silinmek üzere bildirilir. İçeriği bağlanmış dersi döner. */
    @Transactional
    public LessonResponse attachLessonContent(Long instructorId, Long lessonId, Long fileId) {
        Lesson lesson = uploadableLesson(instructorId, lessonId);
        Long previous = lesson.replaceContent(fileId);
        lessonRepository.saveAndFlush(lesson);
        publishDetached(previous);
        return courseMapper.toLessonResponse(lesson);
    }

    /** Kapak yüklenebilir mi (404, 403, 409). */
    @Transactional(readOnly = true)
    public void assertCoverUploadable(Long instructorId, Long courseId) {
        ownership.assertOwnerAndEditable(courseId, instructorId);
    }

    /** Dosyayı kursun kapağı yapar; eski kapak varsa silinmek üzere bildirilir. Kapağı bağlanmış kursu döner. */
    @Transactional
    public CourseResponse attachCover(Long instructorId, Long courseId, Long fileId) {
        Course course = ownership.requireOwnedCourse(courseId, instructorId);
        Long previous = course.replaceCover(fileId);
        publishDetached(previous);
        UserSummary instructor = userDirectory.findSummaries(List.of(instructorId)).get(instructorId);
        if (instructor == null) {
            throw new NotFoundException("Kullanıcı bulunamadı.");
        }
        return courseMapper.toResponse(course, instructor);
    }

    private Lesson uploadableLesson(Long instructorId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Ders bulunamadı."));
        Course course = lesson.getSection().getCourse();
        if (!course.getInstructorId().equals(instructorId)) {
            throw new ForbiddenException("Bu kurs size ait değil.");
        }
        course.assertContentUploadable(lesson);
        return lesson;
    }

    private void publishDetached(Long fileId) {
        if (fileId != null) {
            events.publishEvent(new ContentFileDetached(fileId));
        }
    }
}
