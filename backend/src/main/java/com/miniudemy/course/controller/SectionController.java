package com.miniudemy.course.controller;

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
import com.miniudemy.course.dto.PositionRequest;
import com.miniudemy.course.dto.SectionRequest;
import com.miniudemy.course.dto.SectionResponse;
import com.miniudemy.course.service.SectionService;

import jakarta.validation.Valid;

/** Bölüm uçları (docs/api/openapi.yaml › Sections). */
@RestController
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    /**
     * Herkese açık; token isteğe bağlı. Token varsa kullanıcı ve rolü okunur: yayımlanmamış kursun bölümleri yalnız
     * sahibine ve ADMIN'e görünür.
     */
    @GetMapping("/api/courses/{id}/sections")
    public List<SectionResponse> listSections(@PathVariable Long id, Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        Long viewerId = authenticated ? CurrentUser.id(authentication) : null;
        boolean admin = authenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return sectionService.listSections(id, viewerId, admin);
    }

    @PostMapping("/api/courses/{id}/sections")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@PathVariable Long id, @Valid @RequestBody SectionRequest request,
            Authentication authentication) {
        return sectionService.createSection(id, CurrentUser.id(authentication), request);
    }

    @PutMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public SectionResponse updateSection(@PathVariable Long id, @Valid @RequestBody SectionRequest request,
            Authentication authentication) {
        return sectionService.updateSection(id, CurrentUser.id(authentication), request);
    }

    @DeleteMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable Long id, Authentication authentication) {
        sectionService.deleteSection(id, CurrentUser.id(authentication));
    }

    @PutMapping("/api/sections/{id}/position")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<SectionResponse> moveSection(@PathVariable Long id, @Valid @RequestBody PositionRequest request,
            Authentication authentication) {
        return sectionService.moveSection(id, CurrentUser.id(authentication), request.position());
    }
}
