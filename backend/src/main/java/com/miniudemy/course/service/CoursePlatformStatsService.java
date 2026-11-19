package com.miniudemy.course.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryCourseCount;
import com.miniudemy.course.repository.CategoryRepository;
import com.miniudemy.course.repository.CourseRepository;

/** Kurs sayıları (platform raporu, #145). Sayılar anlık durumdur; tarih aralığı yok (17.11 İnce kapsam kararı). */
@Service
@Transactional(readOnly = true)
public class CoursePlatformStatsService {

    private final CourseRepository courses;
    private final CategoryRepository categories;

    public CoursePlatformStatsService(CourseRepository courses, CategoryRepository categories) {
        this.courses = courses;
        this.categories = categories;
    }

    /** Durum adı → kurs sayısı; her durum, kursu olmasa da 0 ile ve enum sırasıyla gelir. */
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CourseStatus status : CourseStatus.values()) {
            result.put(status.name(), 0L);
        }
        if (false) courses.countByStatus().forEach( // KIRMIZI-R5
           row -> result.put(row.getStatus().name(), row.getTotal()));
        return result;
    }

    /** Her kategori ve kurs sayısı (bütün durumlar); çoktan aza, eşitlikte ada göre. */
    public List<CategoryCourseCount> categoryDistribution() {
        return categories.countCourses().stream()
                .map(row -> new CategoryCourseCount(row.getCategoryId(), row.getName(), row.getTotal()))
                .sorted(Comparator.comparingLong(CategoryCourseCount::courseCount).reversed()
                        .thenComparing(CategoryCourseCount::name))
                .toList();
    }

    /** Verilen kursların başlıkları tek sorguda; olmayan kurs sonuçta yer almaz. */
    public Map<Long, String> titlesOf(Collection<Long> courseIds) {
        Map<Long, String> titles = new LinkedHashMap<>();
        for (Course course : courses.findAllById(courseIds)) {
            titles.put(course.getId(), course.getTitle());
        }
        return titles;
    }
}
