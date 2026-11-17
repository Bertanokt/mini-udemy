package com.miniudemy.quiz.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.quiz.dto.QuestionRequest;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.service.QuestionService;

import jakarta.validation.Valid;

/** Soru bankası uçları (docs/api/openapi.yaml › Quizzes). Rol INSTRUCTOR; kurs sahipliğini service denetler. */
@RestController
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/api/courses/{id}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<QuestionResponse> listQuestions(@PathVariable Long id, Authentication authentication) {
        return questionService.listQuestions(id, CurrentUser.id(authentication));
    }

    @PostMapping("/api/courses/{id}/questions")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {
        return questionService.createQuestion(id, CurrentUser.id(authentication), request);
    }

    @PutMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public QuestionResponse updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request,
            Authentication authentication) {
        return questionService.updateQuestion(id, CurrentUser.id(authentication), request);
    }

    @DeleteMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable Long id, Authentication authentication) {
        questionService.deleteQuestion(id, CurrentUser.id(authentication));
    }
}
