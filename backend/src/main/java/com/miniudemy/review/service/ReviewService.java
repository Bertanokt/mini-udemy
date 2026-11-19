package com.miniudemy.review.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.review.domain.Review;
import com.miniudemy.review.dto.CourseReviewsResponse;
import com.miniudemy.review.dto.ReviewRequest;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.mapper.ReviewMapper;
import com.miniudemy.review.repository.ReviewRepository;

/** Puan ve yorum (#126). Rol denetimi controller'dadır; kayıt ve sahiplik kuralları burada. */
@Service
public class ReviewService {

    private static final String ALREADY_REVIEWED = "Bu kursu zaten değerlendirdiniz.";
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ReviewRepository repository;
    private final CourseLookupService courseLookup;
    private final EnrollmentService enrollmentService;
    private final UserDirectoryService userDirectory;
    private final ReviewMapper mapper;

    public ReviewService(ReviewRepository repository, CourseLookupService courseLookup,
            EnrollmentService enrollmentService, UserDirectoryService userDirectory, ReviewMapper mapper) {
        this.repository = repository;
        this.courseLookup = courseLookup;
        this.enrollmentService = enrollmentService;
        this.userDirectory = userDirectory;
        this.mapper = mapper;
    }

    /**
     * Kursun görünür yorumları (en yeni önce) ve özeti. Sorgu sayısı sayfadaki yorum sayısından bağımsızdır:
     * sayfa, dağılım ve yazar adları için birer sorgu. Kurs yok/yayımlanmamışsa 404.
     */
    @Transactional(readOnly = true)
    public CourseReviewsResponse listCourseReviews(Long courseId, int page, int size) {
        courseLookup.assertPublished(courseId);
        Page<Review> result = repository.findByCourseIdAndHiddenFalse(courseId,
                PageRequest.of(page, size, NEWEST_FIRST));
        Map<Long, UserSummary> authors = userDirectory.findSummaries(
                result.getContent().stream().map(Review::getUserId).distinct().toList());
        List<ReviewResponse> content = result.getContent().stream()
                .map(r -> toResponse(r, authors))
                .toList();
        Map<Integer, Long> counts = new HashMap<>();
        repository.countVisibleByRating(courseId).forEach(row -> counts.put(row.getRating(), row.getTotal()));
        return new CourseReviewsResponse(content, page, size, result.getTotalElements(), result.getTotalPages(),
                RatingMath.summarize(counts));
    }

    /**
     * Kursiyer kursu değerlendirir. Kurs yok/yayımlanmamış 404, kayıtsız 403, ikinci değerlendirme 409. Eşzamanlı
     * iki istekte UNIQUE (user_id, course_id) kısıtı ikincisini reddeder ve o da 409'a çevrilir.
     */
    @Transactional
    public ReviewResponse create(Long courseId, Long userId, ReviewRequest request) {
        courseLookup.assertPublished(courseId);
        if (!enrollmentService.isEnrolled(userId, courseId)) {
            throw new ForbiddenException("Yalnızca kayıtlı olduğunuz kursu değerlendirebilirsiniz.");
        }
        if (repository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ConflictException(ALREADY_REVIEWED);
        }
        Review review;
        try {
            review = repository.saveAndFlush(
                    new Review(userId, courseId, request.rating(), normalize(request.comment())));
        } catch (DataIntegrityViolationException e) {
            // Kurs ve kullanıcı yukarıda doğrulandığı için burada ihlal edilebilecek tek kısıt UNIQUE'tir.
            throw new ConflictException(ALREADY_REVIEWED);
        }
        return toResponse(review, userDirectory.findSummaries(List.of(userId)));
    }

    /** Sahibi kendi değerlendirmesini düzenler. Değerlendirme yoksa 404, başkasınınki 403. */
    @Transactional
    public ReviewResponse update(Long reviewId, Long userId, ReviewRequest request) {
        Review review = repository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Değerlendirme bulunamadı."));
        if (!review.getUserId().equals(userId)) {
            throw new ForbiddenException("Yalnızca kendi değerlendirmenizi düzenleyebilirsiniz.");
        }
        review.edit(request.rating(), normalize(request.comment()));
        return toResponse(review, userDirectory.findSummaries(List.of(userId)));
    }

    /**
     * Yorum var ve herkese görünüyor mu (#144, şikâyet): yoksa ya da gizliyse 404. İkisi birbirinden ayırt edilmez;
     * kullanıcının göremediği yorum şikâyet edilemez.
     */
    @Transactional(readOnly = true)
    public void assertVisible(Long reviewId) {
        boolean visible = repository.findById(reviewId).map(review -> !review.isHidden()).orElse(false);
        if (!visible) {
            throw new NotFoundException("Değerlendirme bulunamadı.");
        }
    }

    private ReviewResponse toResponse(Review review, Map<Long, UserSummary> authors) {
        UserSummary author = authors.getOrDefault(review.getUserId(),
                new UserSummary(review.getUserId(), "Kullanıcı"));
        return mapper.toResponse(review, author);
    }

    /** Boş ya da yalnız boşluktan oluşan yorum "yorum yok" sayılır. */
    private static String normalize(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.strip();
    }
}
