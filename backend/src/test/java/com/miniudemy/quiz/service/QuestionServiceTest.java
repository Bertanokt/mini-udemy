package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.dto.OptionRequest;
import com.miniudemy.quiz.dto.QuestionRequest;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.mapper.QuestionMapper;
import com.miniudemy.quiz.repository.QuestionRepository;

class QuestionServiceTest {

    private static final long COURSE = 3L;
    private static final long OWNER = 7L;
    private static final long QUESTION = 21L;

    private final QuestionRepository repository = mock(QuestionRepository.class);
    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final QuestionService service =
            new QuestionService(repository, Mappers.getMapper(QuestionMapper.class), ownership);

    @BeforeEach
    void saveReturnsArgument() {
        given(repository.save(any(Question.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private static QuestionRequest request(OptionRequest... options) {
        return new QuestionRequest("HTTP'nin varsayılan portu hangisidir?", 10, List.of(options));
    }

    private static QuestionRequest validRequest() {
        return request(new OptionRequest("80", true), new OptionRequest("22", false));
    }

    private Question existing() {
        Question q = new Question(COURSE, "Eski soru metni", 5);
        ReflectionTestUtils.setField(q, "id", QUESTION);
        q.replaceOptions(List.of(new Option("80", true), new Option("22", false)));
        given(repository.findById(QUESTION)).willReturn(Optional.of(q));
        return q;
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    // --- listQuestions

    @Test
    void listQuestions_returnsQuestionsWithCorrectFlags() {
        Question q = existing();
        given(repository.findByCourseIdOrderByIdAsc(COURSE)).willReturn(List.of(q));

        List<QuestionResponse> result = service.listQuestions(COURSE, OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).options()).extracting("correct").containsExactly(true, false);
    }

    @Test
    void listQuestions_forAnotherInstructor_returns403() {
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.listQuestions(COURSE, 8L))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(repository, never()).findByCourseIdOrderByIdAsc(any());
    }

    @Test
    void listQuestions_whenCourseMissing_returns404() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(ownership).assertOwner(COURSE, OWNER);

        assertThatThrownBy(() -> service.listQuestions(COURSE, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // --- createQuestion

    @Test
    void createQuestion_savesQuestionWithOptionsForTheCourse() {
        QuestionResponse response = service.createQuestion(COURSE, OWNER, validRequest());

        assertThat(response.courseId()).isEqualTo(COURSE);
        assertThat(response.points()).isEqualTo(10);
        assertThat(response.options()).extracting("text", "correct")
                .containsExactly(tuple("80", true), tuple("22", false));
        verify(ownership).assertOwner(COURSE, OWNER);
    }

    @Test
    void createQuestion_withoutAnyCorrectOption_returns400WithOptionsError() {
        QuestionRequest noCorrect = request(new OptionRequest("80", false), new OptionRequest("22", false));

        assertThatThrownBy(() -> service.createQuestion(COURSE, OWNER, noCorrect)).satisfies(ex -> {
            assertStatus(ex, HttpStatus.BAD_REQUEST);
            assertThat(((ApiException) ex).getErrors()).containsKey("options");
        });
        verify(repository, never()).save(any());
    }

    @Test
    void createQuestion_forAnotherInstructor_returns403() {
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.createQuestion(COURSE, 8L, validRequest()))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(repository, never()).save(any());
    }

    @Test
    void createQuestion_whenCourseMissing_returns404() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(ownership).assertOwner(COURSE, OWNER);

        assertThatThrownBy(() -> service.createQuestion(COURSE, OWNER, validRequest()))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // --- updateQuestion

    @Test
    void updateQuestion_replacesTextPointsAndOptions() {
        existing();
        QuestionRequest changed = new QuestionRequest("Yeni soru metni", 20,
                List.of(new OptionRequest("443", true), new OptionRequest("80", false), new OptionRequest("22", false)));

        QuestionResponse response = service.updateQuestion(QUESTION, OWNER, changed);

        assertThat(response.text()).isEqualTo("Yeni soru metni");
        assertThat(response.points()).isEqualTo(20);
        assertThat(response.options()).extracting("text").containsExactly("443", "80", "22");
    }

    @Test
    void updateQuestion_withoutCorrectOption_returns400() {
        existing();
        QuestionRequest noCorrect = request(new OptionRequest("80", false), new OptionRequest("22", false));

        assertThatThrownBy(() -> service.updateQuestion(QUESTION, OWNER, noCorrect))
                .satisfies(ex -> assertStatus(ex, HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateQuestion_whenAnsweredInAttemptsAndOptionsChange_returns409() {
        existing();
        given(repository.hasAttemptAnswers(QUESTION)).willReturn(true);
        QuestionRequest changed = request(new OptionRequest("443", true), new OptionRequest("22", false));

        assertThatThrownBy(() -> service.updateQuestion(QUESTION, OWNER, changed))
                .satisfies(ex -> assertStatus(ex, HttpStatus.CONFLICT));
        verify(repository, never()).save(any());
    }

    @Test
    void updateQuestion_whenAnsweredButOptionsSame_updatesTextAndPointsOnly() {
        Question q = existing();
        Option first = q.getOptions().get(0);
        given(repository.hasAttemptAnswers(QUESTION)).willReturn(true);

        QuestionResponse response = service.updateQuestion(QUESTION, OWNER,
                new QuestionRequest("Düzeltilmiş soru metni", 8,
                        List.of(new OptionRequest("80", true), new OptionRequest("22", false))));

        assertThat(response.text()).isEqualTo("Düzeltilmiş soru metni");
        assertThat(q.getOptions().get(0)).isSameAs(first);
    }

    @Test
    void updateQuestion_whenQuestionMissing_returns404() {
        given(repository.findById(QUESTION)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateQuestion(QUESTION, OWNER, validRequest()))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    @Test
    void updateQuestion_forAnotherInstructor_returns403() {
        existing();
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.updateQuestion(QUESTION, 8L, validRequest()))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
    }

    // --- deleteQuestion

    @Test
    void deleteQuestion_deletesUnusedQuestion() {
        Question q = existing();

        service.deleteQuestion(QUESTION, OWNER);

        verify(repository).delete(q);
    }

    @Test
    void deleteQuestion_whenUsedInQuiz_returns409() {
        existing();
        given(repository.isUsedInQuiz(QUESTION)).willReturn(true);

        assertThatThrownBy(() -> service.deleteQuestion(QUESTION, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.CONFLICT));
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteQuestion_whenAnsweredInAttempts_returns409() {
        existing();
        given(repository.hasAttemptAnswers(QUESTION)).willReturn(true);

        assertThatThrownBy(() -> service.deleteQuestion(QUESTION, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.CONFLICT));
    }

    @Test
    void deleteQuestion_whenQuestionMissing_returns404() {
        given(repository.findById(QUESTION)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteQuestion(QUESTION, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteQuestion_forAnotherInstructor_returns403() {
        existing();
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.deleteQuestion(QUESTION, 8L))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(repository, never()).delete(any());
    }
}
