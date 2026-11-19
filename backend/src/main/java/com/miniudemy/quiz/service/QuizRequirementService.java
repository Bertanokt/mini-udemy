package com.miniudemy.quiz.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/**
 * Kursun zorunlu sınav koşulu (#129): kursun bölümlerindeki zorunlu ({@code required}) sınavların her biri kursiyerin
 * en az bir geçen denemesiyle geçilmiş mi. "Geçti" bilgisi denemenin kendisinde saklanır (eşiğe o anki puanla).
 */
@Service
@Transactional(readOnly = true)
public class QuizRequirementService {

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final CourseStructureService structure;

    public QuizRequirementService(QuizRepository quizzes, QuizAttemptRepository attempts,
            CourseStructureService structure) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.structure = structure;
    }

    /** Kursun bütün zorunlu sınavları kursiyer tarafından geçildiyse true; zorunlu sınav yoksa true. */
    public boolean allRequiredQuizzesPassed(Long studentId, Long courseId) {
        Set<Long> sectionIds = structure.sectionLessons(courseId).stream().map(SectionLessons::sectionId)
                .collect(Collectors.toSet());
        if (sectionIds.isEmpty()) {
            return true;
        }
        Set<Long> required = quizzes.findBySectionIdInOrderByIdAsc(sectionIds).stream().filter(Quiz::isRequired)
                .map(Quiz::getId).collect(Collectors.toSet());
        return required.isEmpty() || attempts.countPassedQuizzes(studentId, required) == required.size();
    }
}
