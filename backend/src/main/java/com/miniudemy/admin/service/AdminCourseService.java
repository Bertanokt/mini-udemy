package com.miniudemy.admin.service;

import org.springframework.stereotype.Service;

import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.service.CourseCatalogService;
import com.miniudemy.course.service.CourseLifecycleService;

/**
 * Yönetici kurs işlemleri: onay kuyruğu, onay, red (#59), yayından kaldırma (#123). Kural course modülünde (State deseni, #85); admin modülü
 * course'un yalnız service arayüzünü kullanır (docs/architecture.md › İzin verilen modül bağımlılıkları).
 */
@Service
public class AdminCourseService {

    private final CourseCatalogService catalog;
    private final CourseLifecycleService lifecycle;

    public AdminCourseService(CourseCatalogService catalog, CourseLifecycleService lifecycle) {
        this.catalog = catalog;
        this.lifecycle = lifecycle;
    }

    public CourseSummaryPage list(CourseStatus status, int page, int size) {
        return catalog.listByStatus(status, page, size);
    }

    public CourseResponse approve(Long courseId) {
        return lifecycle.approve(courseId);
    }

    public CourseResponse reject(Long courseId, String reason) {
        return lifecycle.reject(courseId, reason);
    }

    /** Yayından kaldırma (#123): kural course'ta, gerekçe zorunluluğu {@code ReasonRequest}'te. */
    public CourseResponse unpublish(Long courseId, String reason) {
        return lifecycle.unpublish(courseId, null); // KIRMIZI-A4
    }
}
