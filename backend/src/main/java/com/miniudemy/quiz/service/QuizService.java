package com.miniudemy.quiz.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuizRequest;
import com.miniudemy.quiz.mapper.QuestionMapper;
import com.miniudemy.quiz.mapper.QuizMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/**
 * Bölüm sonu sınavının yazma işlemleri (#48). Yalnız kursun sahibi eğitmen içindir ve kurs düzenlenebilir
 * durumdayken (course modülü karar verir; #85'te State nesnesi) yapılır.
 */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizMapper quizMapper;
    private final QuestionMapper questionMapper;
    private final CourseOwnershipService courseOwnershipService;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository, QuizMapper quizMapper,
            QuestionMapper questionMapper, CourseOwnershipService courseOwnershipService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizMapper = quizMapper;
        this.questionMapper = questionMapper;
        this.courseOwnershipService = courseOwnershipService;
    }

    @Transactional
    public InstructorQuizResponse createQuiz(Long sectionId, Long instructorId, QuizRequest request) {
        Long courseId = courseOwnershipService.findCourseIdOfSection(sectionId);
        courseOwnershipService.assertOwnerAndEditable(courseId, instructorId);
        List<Question> questions = resolveQuestions(courseId, request.questionIds());
        Quiz quiz = new Quiz(sectionId, request.title(), request.scoringType(), request.passThreshold(),
                request.required());
        quiz.replaceQuestions(request.questionIds());
        return quizMapper.toInstructorResponse(quizRepository.save(quiz),
                questionMapper.toResponses(questions));
    }

    @Transactional
    public InstructorQuizResponse updateQuiz(Long quizId, Long instructorId, QuizRequest request) {
        Quiz quiz = findOwnedEditable(quizId, instructorId);
        Long courseId = courseOwnershipService.findCourseIdOfSection(quiz.getSectionId());
        List<Question> questions = resolveQuestions(courseId, request.questionIds());
        if (quizRepository.hasAttempts(quizId)) {
            // Denemesi olan sınavın puanlama tipi ve soru kümesi değişirse eski sonuçlar anlamsızlaşır.
            boolean sameQuestions = new HashSet<>(quiz.getQuestionIds()).equals(new HashSet<>(request.questionIds()));
            if (quiz.getScoringType() != request.scoringType() || !sameQuestions) {
                throw new ConflictException("Denemesi olan sınavın soruları ve puanlama tipi değiştirilemez; "
                        + "yalnız başlık, eşik ve zorunluluk değişebilir.");
            }
        }
        quiz.updateSettings(request.title(), request.passThreshold(), request.required());
        quiz.changeScoringType(request.scoringType());
        quiz.replaceQuestions(request.questionIds());
        return quizMapper.toInstructorResponse(quizRepository.save(quiz),
                questionMapper.toResponses(questions));
    }

    @Transactional
    public void deleteQuiz(Long quizId, Long instructorId) {
        Quiz quiz = findOwnedEditable(quizId, instructorId);
        if (quizRepository.hasAttempts(quizId)) {
            throw new ConflictException("Bu sınavın denemeleri var, silinemez.");
        }
        quizRepository.delete(quiz);
    }

    /** Sınav yoksa 404; bölümün kursu için sahiplik (403) ve düzenlenebilirlik (409) denetlenir. */
    private Quiz findOwnedEditable(Long quizId, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new NotFoundException("Sınav bulunamadı."));
        Long courseId = courseOwnershipService.findCourseIdOfSection(quiz.getSectionId());
        courseOwnershipService.assertOwnerAndEditable(courseId, instructorId);
        return quiz;
    }

    /** Soruları verilen sırayla döndürür; boş, tekrarlı ya da başka kursa ait soru 400. */
    private List<Question> resolveQuestions(Long courseId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw BadRequestException.forField("questionIds", "Sınavda en az bir soru olmalıdır.");
        }
        Set<Long> distinct = new HashSet<>(questionIds);
        if (distinct.size() != questionIds.size()) {
            throw BadRequestException.forField("questionIds", "Aynı soru sınava birden fazla kez eklenemez.");
        }
        Map<Long, Question> byId = questionRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        for (Long id : questionIds) {
            Question question = byId.get(id);
            if (question == null || !courseId.equals(question.getCourseId())) {
                throw BadRequestException.forField("questionIds",
                        "Yalnız bu kursun soru bankasındaki sorular seçilebilir.");
            }
        }
        return questionIds.stream().map(byId::get).toList();
    }
}
