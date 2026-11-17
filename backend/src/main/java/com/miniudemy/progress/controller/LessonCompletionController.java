package com.miniudemy.progress.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.service.LessonCompletionService;

/** Ders tamamlama (docs/api/openapi.yaml › completeLesson). */
@RestController
public class LessonCompletionController {

    private final LessonCompletionService completion;

    public LessonCompletionController(LessonCompletionService completion) {
        this.completion = completion;
    }

    @PostMapping("/api/lessons/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public CourseProgressResponse complete(@PathVariable Long id, Authentication authentication) {
        return completion.complete(CurrentUser.id(authentication), id);
    }
}
