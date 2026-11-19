package com.miniudemy.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.review.domain.Review;
import com.miniudemy.review.dto.ReviewPage;
import com.miniudemy.review.dto.ReviewResponse;
import com.miniudemy.review.mapper.ReviewMapperImpl;
import com.miniudemy.review.repository.ReviewRepository;

/** Yorum moderasyonu (#142): gizle / geri aç, gerekçe kuralı, bulunamadı, yönetici listesi. */
class ReviewModerationServiceTest {

    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final ReviewModerationService service = new ReviewModerationService(repository, userDirectory,
            new ReviewMapperImpl());

    private Review review(long id, long userId) {
        Review r = new Review(userId, 5L, 2, "Ses kötü");
        ReflectionTestUtils.setField(r, "id", id);
        given(repository.findById(id)).willReturn(Optional.of(r));
        given(userDirectory.findSummaries(any())).willReturn(Map.of(userId, new UserSummary(userId, "Ayşe Kaya")));
        return r;
    }

    @Test
    void hide_storesReasonAndReturnsItInAdminResponse() {
        Review r = review(9L, 42L);

        ReviewResponse response = service.setVisibility(9L, true, "  Hakaret içeriyor  ");

        assertThat(r.isHidden()).isTrue();
        assertThat(r.getModerationReason()).isEqualTo("Hakaret içeriyor");
        assertThat(response.hidden()).isTrue();
        assertThat(response.moderationReason()).isEqualTo("Hakaret içeriyor");
        assertThat(response.author().fullName()).isEqualTo("Ayşe Kaya");
    }

    @Test
    void hide_withBlankReason_returns400OnReasonFieldAndKeepsReviewVisible() {
        Review r = review(9L, 42L);

        assertThatThrownBy(() -> service.setVisibility(9L, true, "   "))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((BadRequestException) e).getErrors()).containsKey("reason"));
        assertThat(r.isHidden()).isFalse();
    }

    @Test
    void unhide_makesReviewVisibleAndClearsReason() {
        Review r = review(9L, 42L);
        r.hide("Hakaret içeriyor");

        ReviewResponse response = service.setVisibility(9L, false, null);

        assertThat(r.isHidden()).isFalse();
        assertThat(r.getModerationReason()).isNull();
        assertThat(response.hidden()).isFalse();
        assertThat(response.moderationReason()).isNull();
    }

    @Test
    void setVisibility_unknownReview_returns404() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setVisibility(99L, true, "Hakaret içeriyor"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void list_passesFiltersAndReturnsHiddenReviewsWithReason() {
        Review r = new Review(42L, 5L, 2, "Ses kötü");
        ReflectionTestUtils.setField(r, "id", 9L);
        r.hide("Hakaret içeriyor");
        given(repository.searchForAdmin(eq(5L), eq(true), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 20), 1));
        given(userDirectory.findSummaries(any())).willReturn(Map.of(42L, new UserSummary(42L, "Ayşe Kaya")));

        ReviewPage page = service.list(5L, true, 0, 20);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(9L);
            assertThat(item.hidden()).isTrue();
            assertThat(item.moderationReason()).isEqualTo("Hakaret içeriyor");
        });
    }
}
