package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.domain.QuizCompleted;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.AnswerRequest;
import com.miniudemy.quiz.dto.AttemptResultResponse;
import com.miniudemy.quiz.dto.SubmitAttemptRequest;
import com.miniudemy.quiz.mapper.QuizAttemptMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Sınav çözme (#54): puanlama, eşik, kayıt, olay ve doğrulamalar. Strateji gerçek resolver'dan gelir. */
class QuizSubmitServiceTest {

    private static final long COURSE = 3L;
    private static final long SECTION = 11L;
    private static final long STUDENT = 42L;
    private static final long QUIZ = 31L;
    // Soru 21: şıklar 84 (doğru), 85. Soru 22: şıklar 88 (doğru), 89. Her soru 10 puan.

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final QuizAttemptRepository attemptRepository = mock(QuizAttemptRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final EnrollmentService enrollment = mock(EnrollmentService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ScoringStrategyResolver resolver = new ScoringStrategyResolver(
            List.of(new SingleAnswerScoring(), new MultiAnswerAllOrNothing(), new MultiAnswerPartialScoring()));
    private final QuizAttemptService service = new QuizAttemptService(quizRepository, attemptRepository,
            questionRepository, Mappers.getMapper(QuizAttemptMapper.class), ownership, enrollment, resolver, events);

    @BeforeEach
    void stubs() {
        Quiz quiz = new Quiz(SECTION, "Bölüm 1 sınavı", ScoringType.SINGLE_ANSWER, 50, true);
        ReflectionTestUtils.setField(quiz, "id", QUIZ);
        quiz.replaceQuestions(List.of(21L, 22L));
        given(quizRepository.findById(QUIZ)).willReturn(Optional.of(quiz));
        given(ownership.findCourseIdOfSection(SECTION)).willReturn(COURSE);
        given(questionRepository.findAllWithOptionsByIdIn(List.of(21L, 22L)))
                .willReturn(List.of(question(22L, 88L, 89L), question(21L, 84L, 85L)));
        given(attemptRepository.save(any(QuizAttempt.class))).willAnswer(call -> {
            QuizAttempt attempt = call.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", 9L);
            return attempt;
        });
    }

    private static Question question(long id, long correctOption, long wrongOption) {
        Question q = new Question(COURSE, "Soru " + id, 10);
        ReflectionTestUtils.setField(q, "id", id);
        Option correct = new Option("A", true);
        ReflectionTestUtils.setField(correct, "id", correctOption);
        Option wrong = new Option("B", false);
        ReflectionTestUtils.setField(wrong, "id", wrongOption);
        q.replaceOptions(List.of(correct, wrong));
        return q;
    }

    private static AnswerRequest answer(long questionId, Long... optionIds) {
        return new AnswerRequest(questionId, List.of(optionIds));
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    @Test
    void submit_allCorrect_passesWithFullScoreAndPerQuestionResult() {
        AttemptResultResponse result = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 84L), answer(22L, 88L))));

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.quizId()).isEqualTo(QUIZ);
        assertThat(result.score()).isEqualByComparingTo("20");
        assertThat(result.maxScore()).isEqualByComparingTo("20");
        assertThat(result.percent()).isEqualByComparingTo("100.0");
        assertThat(result.passed()).isTrue();
        assertThat(result.submittedAt()).isNotNull();
        assertThat(result.questions()).extracting("questionId").containsExactly(21L, 22L);
        assertThat(result.questions()).extracting("correct").containsExactly(true, true);
    }

    @Test
    void submit_belowThreshold_failsAndMarksWrongQuestions() {
        AttemptResultResponse result = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 85L), answer(22L, 89L))));

        assertThat(result.score()).isEqualByComparingTo("0");
        assertThat(result.percent()).isEqualByComparingTo("0");
        assertThat(result.passed()).isFalse();
        assertThat(result.questions()).extracting("correct").containsExactly(false, false);
        assertThat(result.questions()).extracting("earnedPoints").usingComparatorForType(
                java.math.BigDecimal::compareTo, java.math.BigDecimal.class).containsExactly(
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
    }

    @Test
    void submit_exactlyAtThreshold_passes() {
        // Eşik %50; 20 puanın 10'u tam eşik.
        AttemptResultResponse result = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 84L), answer(22L, 89L))));

        assertThat(result.percent()).isEqualByComparingTo("50.0");
        assertThat(result.passed()).isTrue();
        assertThat(result.questions()).extracting("correct").containsExactly(true, false);
    }

    @Test
    void submit_unansweredQuestion_scoresZeroAndStoresNoAnswerForIt() {
        AttemptResultResponse result = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 84L))));

        assertThat(result.score()).isEqualByComparingTo("10");
        assertThat(result.questions()).extracting("correct").containsExactly(true, false);
        ArgumentCaptor<QuizAttempt> saved = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(attemptRepository).save(saved.capture());
        assertThat(saved.getValue().getAnswers()).extracting("questionId").containsExactly(21L);
    }

    @Test
    void submit_savesAttemptWithAnswers() {
        service.submit(QUIZ, STUDENT, new SubmitAttemptRequest(List.of(answer(21L, 84L), answer(22L, 89L))));

        ArgumentCaptor<QuizAttempt> saved = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(attemptRepository).save(saved.capture());
        QuizAttempt attempt = saved.getValue();
        assertThat(attempt.getQuizId()).isEqualTo(QUIZ);
        assertThat(attempt.getStudentId()).isEqualTo(STUDENT);
        assertThat(attempt.getScore()).isEqualByComparingTo("10");
        assertThat(attempt.getMaxScore()).isEqualByComparingTo("20");
        assertThat(attempt.getAnswers()).extracting("questionId", "optionId")
                .containsExactlyInAnyOrder(org.assertj.core.api.Assertions.tuple(21L, 84L),
                        org.assertj.core.api.Assertions.tuple(22L, 89L));
    }

    @Test
    void submit_publishesQuizCompletedWithCourseAndResult() {
        service.submit(QUIZ, STUDENT, new SubmitAttemptRequest(List.of(answer(21L, 84L), answer(22L, 88L))));

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(QuizCompleted.class);
        QuizCompleted completed = (QuizCompleted) event.getValue();
        assertThat(completed.attemptId()).isEqualTo(9L);
        assertThat(completed.quizId()).isEqualTo(QUIZ);
        assertThat(completed.courseId()).isEqualTo(COURSE);
        assertThat(completed.studentId()).isEqualTo(STUDENT);
        assertThat(completed.passed()).isTrue();
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void submit_notEnrolled_returns403AndStoresNothing() {
        willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz.")).given(enrollment)
                .assertEnrolled(STUDENT, COURSE);

        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT, new SubmitAttemptRequest(List.of())))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(attemptRepository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void submit_unknownQuiz_returns404() {
        assertThatThrownBy(() -> service.submit(99L, STUDENT, new SubmitAttemptRequest(List.of())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void submit_questionNotInQuiz_returns400() {
        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(77L, 84L)))))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
        verify(attemptRepository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void submit_optionOfAnotherQuestion_returns400() {
        // 88, soru 22'nin şıkkı; soru 21 için gönderilemez.
        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 88L)))))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void submit_unknownOption_returns400() {
        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 9999L)))))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void submit_sameQuestionTwice_returns400() {
        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 84L), answer(21L, 85L)))))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void submit_sameOptionTwiceInOneAnswer_returns400() {
        assertThatThrownBy(() -> service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(21L, 84L, 84L)))))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    // --- Strateji seçimi (#56): servis sınavın puanlama tipine göre stratejiyi seçer ---

    /** Tek soru (10 puan): 101 ve 102 doğru, 103 yanlış. Sınav verilen puanlama tipinde. */
    private void multiAnswerQuiz(ScoringType type) {
        Quiz quiz = new Quiz(SECTION, "Çok doğrulu sınav", type, 50, true);
        ReflectionTestUtils.setField(quiz, "id", QUIZ);
        quiz.replaceQuestions(List.of(25L));
        given(quizRepository.findById(QUIZ)).willReturn(Optional.of(quiz));
        Question q = new Question(COURSE, "Soru 25", 10);
        ReflectionTestUtils.setField(q, "id", 25L);
        Option a = new Option("A", true);
        ReflectionTestUtils.setField(a, "id", 101L);
        Option b = new Option("B", true);
        ReflectionTestUtils.setField(b, "id", 102L);
        Option c = new Option("C", false);
        ReflectionTestUtils.setField(c, "id", 103L);
        q.replaceOptions(List.of(a, b, c));
        given(questionRepository.findAllWithOptionsByIdIn(List.of(25L))).willReturn(List.of(q));
    }

    @Test
    void submit_partialScoringQuiz_givesProportionalPointsForHalfOfTheCorrectOptions() {
        multiAnswerQuiz(ScoringType.MULTI_PARTIAL);

        AttemptResultResponse result = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(25L, 101L))));

        assertThat(result.score()).isEqualByComparingTo("5");
        assertThat(result.maxScore()).isEqualByComparingTo("10");
        assertThat(result.passed()).isTrue();
    }

    @Test
    void submit_allOrNothingQuiz_givesZeroForTheSameHalfAnswer_butFullForAllCorrect() {
        multiAnswerQuiz(ScoringType.MULTI_ALL_OR_NOTHING);

        AttemptResultResponse half = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(25L, 101L))));
        AttemptResultResponse full = service.submit(QUIZ, STUDENT,
                new SubmitAttemptRequest(List.of(answer(25L, 101L, 102L))));

        assertThat(half.score()).isEqualByComparingTo("0");
        assertThat(half.passed()).isFalse();
        assertThat(full.score()).isEqualByComparingTo("10");
        assertThat(full.passed()).isTrue();
    }
}
