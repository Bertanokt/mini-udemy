package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Zorunlu sınav koşulu (#129): kursun bölümlerindeki her zorunlu sınavın geçen bir denemesi olmalı. */
class QuizRequirementServiceTest {

    private static final long STUDENT = 42L;
    private static final long COURSE = 3L;

    private final QuizRepository quizzes = mock(QuizRepository.class);
    private final QuizAttemptRepository attempts = mock(QuizAttemptRepository.class);
    private final CourseStructureService structure = mock(CourseStructureService.class);
    private final QuizRequirementService service = new QuizRequirementService(quizzes, attempts, structure);

    @BeforeEach
    void setUp() {
        given(structure.sectionLessons(COURSE)).willReturn(List.of(new SectionLessons(1L, List.of(11L)),
                new SectionLessons(2L, List.of(21L))));
    }

    private static Quiz quiz(long id, long sectionId, boolean required) {
        Quiz quiz = new Quiz(sectionId, "Sınav " + id, ScoringType.SINGLE_ANSWER, 60, required);
        ReflectionTestUtils.setField(quiz, "id", id);
        return quiz;
    }

    @Test
    void everyRequiredQuizPassed_isTrue_andOptionalQuizzesAreNotAsked() {
        given(quizzes.findBySectionIdInOrderByIdAsc(Set.of(1L, 2L)))
                .willReturn(List.of(quiz(81L, 1L, true), quiz(82L, 2L, true), quiz(83L, 2L, false)));
        given(attempts.countPassedQuizzes(STUDENT, Set.of(81L, 82L))).willReturn(2L);

        assertThat(service.allRequiredQuizzesPassed(STUDENT, COURSE)).isTrue();
    }

    @Test
    void oneRequiredQuizNotPassed_isFalse() {
        given(quizzes.findBySectionIdInOrderByIdAsc(Set.of(1L, 2L)))
                .willReturn(List.of(quiz(81L, 1L, true), quiz(82L, 2L, true)));
        given(attempts.countPassedQuizzes(STUDENT, Set.of(81L, 82L))).willReturn(1L);

        assertThat(service.allRequiredQuizzesPassed(STUDENT, COURSE)).isFalse();
    }

    @Test
    void noRequiredQuiz_isTrue_withoutQueryingAttempts() {
        given(quizzes.findBySectionIdInOrderByIdAsc(Set.of(1L, 2L))).willReturn(List.of(quiz(83L, 2L, false)));

        assertThat(service.allRequiredQuizzesPassed(STUDENT, COURSE)).isTrue();
        verify(attempts, never()).countPassedQuizzes(anyLong(), anyCollection());
    }

    @Test
    void courseWithoutSections_isTrue() {
        given(structure.sectionLessons(COURSE)).willReturn(List.of());

        assertThat(service.allRequiredQuizzesPassed(STUDENT, COURSE)).isTrue();
    }
}
