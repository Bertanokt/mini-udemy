package com.miniudemy.quiz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;

class QuizAttemptMapperTest {

    private final QuizAttemptMapper mapper = Mappers.getMapper(QuizAttemptMapper.class);

    @Test
    void toSummary_copiesAllContractFieldsAndComputesPercent() {
        QuizAttempt attempt = new QuizAttempt(31L, 42L, new BigDecimal("35.00"), new BigDecimal("50.00"), true, List.of());
        ReflectionTestUtils.setField(attempt, "id", 9L);
        ReflectionTestUtils.setField(attempt, "submittedAt", Instant.parse("2026-11-17T10:00:00Z"));

        AttemptSummaryResponse response = mapper.toSummary(attempt);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.quizId()).isEqualTo(31L);
        assertThat(response.score()).isEqualByComparingTo("35");
        assertThat(response.maxScore()).isEqualByComparingTo("50");
        assertThat(response.percent()).isEqualByComparingTo("70.0");
        assertThat(response.passed()).isTrue();
        assertThat(response.submittedAt()).isEqualTo(Instant.parse("2026-11-17T10:00:00Z"));
    }

    @Test
    void toSummary_doesNotExposeStudentId() {
        assertThat(AttemptSummaryResponse.class.getRecordComponents())
                .extracting(c -> c.getName()).doesNotContain("studentId");
    }
}
