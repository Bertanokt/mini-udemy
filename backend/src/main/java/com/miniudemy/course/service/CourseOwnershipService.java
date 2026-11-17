package com.miniudemy.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.SectionRepository;

/**
 * Kurs sahipliği denetimi. Başka modüller (ör. quiz) kursun repository'sine ya da entity'sine dokunmadan
 * "bu kullanıcı bu kursun eğitmeni mi" sorusunu yalnız buradan sorar.
 */
@Service
public class CourseOwnershipService {

    private final CourseRepository courseRepository;

    private final SectionRepository sectionRepository;

    public CourseOwnershipService(CourseRepository courseRepository, SectionRepository sectionRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
    }

    /** Kurs yoksa 404, kullanıcı kursun eğitmeni değilse 403. */
    @Transactional(readOnly = true)
    public void assertOwner(Long courseId, Long userId) {
        Long instructorId = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Kurs bulunamadı."))
                .getInstructorId();
        if (!instructorId.equals(userId)) {
            throw new ForbiddenException("Bu kurs size ait değil.");
        }
    }

    /** Bölümün kursunun id'si; bölüm yoksa 404. */
    @Transactional(readOnly = true)
    public Long findCourseIdOfSection(Long sectionId) {
        return sectionRepository.findCourseIdById(sectionId)
                .orElseThrow(() -> new NotFoundException("Bölüm bulunamadı."));
    }

    /**
     * Kurs yoksa 404, kullanıcı eğitmeni değilse 403, kurs düzenlenemez durumdaysa 409. Düzenlenebilirliği
     * kursun durumu belirler ({@link Course#isEditable()}); #85'te State nesnesine yönlenecek.
     */
    @Transactional(readOnly = true)
    public void assertOwnerAndEditable(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Kurs bulunamadı."));
        if (!course.getInstructorId().equals(userId)) {
            throw new ForbiddenException("Bu kurs size ait değil.");
        }
        if (!course.isEditable()) {
            throw new ConflictException(
                    "Kurs bu durumdayken düzenlenemez; yalnız taslak ve yayından kaldırılmış kurslar düzenlenebilir.");
        }
    }
}
