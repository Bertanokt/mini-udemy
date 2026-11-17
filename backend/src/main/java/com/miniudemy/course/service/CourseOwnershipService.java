package com.miniudemy.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.repository.CourseRepository;

/**
 * Kurs sahipliği denetimi. Başka modüller (ör. quiz) kursun repository'sine ya da entity'sine dokunmadan
 * "bu kullanıcı bu kursun eğitmeni mi" sorusunu yalnız buradan sorar.
 */
@Service
public class CourseOwnershipService {

    private final CourseRepository courseRepository;

    public CourseOwnershipService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
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
}
