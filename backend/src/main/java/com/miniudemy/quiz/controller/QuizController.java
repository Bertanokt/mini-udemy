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
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuizRequest;
import com.miniudemy.quiz.dto.QuizSummaryResponse;
import com.miniudemy.quiz.dto.StudentQuizResponse;
import com.miniudemy.quiz.service.QuizService;

import jakarta.validation.Valid;

/**
 * Bölüm sınavı uçları (docs/api/openapi.yaml › Quizzes). Rol kapısı burada; kurs sahipliği, kayıt ve düzenlenebilirlik
 * denetimi service'tedir. Öğrenci ve eğitmen okuması ayrı uçlar ve ayrı DTO'lardır.
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

    @GetMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentQuizResponse getQuizForStudent(@PathVariable Long id, Authentication authentication) {
        return quizService.getQuizForStudent(id, CurrentUser.id(authentication));
    }

    @GetMapping("/api/instructor/quizzes/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public InstructorQuizResponse getQuizForInstructor(@PathVariable Long id, Authentication authentication) {
        return quizService.getQuizForInstructor(id, CurrentUser.id(authentication));
    }

    @GetMapping("/api/sections/{id}/quizzes")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public List<QuizSummaryResponse> listSectionQuizzes(@PathVariable Long id, Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority().substring("ROLE_".length());
        return quizService.listSectionQuizzes(id, CurrentUser.id(authentication), role);
    }
}
