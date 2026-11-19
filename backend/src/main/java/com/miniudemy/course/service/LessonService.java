package com.miniudemy.course.service;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.ContentFileDetached;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLessonsChanged;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonPublished;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.LessonRequest;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.LessonRepository;
import com.miniudemy.course.repository.SectionRepository;

/**
 * Ders ekleme, güncelleme, silme ve sıralama (#25, FR-08). Her işlemde sıra aynıdır: kayıt yoksa 404, kullanıcı
 * kursun eğitmeni değilse 403, kursun durumu izin vermiyorsa 409. Durum kararını kursun durum nesnesi verir
 * (State, #85); burada kurs durumuna bakan bir koşul yoktur.
 *
 * <ul>
 *   <li>Ekleme: yeni ders bölümün sonuna gider. Kurs Yayımlandı durumundaysa {@link LessonPublished} yayınlanır
 *       (Observer, ADR-009); dinleyici işlem commit olunca çalışır.</li>
 *   <li>Güncelleme, silme ve sıralama yalnız düzenlenebilir kursta (Taslak, Yayından kaldırıldı) yapılır.</li>
 *   <li>Dersin içerik dosyasının bağı kopunca (ders silinir ya da içerik türü değişir) {@link ContentFileDetached}
 *       yayınlanır; dosyayı diskten content dinleyicisi siler (#112).</li>
 *   <li>Silme ve sıralamadan sonra bölümdeki derslerin {@code position} değerleri 0'dan boşluksuz kalır.</li>
 * </ul>
 */
@Service
public class LessonService {

    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final ApplicationEventPublisher events;

    public LessonService(SectionRepository sectionRepository, LessonRepository lessonRepository,
            CourseMapper courseMapper, ApplicationEventPublisher events) {
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.courseMapper = courseMapper;
        this.events = events;
    }

    @Transactional
    public LessonResponse create(Long instructorId, Long sectionId, LessonRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Bölüm bulunamadı."));
        Course course = section.getCourse();
        assertOwner(course, instructorId);
        course.assertLessonAddable();

        Lesson lesson = new Lesson(request.title(), request.contentType(), request.mandatory(),
                request.durationSeconds(), section.getLessons().size());
        section.addLesson(lesson);
        Lesson saved = lessonRepository.saveAndFlush(lesson);
        if (course.isPublished()) {
            events.publishEvent(new LessonPublished(saved.getId(), course.getId(), Instant.now()));
        }
        events.publishEvent(new CourseLessonsChanged(course.getId()));
        return courseMapper.toLessonResponse(saved);
    }

    @Transactional
    public LessonResponse update(Long instructorId, Long lessonId, LessonRequest request) {
        Lesson lesson = editableLesson(instructorId, lessonId);
        Long previousFile = lesson.getContentFileId();
        lesson.update(request.title(), request.contentType(), request.mandatory(), request.durationSeconds());
        LessonResponse response = courseMapper.toLessonResponse(lessonRepository.saveAndFlush(lesson));
        if (previousFile != null && lesson.getContentFileId() == null) {
            events.publishEvent(new ContentFileDetached(previousFile));
        }
        return response;
    }

    @Transactional
    public void delete(Long instructorId, Long lessonId) {
        Lesson lesson = editableLesson(instructorId, lessonId);
        Long courseId = lesson.getSection().getCourse().getId();
        Long contentFile = lesson.getContentFileId();
        lesson.getSection().removeLesson(lesson);
        lessonRepository.flush();
        if (contentFile != null) {
            events.publishEvent(new ContentFileDetached(contentFile));
        }
        events.publishEvent(new CourseLessonsChanged(courseId));
    }

    /** Dersi aynı bölümde verilen sıraya taşır; bölümün yeni sıradaki derslerini döner. */
    @Transactional
    public List<LessonResponse> move(Long instructorId, Long lessonId, int position) {
        Lesson lesson = editableLesson(instructorId, lessonId);
        Section section = lesson.getSection();
        int count = section.getLessons().size();
        if (position >= count) {
            throw BadRequestException.forField("position", "Sıra 0 ile " + (count - 1) + " arasında olmalıdır.");
        }
        section.moveLesson(lesson, position);
        lessonRepository.flush();
        return section.getLessons().stream().map(courseMapper::toLessonResponse).toList();
    }

    /** Dersi bulur (404), sahipliği (403) ve kursun düzenlenebilirliğini (409) denetler. */
    private Lesson editableLesson(Long instructorId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Ders bulunamadı."));
        Course course = lesson.getSection().getCourse();
        assertOwner(course, instructorId);
        course.assertEditable();
        return lesson;
    }

    private static void assertOwner(Course course, Long instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new ForbiddenException("Bu kurs size ait değil.");
        }
    }
}
