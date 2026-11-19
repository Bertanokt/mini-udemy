package com.miniudemy.course.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.CreateCourseRequest;
import com.miniudemy.course.dto.UpdateCourseRequest;
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

    /**
     * Kursu günceller (#23); yalnız kursun eğitmeni. Başka eğitmenin kursu 403, olmayan kurs 404, düzenlenemez durum
     * 409, hatalı alan ya da olmayan kategori 400.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request,
            Authentication authentication) {
        return courseService.update(id, CurrentUser.id(authentication), request);
    }

    /** Kursu siler (#23); yalnız kursun eğitmeni ve yalnız Taslak'ta. Kursiyeri olan kurs 409. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        courseService.delete(id, CurrentUser.id(authentication));
    }
}
