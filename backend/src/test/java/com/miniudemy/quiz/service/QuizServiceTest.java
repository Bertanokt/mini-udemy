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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuizRequest;
import com.miniudemy.quiz.mapper.QuestionMapper;
import com.miniudemy.quiz.mapper.QuizMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizRepository;

class QuizServiceTest {

    private static final long COURSE = 3L;
    private static final long SECTION = 11L;
    private static final long OWNER = 7L;
    private static final long QUIZ = 31L;

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final QuizService service = new QuizService(quizRepository, questionRepository,
            Mappers.getMapper(QuizMapper.class), Mappers.getMapper(QuestionMapper.class), ownership,
            mock(com.miniudemy.enrollment.service.EnrollmentService.class));

    private final Question q1 = question(21L, COURSE);
    private final Question q2 = question(22L, COURSE);
    private final Question foreign = question(99L, 4L);

    @BeforeEach
    void stubs() {
        given(quizRepository.save(any(Quiz.class))).willAnswer(inv -> {
            Quiz quiz = inv.getArgument(0);
            if (quiz.getId() == null) {
                ReflectionTestUtils.setField(quiz, "id", QUIZ);
            }
            return quiz;
        });
        given(ownership.findCourseIdOfSection(SECTION)).willReturn(COURSE);
        given(questionRepository.findAllById(any())).willAnswer(inv -> {
            Iterable<Long> ids = inv.getArgument(0);
            return List.of(q1, q2, foreign).stream().filter(q -> {
                for (Long id : ids) {
                    if (id.equals(q.getId())) {
                        return true;
                    }
                }
                return false;
            }).toList();
        });
    }

    private static Question question(long id, long courseId) {
        Question q = new Question(courseId, "Soru " + id, 10);
        ReflectionTestUtils.setField(q, "id", id);
        q.replaceOptions(List.of(new Option("A", true), new Option("B", false)));
        return q;
    }

    private static QuizRequest request(ScoringType type, Long... questionIds) {
        return new QuizRequest("Bölüm 1 sınavı", type, 70, true, List.of(questionIds));
    }

    private Quiz existingQuiz() {
        Quiz quiz = new Quiz(SECTION, "Eski başlık", ScoringType.SINGLE_ANSWER, 50, false);
        ReflectionTestUtils.setField(quiz, "id", QUIZ);
        quiz.replaceQuestions(List.of(21L, 22L));
        given(quizRepository.findById(QUIZ)).willReturn(Optional.of(quiz));
        return quiz;
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    // ---- createQuiz

    @Test
    void createQuiz_storesQuestionsInGivenOrderAndReturnsInstructorView() {
        InstructorQuizResponse response = service.createQuiz(SECTION, OWNER,
                request(ScoringType.MULTI_PARTIAL, 22L, 21L));

        assertThat(response.id()).isEqualTo(QUIZ);
        assertThat(response.sectionId()).isEqualTo(SECTION);
        assertThat(response.scoringType()).isEqualTo(ScoringType.MULTI_PARTIAL);
        assertThat(response.passThreshold()).isEqualTo(70);
        assertThat(response.required()).isTrue();
        assertThat(response.questionCount()).isEqualTo(2);
        assertThat(response.questions()).extracting("id").containsExactly(22L, 21L);
        assertThat(response.questions().get(0).options()).extracting("correct").containsExactly(true, false);
        verify(ownership).assertOwnerAndEditable(COURSE, OWNER);
    }

    @Test
    void createQuiz_setsPositionsFromZero() {
        service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 22L, 21L));

