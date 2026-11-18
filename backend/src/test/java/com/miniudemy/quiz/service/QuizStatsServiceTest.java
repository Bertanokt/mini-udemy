package com.miniudemy.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.dto.QuizAttemptStats;
import com.miniudemy.course.dto.QuizBestResult;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizAttemptRepository.BestResultRow;
import com.miniudemy.quiz.repository.QuizAttemptRepository.QuizCountRow;
import com.miniudemy.quiz.repository.QuizRepository;

class QuizStatsServiceTest {

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final QuizAttemptRepository attemptRepository = mock(QuizAttemptRepository.class);
    private final CourseStructureService structure = mock(CourseStructureService.class);
    private final QuizStatsService service = new QuizStatsService(quizRepository, attemptRepository, structure);

    @Test
    void attemptStats_countsAttemptsPerQuiz_andListsQuizzesWithoutAttemptsAsZero() {
        courseWithQuizzes(quiz(1L, "Giriş sınavı"), quiz(2L, "Final"));
        QuizCountRow first = mock(QuizCountRow.class);
        given(first.getQuizId()).willReturn(1L);
        given(first.getAttemptCount()).willReturn(4L);
        given(first.getPassedCount()).willReturn(3L);
        given(attemptRepository.countByQuizIds(anyCollection())).willReturn(List.of(first));

        assertThat(service.attemptStats(5L)).containsExactly(
                new QuizAttemptStats(1L, "Giriş sınavı", 4, 3),
                new QuizAttemptStats(2L, "Final", 0, 0));
        verify(quizRepository).findBySectionIdInOrderByIdAsc(List.of(10L, 11L));
        verify(attemptRepository).countByQuizIds(List.of(1L, 2L));
    }

    @Test
    void attemptStats_courseWithoutSections_isEmptyWithoutQueries() {
        given(structure.sectionLessons(5L)).willReturn(List.of());

        assertThat(service.attemptStats(5L)).isEmpty();
        verifyNoInteractions(quizRepository, attemptRepository);
    }

    @Test
    void attemptStats_courseWithoutQuizzes_isEmptyWithoutAttemptQuery() {
        courseWithQuizzes();

        assertThat(service.attemptStats(5L)).isEmpty();
        verifyNoInteractions(attemptRepository);
    }

    @Test
    void bestResults_groupsByStudent_roundsToOneDecimal_andMapsPassedFlag() {
        courseWithQuizzes(quiz(1L, "Giriş sınavı"), quiz(2L, "Final"));
        List<BestResultRow> rows = List.of(
                row(100L, 1L, "66.666666", 1), row(100L, 2L, "40.0", 0), row(101L, 1L, "100", 1));
        given(attemptRepository.bestResults(anyCollection(), anyCollection())).willReturn(rows);

        Map<Long, List<QuizBestResult>> results = service.bestResults(5L, List.of(100L, 101L, 102L));

        assertThat(results.get(100L)).containsExactly(new QuizBestResult(1L, 66.7, true),
                new QuizBestResult(2L, 40.0, false));
        assertThat(results.get(101L)).containsExactly(new QuizBestResult(1L, 100.0, true));
        // Denemesi olmayan kursiyer haritada yok.
        assertThat(results).doesNotContainKey(102L);
        verify(attemptRepository).bestResults(List.of(1L, 2L), List.of(100L, 101L, 102L));
    }

    @Test
    void bestResults_noStudents_skipsEverything() {
        assertThat(service.bestResults(5L, List.of())).isEmpty();
        verifyNoInteractions(structure, quizRepository, attemptRepository);
    }

    @Test
    void bestResults_courseWithoutQuizzes_isEmptyWithoutAttemptQuery() {
        courseWithQuizzes();

        assertThat(service.bestResults(5L, List.of(100L))).isEmpty();
        verifyNoInteractions(attemptRepository);
    }

    private void courseWithQuizzes(Quiz... quizzes) {
        given(structure.sectionLessons(5L)).willReturn(
                List.of(new SectionLessons(10L, List.of(1L)), new SectionLessons(11L, List.of())));
        given(quizRepository.findBySectionIdInOrderByIdAsc(anyCollection())).willReturn(List.of(quizzes));
    }

    private static Quiz quiz(long id, String title) {
        Quiz quiz = new Quiz(10L, title, ScoringType.SINGLE_ANSWER, 60, true);
        ReflectionTestUtils.setField(quiz, "id", id);
        return quiz;
    }

    private static BestResultRow row(long studentId, long quizId, String percent, int passed) {
        BestResultRow row = mock(BestResultRow.class);
        given(row.getStudentId()).willReturn(studentId);
        given(row.getQuizId()).willReturn(quizId);
        given(row.getBestPercent()).willReturn(new BigDecimal(percent));
        given(row.getPassed()).willReturn(passed);
        return row;
    }
}
