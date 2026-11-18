package com.miniudemy.course.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.mapper.CourseMapper;
import com.miniudemy.course.repository.CourseRepository;

/** Kurs durum geçişlerinin uçları (State deseni, #85; ADR-010): onaya gönder (#27). */
@Service
public class CourseLifecycleService {

    private final CourseOwnershipService ownership;
    private final CourseRepository courseRepository;
    private final UserDirectoryService userDirectory;
    private final CourseMapper courseMapper;

    public CourseLifecycleService(CourseOwnershipService ownership, CourseRepository courseRepository,
            UserDirectoryService userDirectory, CourseMapper courseMapper) {
        this.ownership = ownership;
        this.courseRepository = courseRepository;
        this.userDirectory = userDirectory;
        this.courseMapper = courseMapper;
    }

    /**
     * Onaya gönder (#27): kurs yoksa 404, eğitmeni değilse 403, durum izin vermiyorsa 409, içerik eksikse 400.
     * Kurs İncelemede olarak kaydedilir ve güncel haliyle döner.
     */
    @Transactional
    public CourseResponse submit(Long courseId, Long instructorId) {
        Course course = ownership.requireOwnedCourse(courseId, instructorId);
        course.submit();
        return toResponse(courseRepository.saveAndFlush(course));
    }

    private CourseResponse toResponse(Course course) {
        UserSummary instructor = userDirectory.findSummaries(List.of(course.getInstructorId()))
                .get(course.getInstructorId());
        if (instructor == null) {
            // Güvenlik filtresi kullanıcının var olduğunu doğruladı; buraya yalnız arada silinen kullanıcıda düşülür.
            throw new NotFoundException("Kullanıcı bulunamadı.");
        }
        return courseMapper.toResponse(course, instructor);
    }
}
