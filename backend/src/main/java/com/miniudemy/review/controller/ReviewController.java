package com.miniudemy.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.common.security.CurrentUser;
import com.miniudemy.review.dto.CourseReviewsResponse;
import com.miniudemy.review.dto.ReviewRequest;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.service.ReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Değerlendirme uçları (docs/api/openapi.yaml › Reviews). Liste herkese açık; yazma uçları STUDENT. */
@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/courses/{id}/reviews")
    public CourseReviewsResponse listCourseReviews(@PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "0 ya da daha büyük olmalı.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "En az 1 olmalı.")
            @Max(value = 100, message = "En fazla 100 olabilir.") int size) {
        return reviewService.listCourseReviews(id, page, size);
    }

    @PostMapping("/api/courses/{id}/reviews")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return reviewService.create(id, CurrentUser.id(authentication), request);
    }

    @PutMapping("/api/reviews/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ReviewResponse updateReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        return reviewService.update(id, CurrentUser.id(authentication), request);
    }
}
