package com.miniudemy.quiz.service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.QuizAttemptStats;
import com.miniudemy.course.dto.QuizBestResult;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseQuizStats;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizAttemptRepository.QuizCountRow;
import com.miniudemy.quiz.repository.QuizRepository;

/**
 * Eğitmen istatistikleri için sınav verisi (#133, #134): {@code course}'un tanımladığı {@link CourseQuizStats}
 * arayüzünün uygulaması. Sorgu sayısı sınav, kursiyer ve deneme sayısından bağımsızdır.
 */
@Service
@Transactional(readOnly = true)
public class QuizStatsService implements CourseQuizStats {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CourseStructureService structure;

    public QuizStatsService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
            CourseStructureService structure) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.structure = structure;
    }

    @Override
    public List<QuizAttemptStats> attemptStats(Long courseId) {
        List<Quiz> quizzes = courseQuizzes(courseId);
        if (quizzes.isEmpty()) {
            return List.of();
        }
        Map<Long, QuizCountRow> counts = attemptRepository
                .countByQuizIds(quizzes.stream().map(Quiz::getId).toList()).stream()
                .collect(Collectors.toMap(QuizCountRow::getQuizId, Function.identity()));
        return quizzes.stream().map(q -> {
            QuizCountRow row = counts.get(q.getId());
            return new QuizAttemptStats(q.getId(), q.getTitle(), row == null ? 0 : row.getAttemptCount(),
                    row == null ? 0 : row.getPassedCount());
        }).toList();
    }

    @Override
    public Map<Long, List<QuizBestResult>> bestResults(Long courseId, Collection<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        List<Quiz> quizzes = courseQuizzes(courseId);
        if (quizzes.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<QuizBestResult>> results = new LinkedHashMap<>();
        attemptRepository.bestResults(quizzes.stream().map(Quiz::getId).toList(), studentIds).forEach(row ->
                results.computeIfAbsent(row.getStudentId(), id -> new ArrayList<>())
                        .add(new QuizBestResult(row.getQuizId(),
                                row.getBestPercent().setScale(1, RoundingMode.HALF_UP).doubleValue(),
                                row.getPassed() != null && row.getPassed() > 0)));
        return results;
    }

    private List<Quiz> courseQuizzes(Long courseId) {
        List<Long> sectionIds = structure.sectionLessons(courseId).stream().map(SectionLessons::sectionId).toList();
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        return quizRepository.findBySectionIdInOrderByIdAsc(sectionIds);
    }
}
