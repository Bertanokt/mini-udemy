package com.miniudemy.quiz.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.mapper.QuizAttemptMapper;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Öğrencinin sınav deneme geçmişi (#55). */
@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptMapper attemptMapper;
    private final CourseOwnershipService courseOwnershipService;
    private final EnrollmentService enrollmentService;

    public QuizAttemptService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
            QuizAttemptMapper attemptMapper, CourseOwnershipService courseOwnershipService,
            EnrollmentService enrollmentService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.attemptMapper = attemptMapper;
        this.courseOwnershipService = courseOwnershipService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Yalnız öğrencinin kendi denemeleri, en yeni önce. Sınav yoksa 404, kursa kayıtlı değilse 403 (sınav görüntüleme
     * ile aynı kural).
     */
    @Transactional(readOnly = true)
    public List<AttemptSummaryResponse> listMyAttempts(Long quizId, Long studentId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new NotFoundException("Sınav bulunamadı."));
        enrollmentService.assertEnrolled(studentId, courseOwnershipService.findCourseIdOfSection(quiz.getSectionId()));
        return attemptMapper.toSummaries(
                attemptRepository.findByQuizIdAndStudentIdOrderBySubmittedAtDescIdDesc(quizId, studentId));
    }
}
