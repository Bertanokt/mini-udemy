package com.miniudemy.quiz.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.service.QuizAttemptService;

/** Öğrencinin sınav denemeleri (docs/api/openapi.yaml › Quizzes). */
@RestController
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    public QuizAttemptController(QuizAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @GetMapping("/api/me/quizzes/{id}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AttemptSummaryResponse> listMyAttempts(@PathVariable Long id, Authentication authentication) {
        return attemptService.listMyAttempts(id, CurrentUser.id(authentication));
    }
}
