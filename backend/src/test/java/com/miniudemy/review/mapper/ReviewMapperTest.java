package com.miniudemy.review.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.review.domain.Review;
import com.miniudemy.review.dto.ReviewResponse;

class ReviewMapperTest {

    private final ReviewMapper mapper = new ReviewMapperImpl();

    @Test
    void toResponse_mapsFieldsAndNeverExposesModerationReason() {
        Review review = new Review(42L, 5L, 4, "İyi");
        ReflectionTestUtils.setField(review, "id", 9L);
        ReflectionTestUtils.setField(review, "moderationReason", "Uygunsuz dil");
        Instant at = Instant.parse("2026-11-10T10:00:00Z");
        ReflectionTestUtils.setField(review, "createdAt", at);
        ReflectionTestUtils.setField(review, "updatedAt", at);
        UserSummary author = new UserSummary(42L, "Ayşe Kaya");

        ReviewResponse response = mapper.toResponse(review, author);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.courseId()).isEqualTo(5L);
        assertThat(response.author()).isSameAs(author);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isEqualTo("İyi");
        assertThat(response.hidden()).isFalse();
        assertThat(response.moderationReason()).isNull();
        assertThat(response.createdAt()).isEqualTo(at);
        assertThat(response.updatedAt()).isEqualTo(at);
    }
}
