package com.miniudemy.review.dto;

import java.util.List;

/** Sayfalı yorumlar ve özet (docs/api/openapi.yaml › CourseReviewsResponse). {@code page} 0'dan başlar. */
public record CourseReviewsResponse(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        RatingSummary summary) {
}
