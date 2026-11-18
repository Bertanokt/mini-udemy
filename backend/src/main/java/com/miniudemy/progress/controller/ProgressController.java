package com.miniudemy.progress.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.ResumeResponse;
import com.miniudemy.progress.service.ProgressService;

/** İlerleme uçları (docs/api/openapi.yaml › Progress). Rol STUDENT. */
@RestController
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/api/me/courses/{id}/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public CourseProgressResponse getMyProgress(@PathVariable Long id, Authentication authentication) {
        return progressService.getMyProgress(CurrentUser.id(authentication), id);
    }

    /** Kaldığı yerden devam (#119); yalnız kursa kayıtlı kursiyer. */
    @GetMapping("/api/me/courses/{id}/resume")
    @PreAuthorize("hasRole('STUDENT')")
    public ResumeResponse getMyResumePoint(@PathVariable Long id, Authentication authentication) {
        return progressService.getMyResumePoint(CurrentUser.id(authentication), id);
    }
}
