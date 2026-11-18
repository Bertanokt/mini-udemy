package com.miniudemy.course.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.service.CourseService;

/** Eğitmen uçları (docs/api/openapi.yaml › Instructor). Adres ayrıca SecurityConfig'te INSTRUCTOR'a kilitlidir. */
@RestController
@RequestMapping("/api/instructor/courses")
public class InstructorCourseController {

    private final CourseService courseService;

    public InstructorCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** Giriş yapan eğitmenin kursları; {@code status} isteğe bağlı (geçersiz değer 400). */
    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<CourseResponse> listMine(@RequestParam(required = false) CourseStatus status,
            Authentication authentication) {
        return courseService.listByInstructor(CurrentUser.id(authentication), status);
    }
}