        org.mockito.ArgumentCaptor<Quiz> captor = org.mockito.ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).save(captor.capture());
        assertThat(captor.getValue().getQuestionIds()).containsExactly(22L, 21L);
        assertThat((List<?>) ReflectionTestUtils.getField(captor.getValue(), "questions"))
                .extracting("position").containsExactly(0, 1);
    }

    @Test
    void createQuiz_withQuestionOfAnotherCourse_returns400() {
        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 21L, 99L)))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_withUnknownQuestion_returns400() {
        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 404L)))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void createQuiz_withDuplicateQuestion_returns400() {
        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 21L, 21L)))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_withEmptyQuestionList_returns400() {
        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER)))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void createQuiz_whenNotOwner_returns403() {
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(ForbiddenException.class);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_whenSectionMissing_returns404() {
        given(ownership.findCourseIdOfSection(SECTION)).willThrow(new NotFoundException("Bölüm bulunamadı."));

        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createQuiz_whenCourseNotEditable_returns409() {
        willThrow(new ConflictException("Kurs düzenlenemez.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.createQuiz(SECTION, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(ConflictException.class);
        verify(quizRepository, never()).save(any());
    }

    // ---- updateQuiz

    @Test
    void updateQuiz_replacesQuestionListSettingsAndScoringType() {
        Quiz quiz = existingQuiz();

        InstructorQuizResponse response = service.updateQuiz(QUIZ, OWNER, request(ScoringType.MULTI_PARTIAL, 22L));

        assertThat(quiz.getQuestionIds()).containsExactly(22L);
        assertThat(quiz.getTitle()).isEqualTo("Bölüm 1 sınavı");
        assertThat(quiz.getScoringType()).isEqualTo(ScoringType.MULTI_PARTIAL);
        assertThat(response.questionCount()).isEqualTo(1);
        verify(ownership).assertOwnerAndEditable(COURSE, OWNER);
    }

    @Test
    void updateQuiz_whenQuizMissing_returns404() {
        given(quizRepository.findById(QUIZ)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateQuiz_whenNotOwner_returns403() {
        existingQuiz();
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateQuiz_whenCourseNotEditable_returns409() {
        existingQuiz();
        willThrow(new ConflictException("Kurs düzenlenemez.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateQuiz_withQuestionOfAnotherCourse_returns400() {
        existingQuiz();

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.SINGLE_ANSWER, 99L)))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateQuiz_withAttempts_changingQuestionSet_returns409() {
        existingQuiz();
        given(quizRepository.hasAttempts(QUIZ)).willReturn(true);

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.SINGLE_ANSWER, 21L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateQuiz_withAttempts_changingScoringType_returns409() {
        existingQuiz();
        given(quizRepository.hasAttempts(QUIZ)).willReturn(true);

        assertThatThrownBy(() -> service.updateQuiz(QUIZ, OWNER, request(ScoringType.MULTI_PARTIAL, 21L, 22L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateQuiz_withAttempts_changingOnlyTitleThresholdRequired_succeeds() {
        Quiz quiz = existingQuiz();
        given(quizRepository.hasAttempts(QUIZ)).willReturn(true);

        service.updateQuiz(QUIZ, OWNER, new QuizRequest("Yeni başlık", ScoringType.SINGLE_ANSWER, 80, true,
                List.of(21L, 22L)));

        assertThat(quiz.getTitle()).isEqualTo("Yeni başlık");
        assertThat(quiz.getPassThreshold()).isEqualTo(80);
        assertThat(quiz.isRequired()).isTrue();
    }

    // ---- deleteQuiz

    @Test
    void deleteQuiz_deletesQuiz() {
        Quiz quiz = existingQuiz();

        service.deleteQuiz(QUIZ, OWNER);

        verify(quizRepository).delete(quiz);
        verify(ownership).assertOwnerAndEditable(COURSE, OWNER);
    }

    @Test
    void deleteQuiz_withAttempts_returns409AndDeletesNothing() {
        existingQuiz();
        given(quizRepository.hasAttempts(QUIZ)).willReturn(true);

        assertThatThrownBy(() -> service.deleteQuiz(QUIZ, OWNER)).isInstanceOf(ConflictException.class);
        verify(quizRepository, never()).delete(any());
    }

    @Test
    void deleteQuiz_whenQuizMissing_returns404() {
        given(quizRepository.findById(QUIZ)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteQuiz(QUIZ, OWNER)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteQuiz_whenNotOwner_returns403() {
        existingQuiz();
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.deleteQuiz(QUIZ, OWNER)).isInstanceOf(ForbiddenException.class);
        verify(quizRepository, never()).delete(any());
    }

    @Test
    void deleteQuiz_whenCourseNotEditable_returns409() {
        existingQuiz();
        willThrow(new ConflictException("Kurs düzenlenemez.")).given(ownership)
                .assertOwnerAndEditable(COURSE, OWNER);

        assertThatThrownBy(() -> service.deleteQuiz(QUIZ, OWNER)).isInstanceOf(ConflictException.class);
        verify(quizRepository, never()).delete(any());
    }
}
