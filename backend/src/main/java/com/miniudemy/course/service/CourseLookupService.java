package com.miniudemy.course.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.repository.CourseRepository;

/**
 * Başka modüllerin (ör. enrollment) kursun repository'sine ya da entity'sine dokunmadan sorduğu iki şey: "bu kurs
 * yayımlanmış mı" ve "bu kursların kartları".
 */
@Service
@Transactional(readOnly = true)
public class CourseLookupService {

    private final CourseRepository courseRepository;
    private final CourseCatalogService catalogService;

    public CourseLookupService(CourseRepository courseRepository, CourseCatalogService catalogService) {
        this.courseRepository = courseRepository;
        this.catalogService = catalogService;
    }

    /** Kurs yoksa ya da yayımlanmamışsa 404; ikisi birbirinden ayırt edilmez (taslağın varlığı açığa çıkmaz). */
    public void assertPublished(Long courseId) {
        boolean published = courseRepository.findById(courseId).map(Course::isPublished).orElse(false);
        if (!published) {
            throw new NotFoundException("Kurs bulunamadı.");
        }
    }

    /** Kurs kartları id ile; kart başına ek sorgu yok. Bulunamayan id haritada yer almaz. */
    public Map<Long, CourseSummaryResponse> findSummaries(Collection<Long> courseIds) {
        Map<Long, CourseSummaryResponse> result = new HashMap<>();
        if (courseIds.isEmpty()) {
            return result;
        }
        catalogService.summaries(courseRepository.findAllByIdIn(courseIds))
                .forEach(card -> result.put(card.id(), card));
        return result;
    }
}
