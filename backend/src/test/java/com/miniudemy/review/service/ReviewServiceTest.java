package com.miniudemy.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.miniudemy.review.mapper.ReviewMapperImpl;
import com.miniudemy.review.repository.ReviewRepository;

/** Puan ve yorum (#126): kayıt şartı, tekil değerlendirme, sahiplik, liste ve özet. */
class ReviewServiceTest {

    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final CourseLookupService courseLookup = mock(CourseLookupService.class);
    private final EnrollmentService enrollmentService = mock(EnrollmentService.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final ReviewService service = new ReviewService(repository, courseLookup, enrollmentService,
            userDirectory, new ReviewMapperImpl());

    private static Review review(long id, long userId, long courseId, int rating, String comment) {
        Review r = new Review(userId, courseId, rating, comment);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    private void saveAssignsId99() {
        given(repository.saveAndFlush(any(Review.class))).willAnswer(inv -> {
            Review r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 99L);
            return r;
        });
    }

    private void author(long id, String name) {
        given(userDirectory.findSummaries(any())).willReturn(Map.of(id, new UserSummary(id, name)));
    }

    // ---- create

    @Test
    void create_enrolledStudent_savesAndReturnsReviewWithAuthor() {
        given(enrollmentService.isEnrolled(42L, 5L)).willReturn(true);
        saveAssignsId99();
        author(42L, "Ayşe Kaya");

        ReviewResponse response = service.create(5L, 42L, new ReviewRequest(4, "  Çok iyi  "));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.courseId()).isEqualTo(5L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("Çok iyi");
        assertThat(response.author().fullName()).isEqualTo("Ayşe Kaya");
        assertThat(response.hidden()).isFalse();
        assertThat(response.moderationReason()).isNull();
    }

    @Test
    void create_blankComment_storedAsNull() {
        given(enrollmentService.isEnrolled(42L, 5L)).willReturn(true);
        saveAssignsId99();
        author(42L, "Ayşe Kaya");

        assertThat(service.create(5L, 42L, new ReviewRequest(5, "   ")).comment()).isNull();
    }

    @Test
    void create_notEnrolled_throwsForbidden() {
        given(enrollmentService.isEnrolled(42L, 5L)).willReturn(false);

        assertThatThrownBy(() -> service.create(5L, 42L, new ReviewRequest(5, null)))
                .isInstanceOf(ForbiddenException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_courseNotPublished_throwsNotFound() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(courseLookup).assertPublished(5L);

        assertThatThrownBy(() -> service.create(5L, 42L, new ReviewRequest(5, null)))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_secondReview_throwsConflict() {
        given(enrollmentService.isEnrolled(42L, 5L)).willReturn(true);
        given(repository.existsByUserIdAndCourseId(42L, 5L)).willReturn(true);

        assertThatThrownBy(() -> service.create(5L, 42L, new ReviewRequest(3, null)))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_concurrentDuplicate_uniqueViolationBecomesConflict() {
        given(enrollmentService.isEnrolled(42L, 5L)).willReturn(true);
        given(repository.saveAndFlush(any(Review.class))).willThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.create(5L, 42L, new ReviewRequest(3, null)))
                .isInstanceOf(ConflictException.class);
    }

    // ---- update

    @Test
    void update_owner_changesRatingAndComment() {
        Review existing = review(9L, 42L, 5L, 2, "Eh");
        given(repository.findById(9L)).willReturn(Optional.of(existing));
        author(42L, "Ayşe Kaya");

        ReviewResponse response = service.update(9L, 42L, new ReviewRequest(5, "Düzelttim"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Düzelttim");
        assertThat(existing.getRating()).isEqualTo(5);
    }

    @Test
    void update_someoneElsesReview_throwsForbiddenAndKeepsReview() {
        Review existing = review(9L, 42L, 5L, 2, "Eh");
        given(repository.findById(9L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(9L, 43L, new ReviewRequest(5, "Hile")))
                .isInstanceOf(ForbiddenException.class);
        assertThat(existing.getRating()).isEqualTo(2);
        assertThat(existing.getComment()).isEqualTo("Eh");
    }

    @Test
    void update_unknownReview_throwsNotFound() {
        given(repository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9L, 42L, new ReviewRequest(5, null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- list

    private static ReviewRepository.RatingCount count(int rating, long total) {
        return new ReviewRepository.RatingCount() {
            @Override
            public int getRating() {
                return rating;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    @Test
    void listCourseReviews_returnsPageAuthorsAndSummary() {
        Pageable pageable = PageRequest.of(0, 20);
        given(repository.findByCourseIdAndHiddenFalse(any(), any(Pageable.class))).willReturn(new PageImpl<>(
                List.of(review(2L, 43L, 5L, 5, "Harika"), review(1L, 42L, 5L, 3, null)), pageable, 2));
        given(userDirectory.findSummaries(any())).willReturn(Map.of(
                42L, new UserSummary(42L, "Ayşe Kaya"), 43L, new UserSummary(43L, "Can Er")));
        given(repository.countVisibleByRating(5L)).willReturn(List.of(count(5, 1), count(3, 1)));

        CourseReviewsResponse response = service.listCourseReviews(5L, 0, 20);

        assertThat(response.content()).extracting(r -> r.author().fullName()).containsExactly("Can Er", "Ayşe Kaya");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.summary().reviewCount()).isEqualTo(2);
        assertThat(response.summary().averageRating()).isEqualTo(4.0);
        assertThat(response.summary().distribution()).containsEntry("5", 1L).containsEntry("3", 1L)
                .containsEntry("1", 0L);
    }

    @Test
    void listCourseReviews_noReviews_returnsEmptyPageAndNullAverage() {
        given(repository.findByCourseIdAndHiddenFalse(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        given(userDirectory.findSummaries(any())).willReturn(Map.of());
        given(repository.countVisibleByRating(5L)).willReturn(List.of());

        CourseReviewsResponse response = service.listCourseReviews(5L, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.summary().averageRating()).isNull();
        assertThat(response.summary().reviewCount()).isZero();
        assertThat(response.summary().distribution()).hasSize(5).containsValues(0L, 0L, 0L, 0L, 0L);
    }

    @Test
    void listCourseReviews_courseNotPublished_throwsNotFound() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(courseLookup).assertPublished(5L);

        assertThatThrownBy(() -> service.listCourseReviews(5L, 0, 20)).isInstanceOf(NotFoundException.class);
    }

    // ---- assertVisible (#144, şikâyet)

    @Test
    void assertVisible_visibleReview_passes() {
        given(repository.findById(9L)).willReturn(Optional.of(review(9L, 42L, 5L, 2, "Kötü")));

        service.assertVisible(9L);

        verify(courseLookup).assertPublished(5L);
    }

    @Test
    void assertVisible_visibleReviewOfUnpublishedCourse_throwsNotFound() {
        given(repository.findById(9L)).willReturn(Optional.of(review(9L, 42L, 5L, 2, "Kötü")));
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(courseLookup).assertPublished(5L);

        assertThatThrownBy(() -> service.assertVisible(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void assertVisible_hiddenOrMissingReview_throwsNotFound() {
        Review hidden = review(9L, 42L, 5L, 2, "Kötü");
        ReflectionTestUtils.setField(hidden, "hidden", true);
        given(repository.findById(9L)).willReturn(Optional.of(hidden));
        given(repository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertVisible(9L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.assertVisible(10L)).isInstanceOf(NotFoundException.class);
    }
}
