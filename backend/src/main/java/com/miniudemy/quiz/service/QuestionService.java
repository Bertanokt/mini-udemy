package com.miniudemy.quiz.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.service.CourseOwnershipService;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.dto.OptionRequest;
import com.miniudemy.quiz.dto.QuestionRequest;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.mapper.QuestionMapper;
import com.miniudemy.quiz.repository.QuestionRepository;

/** Kursun soru bankası (#121). Tüm işlemler yalnız kursun sahibi eğitmen içindir. */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final CourseOwnershipService courseOwnershipService;

    public QuestionService(QuestionRepository questionRepository, QuestionMapper questionMapper,
            CourseOwnershipService courseOwnershipService) {
        this.questionRepository = questionRepository;
        this.questionMapper = questionMapper;
        this.courseOwnershipService = courseOwnershipService;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(Long courseId, Long instructorId) {
        courseOwnershipService.assertOwner(courseId, instructorId);
        return questionMapper.toResponses(questionRepository.findByCourseIdOrderByIdAsc(courseId));
    }

    @Transactional
    public QuestionResponse createQuestion(Long courseId, Long instructorId, QuestionRequest request) {
        courseOwnershipService.assertOwner(courseId, instructorId);
        requireOneCorrectOption(request);
        Question question = new Question(courseId, request.text(), request.points());
        question.replaceOptions(questionMapper.toEntities(request.options()));
        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, Long instructorId, QuestionRequest request) {
        Question question = findOwned(questionId, instructorId);
        requireOneCorrectOption(request);
        boolean optionsChanged = !sameOptions(question.getOptions(), request.options());
        // Cevaplanmış sorunun şıkları değişirse sonuç geçmişi bozulur; aynı şıklarla yalnız metin/puan değişebilir.
        if (optionsChanged && questionRepository.hasAttemptAnswers(questionId)) {
            throw new ConflictException("Denemelerde cevaplanmış bir sorunun şıkları değiştirilemez.");
        }
        question.update(request.text(), request.points());
        if (optionsChanged) {
            question.replaceOptions(questionMapper.toEntities(request.options()));
        }
        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long instructorId) {
        Question question = findOwned(questionId, instructorId);
        // quiz_questions FK'si DEFERRABLE: ihlal commit'te patlar (500). Bu yüzden silmeden önce biz soruyoruz.
        if (questionRepository.isUsedInQuiz(questionId)) {
            throw new ConflictException("Bu soru bir sınavda kullanılıyor, silinemez.");
        }
        if (questionRepository.hasAttemptAnswers(questionId)) {
            throw new ConflictException("Bu soru sınav denemelerinde cevaplanmış, silinemez.");
        }
        questionRepository.delete(question);
    }

    private Question findOwned(Long questionId, Long instructorId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Soru bulunamadı."));
        courseOwnershipService.assertOwner(question.getCourseId(), instructorId);
        return question;
    }

    private void requireOneCorrectOption(QuestionRequest request) {
        if (request.options().stream().noneMatch(o -> Boolean.TRUE.equals(o.correct()))) {
            throw BadRequestException.forField("options", "En az bir şık doğru olarak işaretlenmelidir.");
        }
    }

    private boolean sameOptions(List<Option> current, List<OptionRequest> requested) {
        if (current.size() != requested.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (!current.get(i).getText().equals(requested.get(i).text())
                    || current.get(i).isCorrect() != Boolean.TRUE.equals(requested.get(i).correct())) {
                return false;
            }
        }
        return true;
    }
}
