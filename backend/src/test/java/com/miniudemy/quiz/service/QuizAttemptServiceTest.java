package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.mapper.QuizAttemptMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Deneme geçmişi (#55). */
class QuizAttemptServiceTest {

    private static final long COURSE = 3L;
    private static final long SECTION = 11L;
    private static final long STUDENT = 42L;
    private static final long QUIZ = 31L;

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final QuizAttemptRepository attemptRepository = mock(QuizAttemptRepository.class);
    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final EnrollmentService enrollment = mock(EnrollmentService.class);
    private final QuizAttemptService service = new QuizAttemptService(quizRepository, attemptRepository,
            mock(QuestionRepository.class), Mappers.getMapper(QuizAttemptMapper.class), ownership, enrollment,
            mock(ScoringStrategyResolver.class), mock(ApplicationEventPublisher.class));

    @BeforeEach
    void stubs() {
        Quiz quiz = new Quiz(SECTION, "Bölüm 1 sınavı", ScoringType.SINGLE_ANSWER, 70, true);
        ReflectionTestUtils.setField(quiz, "id", QUIZ);
        given(quizRepository.findById(QUIZ)).willReturn(Optional.of(quiz));
        given(ownership.findCourseIdOfSection(SECTION)).willReturn(COURSE);
    }

    private static QuizAttempt attempt(long id, long student, String score, String max, boolean passed, String at) {
        QuizAttempt a = new QuizAttempt(QUIZ, student, new BigDecimal(score), new BigDecimal(max), passed, List.of());
        ReflectionTestUtils.setField(a, "id", id);
        ReflectionTestUtils.setField(a, "submittedAt", Instant.parse(at));
        return a;
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    @Test
    void listMyAttempts_returnsRepositoryOrderWithContractFields() {
        given(attemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(QUIZ, STUDENT)).willReturn(List.of(
                attempt(2L, STUDENT, "35.00", "50.00", true, "2026-11-17T10:00:00Z"),
                attempt(1L, STUDENT, "10.00", "50.00", false, "2026-11-16T10:00:00Z")));

        List<AttemptSummaryResponse> result = service.listMyAttempts(QUIZ, STUDENT);

        assertThat(result).extracting(AttemptSummaryResponse::id).containsExactly(2L, 1L);
        AttemptSummaryResponse first = result.get(0);
        assertThat(first.quizId()).isEqualTo(QUIZ);
        assertThat(first.score()).isEqualByComparingTo("35");
        assertThat(first.maxScore()).isEqualByComparingTo("50");
        assertThat(first.percent()).isEqualByComparingTo("70.0");
        assertThat(first.passed()).isTrue();
        assertThat(first.submittedAt()).isEqualTo(Instant.parse("2026-11-17T10:00:00Z"));
        verify(enrollment).assertEnrolled(STUDENT, COURSE);
    }

    @Test
    void listMyAttempts_queriesOnlyTheCallersAttemptsWithOneQuery() {
        service.listMyAttempts(QUIZ, STUDENT);

        verify(attemptRepository, times(1)).findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(QUIZ, STUDENT);
        verify(attemptRepository, never()).findAll();
        verify(attemptRepository, never()).findById(anyLong());
    }

    @Test
    void listMyAttempts_otherStudentsAttemptsAreNotVisible() {
        given(attemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(QUIZ, 99L)).willReturn(List.of(
                attempt(5L, 99L, "50.00", "50.00", true, "2026-11-17T10:00:00Z")));

        assertThat(service.listMyAttempts(QUIZ, STUDENT)).isEmpty();
    }

    @Test
    void listMyAttempts_percentRoundsToOneDecimal() {
        given(attemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(QUIZ, STUDENT)).willReturn(List.of(
                attempt(1L, STUDENT, "1.00", "3.00", false, "2026-11-17T10:00:00Z")));

        assertThat(service.listMyAttempts(QUIZ, STUDENT).get(0).percent()).isEqualByComparingTo("33.3");
    }

    @Test
    void listMyAttempts_zeroMaxScore_percentIsZero() {
        given(attemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(QUIZ, STUDENT)).willReturn(List.of(
                attempt(1L, STUDENT, "0.00", "0.00", false, "2026-11-17T10:00:00Z")));

        assertThat(service.listMyAttempts(QUIZ, STUDENT).get(0).percent()).isEqualByComparingTo("0");
    }

    @Test
    void listMyAttempts_unknownQuiz_returns404() {
        assertThatThrownBy(() -> service.listMyAttempts(999L, STUDENT))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
        verify(attemptRepository, never()).findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(anyLong(), anyLong());
    }

    @Test
    void listMyAttempts_notEnrolled_returns403() {
        willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz.")).given(enrollment)
                .assertEnrolled(STUDENT, COURSE);

        assertThatThrownBy(() -> service.listMyAttempts(QUIZ, STUDENT))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(attemptRepository, never()).findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(anyLong(), anyLong());
    }
}
