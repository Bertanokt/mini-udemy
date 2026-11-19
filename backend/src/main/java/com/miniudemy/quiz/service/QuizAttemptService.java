package com.miniudemy.quiz.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.quiz.domain.AttemptAnswer;
import com.miniudemy.quiz.domain.GradedQuestion;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.domain.QuizCompleted;
import com.miniudemy.quiz.dto.AnswerRequest;
import com.miniudemy.quiz.dto.AttemptResultResponse;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.dto.SubmitAttemptRequest;
import com.miniudemy.quiz.mapper.QuizAttemptMapper;
import com.miniudemy.quiz.repository.QuestionRepository;
import com.miniudemy.quiz.repository.QuizAttemptRepository;
import com.miniudemy.quiz.repository.QuizRepository;

/** Sınav çözme (#54) ve öğrencinin deneme geçmişi (#55). */
@Service
public class QuizAttemptService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final QuizAttemptMapper attemptMapper;
    private final CourseOwnershipService courseOwnershipService;
    private final EnrollmentService enrollmentService;
    private final ScoringStrategyResolver scoringResolver;
    private final ApplicationEventPublisher events;

    public QuizAttemptService(QuizRepository quizRepository, QuizAttemptRepository attemptRepository,
            QuestionRepository questionRepository, QuizAttemptMapper attemptMapper,
            CourseOwnershipService courseOwnershipService, EnrollmentService enrollmentService,
            ScoringStrategyResolver scoringResolver, ApplicationEventPublisher events) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.scoringResolver = scoringResolver;
        this.events = events;
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
        return List.of(); // KIRMIZI-Q2
    }

    /**
     * Kursiyerin cevaplarını puanlar, denemeyi ve cevapları kaydeder, {@link QuizCompleted} yayınlar. Sınav yoksa 404,
     * kursa kayıtlı değilse 403; sınava ait olmayan soru, soruya ait olmayan şık ya da aynı soru için tekrarlı cevap
     * 400. Puanlama sınavın tipine göre {@link ScoringStrategyResolver}'dan gelen stratejiyle yapılır. Geçme:
     * yüzde, sınavın eşiğine eşit ya da büyükse (yuvarlamadan hesaplanır).
     */
    @Transactional
    public AttemptResultResponse submit(Long quizId, Long studentId, SubmitAttemptRequest request) {
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new NotFoundException("Sınav bulunamadı."));
        Long courseId = courseOwnershipService.findCourseIdOfSection(quiz.getSectionId());
        // KIRMIZI-Q3 enrollmentService.assertEnrolled(studentId, courseId);

        List<Question> questions = loadQuestions(quiz);
        Map<Long, Set<Long>> selected = validateAnswers(questions, request.answers());

        ScoringStrategy strategy = scoringResolver.resolve(quiz.getScoringType());
        List<GradedQuestion> graded = new ArrayList<>();
        BigDecimal score = BigDecimal.ZERO;
        int maxScore = 0;
        for (Question question : questions) {
            Set<Long> correct = question.getOptions().stream().filter(Option::isCorrect).map(Option::getId)
                    .collect(Collectors.toSet());
            QuestionScore result = strategy.score(new ScoringInput(question.getPoints(), correct,
                    selected.getOrDefault(question.getId(), Set.of())));
            graded.add(new GradedQuestion(question.getId(), result.earned(), result.max()));
            score = score.add(result.earned());
            maxScore += result.max();
        }
        boolean passed = score.multiply(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.valueOf((long) quiz.getPassThreshold() * maxScore)) >= 0;

        List<AttemptAnswer> answers = selected.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(optionId -> new AttemptAnswer(e.getKey(), optionId)))
                .toList();
        QuizAttempt attempt = attemptRepository.save(
                new QuizAttempt(quizId, studentId, score, BigDecimal.valueOf(maxScore), passed, answers));
        events.publishEvent(new QuizCompleted(attempt.getId(), quizId, courseId, studentId, passed,
                attempt.getSubmittedAt()));
        return attemptMapper.toResult(attempt, graded);
    }

    /** Sınavın sorularını şıklarıyla, sınavdaki sırayla yükler. */
    private List<Question> loadQuestions(Quiz quiz) {
        List<Long> ids = quiz.getQuestionIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Question> byId = questionRepository.findAllWithOptionsByIdIn(ids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return ids.stream().map(byId::get).toList();
    }

    /** Cevapları soru id'sine göre toplar; sınavda olmayan soru, soruda olmayan şık, tekrarlı soru ya da şık 400. */
    private Map<Long, Set<Long>> validateAnswers(List<Question> questions, List<AnswerRequest> answers) {
        Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Set<Long>> selected = new LinkedHashMap<>();
        for (AnswerRequest answer : answers) {
            Question question = byId.get(answer.questionId());
            if (question == null) {
                throw BadRequestException.forField("answers", "Cevaplanan soru bu sınava ait değil.");
            }
            if (selected.containsKey(question.getId())) {
                throw BadRequestException.forField("answers", "Aynı soru için birden fazla cevap gönderilemez.");
            }
            Set<Long> validOptions = question.getOptions().stream().map(Option::getId).collect(Collectors.toSet());
            Set<Long> chosen = new LinkedHashSet<>(answer.optionIds());
            if (chosen.size() != answer.optionIds().size()) {
                throw BadRequestException.forField("answers", "Aynı şık bir soruda birden fazla kez seçilemez.");
            }
            if (!validOptions.containsAll(chosen)) {
                throw BadRequestException.forField("answers", "Seçilen şık sorunun şıklarından biri değil.");
            }
            selected.put(question.getId(), chosen);
        }
        return selected;
    }
}
