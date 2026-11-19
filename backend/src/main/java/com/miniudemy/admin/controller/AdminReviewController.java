package com.miniudemy.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.admin.dto.ReviewVisibilityRequest;
import com.miniudemy.admin.service.AdminReviewService;
import com.miniudemy.review.dto.ReviewPage;
import com.miniudemy.review.dto.ReviewResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Yönetici yorum moderasyonu uçları (docs/api/openapi.yaml › adminListReviews, adminSetReviewVisibility). Yalnız
 * ADMIN: {@code /api/admin/**} adres kuralıyla korunur (SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public ReviewPage adminListReviews(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olabilir.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return adminReviewService.list(courseId, hidden, page, size);
    }

    @PutMapping("/{id}/visibility")
    public ReviewResponse adminSetReviewVisibility(@PathVariable Long id,
            @Valid @RequestBody ReviewVisibilityRequest request) {
        return adminReviewService.setVisibility(id, request.hidden(), request.reason());
    }
}
