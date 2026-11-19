package com.miniudemy.course.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.UnpublishCourseRequest;
import com.miniudemy.course.service.CourseLifecycleService;

import jakarta.validation.Valid;

/** Kurs durum geçişleri (docs/api/openapi.yaml › submitCourse, unpublishOwnCourse). */
@RestController
public class CourseLifecycleController {

    private final CourseLifecycleService lifecycle;

    public CourseLifecycleController(CourseLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    /** Kursu onaya gönderir; yalnız kursun sahibi eğitmen. */
    @PostMapping("/api/courses/{id}/submit")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseResponse submit(@PathVariable Long id, Authentication authentication) {
        return lifecycle.submit(id, CurrentUser.id(authentication));
    }

    /** Kendi kursunu yayından kaldırır (#123); yalnız kursun sahibi eğitmen. Gövde ve gerekçe isteğe bağlı. */
    @PostMapping("/api/courses/{id}/unpublish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public CourseResponse unpublish(@PathVariable Long id,
            @Valid @RequestBody(required = false) UnpublishCourseRequest request, Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return lifecycle.unpublishOwn(id, CurrentUser.id(authentication), reason);
    }
}
