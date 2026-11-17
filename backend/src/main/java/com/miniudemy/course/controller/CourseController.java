package com.miniudemy.course.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.service.CourseService;

import jakarta.validation.Valid;

/** Kurs uçları (docs/api/openapi.yaml › Courses). */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Kurs oluşturur; yalnız eğitmen (diğer roller 403, token yoksa 401). Eğitmen token'dan alınır; kurs
     * {@code DRAFT} olarak 201 ile döner. Hatalı alanlar 400, olmayan kategori 400 ({@code errors.categoryId}).
     */
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request, Authentication authentication) {
        return courseService.create(CurrentUser.id(authentication), request);
    }
}
