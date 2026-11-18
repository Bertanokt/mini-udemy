package com.miniudemy.course.controller;

import java.util.List;

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
import com.miniudemy.course.dto.LessonRequest;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.dto.PositionRequest;
import com.miniudemy.course.service.LessonService;

import jakarta.validation.Valid;

/**
 * Ders uçları (docs/api/openapi.yaml › Lessons): ekleme, güncelleme, silme, sıra değiştirme. Yalnız eğitmen;
 * kursun sahibi olup olmadığı ve kursun durumu serviste denetlenir. İçerik yükleme ve sunma uçları content
 * modülündedir.
 */
@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping("/api/sections/{id}/lessons")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@PathVariable Long id, @Valid @RequestBody LessonRequest request,
            Authentication authentication) {
        return lessonService.create(CurrentUser.id(authentication), id, request);
    }

    @PutMapping("/api/lessons/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public LessonResponse update(@PathVariable Long id, @Valid @RequestBody LessonRequest request,
            Authentication authentication) {
        return lessonService.update(CurrentUser.id(authentication), id, request);
    }

    @DeleteMapping("/api/lessons/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        lessonService.delete(CurrentUser.id(authentication), id);
    }

    @PutMapping("/api/lessons/{id}/position")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<LessonResponse> move(@PathVariable Long id, @Valid @RequestBody PositionRequest request,
            Authentication authentication) {
        return lessonService.move(CurrentUser.id(authentication), id, request.position());
    }
}
