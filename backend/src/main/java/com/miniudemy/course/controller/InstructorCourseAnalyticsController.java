package com.miniudemy.course.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.dto.CourseStatsResponse;
import com.miniudemy.course.dto.CourseStudentPage;
import com.miniudemy.course.service.CourseAnalyticsService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Eğitmen paneli istatistik uçları (docs/api/openapi.yaml › Instructor). Adres SecurityConfig'te INSTRUCTOR'a kilitlidir. */
@RestController
@RequestMapping("/api/instructor/courses/{id}")
public class InstructorCourseAnalyticsController {

    private final CourseAnalyticsService analytics;

    public InstructorCourseAnalyticsController(CourseAnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseStatsResponse stats(@PathVariable Long id, Authentication authentication) {
        return analytics.getStats(id, CurrentUser.id(authentication));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseStudentPage students(@PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size,
            Authentication authentication) {
        return analytics.listStudents(id, CurrentUser.id(authentication), page, size);
    }
}
