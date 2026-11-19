package com.miniudemy.quiz.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.quiz.dto.AttemptResultResponse;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.dto.SubmitAttemptRequest;
import com.miniudemy.quiz.service.QuizAttemptService;

import jakarta.validation.Valid;

/** Sınav gönderme ve öğrencinin sınav denemeleri (docs/api/openapi.yaml › Quizzes). */
@RestController
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    public QuizAttemptController(QuizAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @GetMapping("/api/me/quizzes/{id}/attempts")
    @PreAuthorize("hasRole('INSTRUCTOR')") // KIRMIZI-M1
    public List<AttemptSummaryResponse> listMyAttempts(@PathVariable Long id, Authentication authentication) {
        return attemptService.listMyAttempts(id, CurrentUser.id(authentication));
    }

    @PostMapping("/api/quizzes/{id}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResultResponse submit(@PathVariable Long id, @Valid @RequestBody SubmitAttemptRequest request,
            Authentication authentication) {
        return attemptService.submit(id, CurrentUser.id(authentication), request);
    }
}
