package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuizSummaryResponse;
import com.miniudemy.quiz.dto.StudentQuizResponse;
import com.miniudemy.quiz.mapper.QuestionMapper;
import com.miniudemy.quiz.mapper.QuizMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Sınav okuma (#49): öğrenci görünümü, eğitmen görünümü ve bölüm listesi. */
class QuizViewServiceTest {

    private static final long COURSE = 3L;
    private static final long SECTION = 11L;
    private static final long OWNER = 7L;
    private static final long STUDENT = 42L;
    private static final long QUIZ = 31L;

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final EnrollmentService enrollment = mock(EnrollmentService.class);
    private final QuizService service = new QuizService(quizRepository, questionRepository,
            Mappers.getMapper(QuizMapper.class), Mappers.getMapper(QuestionMapper.class), ownership, enrollment);

    private Quiz quiz;

    @BeforeEach
    void stubs() {
        quiz = new Quiz(SECTION, "Bölüm 1 sınavı", ScoringType.SINGLE_ANSWER, 70, true);
        ReflectionTestUtils.setField(quiz, "id", QUIZ);
        quiz.replaceQuestions(List.of(22L, 21L));
        given(quizRepository.findById(QUIZ)).willReturn(Optional.of(quiz));
        given(ownership.findCourseIdOfSection(SECTION)).willReturn(COURSE);
        // Veritabanı sırayı garanti etmez; service sınavdaki sıraya dizmeli.
        given(questionRepository.findAllWithOptionsByIdIn(List.of(22L, 21L)))
                .willReturn(List.of(question(21L), question(22L)));
    }

    private static Question question(long id) {
        Question q = new Question(COURSE, "Soru " + id, 10);
        ReflectionTestUtils.setField(q, "id", id);
        q.replaceOptions(List.of(new Option("A", true), new Option("B", false)));
        return q;
    }

    private static void assertStatus(Throwable ex, HttpStatus status) {
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getStatus()).isEqualTo(status);
    }

    // ---- getQuizForStudent

    @Test
    void getQuizForStudent_returnsQuestionsInQuizOrder() {
        StudentQuizResponse response = service.getQuizForStudent(QUIZ, STUDENT);

        assertThat(response.id()).isEqualTo(QUIZ);
        assertThat(response.questionCount()).isEqualTo(2);
        assertThat(response.questions()).extracting("id").containsExactly(22L, 21L);
        assertThat(response.questions().get(0).options()).extracting("text").containsExactly("A", "B");
        verify(enrollment).assertEnrolled(STUDENT, COURSE);
    }

    @Test
    void getQuizForStudent_notEnrolled_returns403() {
        willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz.")).given(enrollment)
                .assertEnrolled(STUDENT, COURSE);

        assertThatThrownBy(() -> service.getQuizForStudent(QUIZ, STUDENT))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
        verify(questionRepository, never()).findAllWithOptionsByIdIn(any());
    }

    @Test
    void getQuizForStudent_unknownQuiz_returns404() {
        assertThatThrownBy(() -> service.getQuizForStudent(999L, STUDENT))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    @Test
    void getQuizForStudent_loadsQuestionsWithOneQuery() {
        service.getQuizForStudent(QUIZ, STUDENT);

        verify(questionRepository, times(1)).findAllWithOptionsByIdIn(List.of(22L, 21L));
    }

    // ---- getQuizForInstructor

    @Test
    void getQuizForInstructor_returnsCorrectAnswers() {
        InstructorQuizResponse response = service.getQuizForInstructor(QUIZ, OWNER);

        assertThat(response.questions()).extracting("id").containsExactly(22L, 21L);
        assertThat(response.questions().get(0).options()).extracting("correct").containsExactly(true, false);
        verify(ownership).assertOwner(COURSE, OWNER);
    }

    @Test
    void getQuizForInstructor_foreignCourse_returns403() {
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.getQuizForInstructor(QUIZ, 8L))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
    }

    @Test
    void getQuizForInstructor_unknownQuiz_returns404() {
        assertThatThrownBy(() -> service.getQuizForInstructor(999L, OWNER))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }

    // ---- listSectionQuizzes

    @Test
    void listSectionQuizzes_asEnrolledStudent_returnsSummaries() {
        given(quizRepository.findBySectionIdOrderByIdAsc(SECTION)).willReturn(List.of(quiz));

        List<QuizSummaryResponse> list = service.listSectionQuizzes(SECTION, STUDENT, "STUDENT");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(QUIZ);
        assertThat(list.get(0).questionCount()).isEqualTo(2);
        verify(enrollment).assertEnrolled(STUDENT, COURSE);
    }

    @Test
    void listSectionQuizzes_asNotEnrolledStudent_returns403() {
        willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz.")).given(enrollment)
                .assertEnrolled(STUDENT, COURSE);

        assertThatThrownBy(() -> service.listSectionQuizzes(SECTION, STUDENT, "STUDENT"))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
    }

    @Test
    void listSectionQuizzes_asOwner_checksOwnership() {
        given(quizRepository.findBySectionIdOrderByIdAsc(SECTION)).willReturn(List.of(quiz));

        assertThat(service.listSectionQuizzes(SECTION, OWNER, "INSTRUCTOR")).hasSize(1);
        verify(ownership).assertOwner(COURSE, OWNER);
    }

    @Test
    void listSectionQuizzes_asForeignInstructor_returns403() {
        willThrow(new ForbiddenException("Bu kurs size ait değil.")).given(ownership).assertOwner(COURSE, 8L);

        assertThatThrownBy(() -> service.listSectionQuizzes(SECTION, 8L, "INSTRUCTOR"))
                .satisfies(ex -> assertStatus(ex, HttpStatus.FORBIDDEN));
    }

    @Test
    void listSectionQuizzes_asAdmin_skipsOwnershipAndEnrollment() {
        given(quizRepository.findBySectionIdOrderByIdAsc(SECTION)).willReturn(List.of(quiz));

        assertThat(service.listSectionQuizzes(SECTION, 1L, "ADMIN")).hasSize(1);
        verify(ownership, never()).assertOwner(anyLong(), anyLong());
        verify(enrollment, never()).assertEnrolled(anyLong(), anyLong());
    }

    @Test
    void listSectionQuizzes_unknownSection_returns404() {
        given(ownership.findCourseIdOfSection(404L)).willThrow(new NotFoundException("Bölüm bulunamadı."));

        assertThatThrownBy(() -> service.listSectionQuizzes(404L, STUDENT, "STUDENT"))
                .satisfies(ex -> assertStatus(ex, HttpStatus.NOT_FOUND));
    }
}
