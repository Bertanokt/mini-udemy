package com.miniudemy.review.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.review.domain.Review;
import com.miniudemy.review.dto.ReviewPage;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.mapper.ReviewMapper;
import com.miniudemy.review.repository.ReviewRepository;

/**
 * Yorum moderasyonu (#142): yönetici yorumu gizler ya da geri açar. Gizli yorum herkese açık listede ve ortalamada
 * yer almaz; bunu {@link ReviewRepository}'nin görünür yorum sorguları sağlar. Rol denetimi çağıran uçtadır
 * ({@code /api/admin/**}); kullanıcının kendi yazma işlemleri {@link ReviewService}'tedir.
 */
@Service
public class ReviewModerationService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ReviewRepository repository;
    private final UserDirectoryService userDirectory;
    private final ReviewMapper mapper;

    public ReviewModerationService(ReviewRepository repository, UserDirectoryService userDirectory,
            ReviewMapper mapper) {
        this.repository = repository;
        this.userDirectory = userDirectory;
        this.mapper = mapper;
    }

    /** Gizliler dahil tüm yorumlar, en yeni önce; {@code courseId} ve {@code hidden} boşsa süzülmez. */
    @Transactional(readOnly = true)
    public ReviewPage list(Long courseId, Boolean hidden, int page, int size) {
        Page<Review> result = repository.searchForAdmin(courseId, hidden, PageRequest.of(page, size, NEWEST_FIRST));
        Map<Long, UserSummary> authors = userDirectory.findSummaries(
                result.getContent().stream().map(Review::getUserId).distinct().toList());
        List<ReviewResponse> content = result.getContent().stream().map(r -> toResponse(r, authors)).toList();
        return new ReviewPage(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** Gizler (gerekçe zorunlu, 400) ya da geri açar (gerekçe silinir). Yorum yoksa 404. */
    @Transactional
    public ReviewResponse setVisibility(Long reviewId, boolean hidden, String reason) {
        Review review = repository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Değerlendirme bulunamadı."));
        if (hidden) {
            review.hide(reason);
        } else {
            review.unhide();
        }
        return toResponse(review, userDirectory.findSummaries(List.of(review.getUserId())));
    }

    private ReviewResponse toResponse(Review review, Map<Long, UserSummary> authors) {
        UserSummary author = authors.getOrDefault(review.getUserId(),
                new UserSummary(review.getUserId(), "Kullanıcı"));
        return mapper.toAdminResponse(review, author);
    }
}
