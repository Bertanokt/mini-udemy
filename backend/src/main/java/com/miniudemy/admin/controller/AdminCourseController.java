package com.miniudemy.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.admin.dto.ReasonRequest;
import com.miniudemy.admin.service.AdminCourseService;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CourseSummaryPage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Yönetici kurs uçları (docs/api/openapi.yaml › adminListCourses, adminApproveCourse, adminRejectCourse,
 * adminUnpublishCourse). Yalnız
 * ADMIN: {@code /api/admin/**} adres kuralıyla korunur (SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    /** Onay kuyruğu ve durum bazlı liste; durum verilmezse hepsi. */
    @GetMapping
    public CourseSummaryPage adminListCourses(
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return adminCourseService.list(status, page, size);
    }

    @PostMapping("/{id}/approve")
    public CourseResponse adminApproveCourse(@PathVariable Long id) {
        return adminCourseService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public CourseResponse adminRejectCourse(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return adminCourseService.reject(id, request.reason());
    }

    @PostMapping("/{id}/unpublish")
    public CourseResponse adminUnpublishCourse(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return adminCourseService.unpublish(id, request.reason());
    }
}
