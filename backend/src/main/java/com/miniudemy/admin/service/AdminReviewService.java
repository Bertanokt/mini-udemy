package com.miniudemy.admin.service;

import org.springframework.stereotype.Service;

import com.miniudemy.review.dto.ReviewPage;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.service.ReviewModerationService;

/**
 * Yönetici yorum moderasyonu (#142). Kural ve reviews tablosu review modülündedir; admin modülü review'un yalnız
 * service arayüzünü kullanır (docs/architecture.md › İzin verilen modül bağımlılıkları).
 */
@Service
public class AdminReviewService {

    private final ReviewModerationService moderation;

    public AdminReviewService(ReviewModerationService moderation) {
        this.moderation = moderation;
    }

    public ReviewPage list(Long courseId, Boolean hidden, int page, int size) {
        return moderation.list(courseId, hidden, page, size);
    }

    public ReviewResponse setVisibility(Long reviewId, boolean hidden, String reason) {
        return moderation.setVisibility(reviewId, hidden, reason);
    }
}
