package com.miniudemy.enrollment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.enrollment.dto.EnrollmentResponse;
import com.miniudemy.enrollment.service.EnrollmentService;

/** Kayıt uçları (docs/api/openapi.yaml › Enrollments). Rol STUDENT; diğer roller 403, token yoksa 401. */
@RestController
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/api/courses/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enrollCourse(@PathVariable Long id, Authentication authentication) {
        return enrollmentService.enroll(id, CurrentUser.id(authentication));
    }

    @GetMapping("/api/me/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentResponse> listMyEnrollments(Authentication authentication) {
        return enrollmentService.listMyEnrollments(CurrentUser.id(authentication));
    }
}
