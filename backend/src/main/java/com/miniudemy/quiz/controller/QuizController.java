package com.miniudemy.quiz.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuizRequest;
import com.miniudemy.quiz.service.QuizService;

import jakarta.validation.Valid;

/**
 * Bölüm sınavı yazma uçları (docs/api/openapi.yaml › Quizzes). Rol INSTRUCTOR; kurs sahipliğini ve
 * düzenlenebilirliği service denetler. Okuma uçları #49'dadır.
 */
@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/api/sections/{id}/quizzes")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public InstructorQuizResponse createQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request,
            Authentication authentication) {
        return quizService.createQuiz(id, CurrentUser.id(authentication), request);
    }

    @PutMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public InstructorQuizResponse updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request,
            Authentication authentication) {
        return quizService.updateQuiz(id, CurrentUser.id(authentication), request);
    }

    @DeleteMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuiz(@PathVariable Long id, Authentication authentication) {
        quizService.deleteQuiz(id, CurrentUser.id(authentication));
    }
}
