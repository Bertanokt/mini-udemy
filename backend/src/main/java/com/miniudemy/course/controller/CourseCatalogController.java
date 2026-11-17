package com.miniudemy.course.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseSummaryPage;
import com.miniudemy.course.service.CatalogSort;
import com.miniudemy.course.service.CourseCatalogService;

/** Katalog (docs/api/openapi.yaml › Catalog): herkese açık arama ve kurs detayı. */
@RestController
@RequestMapping("/api/courses")
public class CourseCatalogController {

    private final CourseCatalogService catalogService;

    public CourseCatalogController(CourseCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Herkese açık (SecurityConfig › PUBLIC_GET). Geçersiz sorgu parametresi 400 olur. */
    @GetMapping
    public CourseSummaryPage searchCatalog(
            @RequestParam(required = false) @Size(max = 100, message = "En fazla 100 karakter olabilir.") String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) Long instructorId,
            @RequestParam(required = false)
            @DecimalMin(value = "1", message = "En az 1 olabilir.")
            @DecimalMax(value = "5", message = "En fazla 5 olabilir.") Double minRating,
            @RequestParam(defaultValue = "newest")
            @Pattern(regexp = "newest|title|rating|popular",
                    message = "newest, title, rating ya da popular olmalı.") String sort,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return catalogService.search(q, categoryId, level, instructorId, minRating, CatalogSort.fromValue(sort),
                page, size);
    }

    /**
     * Herkese açık; token isteğe bağlı. Token varsa (geçerliyse) kullanıcı ve rolü okunur: yayımlanmamış kurs yalnız
     * sahibine ve ADMIN'e görünür. Token yoksa {@code authentication} null gelir.
     */
    @GetMapping("/{id}")
    public CourseDetailResponse getCourseDetail(@PathVariable Long id, Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        Long viewerId = authenticated ? CurrentUser.id(authentication) : null;
        boolean admin = authenticated && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return catalogService.getDetail(id, viewerId, admin);
    }
}
