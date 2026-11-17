package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.dto.QuizSummaryResponse;
import com.miniudemy.quiz.dto.StudentQuestionResponse;
import com.miniudemy.quiz.dto.StudentQuizResponse;

/** Sınav entity'si ile DTO'su arasındaki dönüşüm; soru yanıtları (QuestionMapper) sınavdaki sırayla verilir. */
@Mapper(config = MapStructConfig.class)
public interface QuizMapper {

    @Mapping(target = "id", source = "quiz.id")
    @Mapping(target = "sectionId", source = "quiz.sectionId")
    @Mapping(target = "title", source = "quiz.title")
    @Mapping(target = "scoringType", source = "quiz.scoringType")
    @Mapping(target = "passThreshold", source = "quiz.passThreshold")
    @Mapping(target = "required", source = "quiz.required")
    @Mapping(target = "questionCount", expression = "java(questions.size())")
    @Mapping(target = "questions", source = "questions")
    InstructorQuizResponse toInstructorResponse(Quiz quiz, List<QuestionResponse> questions);

    @Mapping(target = "questionCount", expression = "java(quiz.getQuestionIds().size())")
    QuizSummaryResponse toSummary(Quiz quiz);

    List<QuizSummaryResponse> toSummaries(List<Quiz> quizzes);

    @Mapping(target = "id", source = "quiz.id")
    @Mapping(target = "sectionId", source = "quiz.sectionId")
    @Mapping(target = "title", source = "quiz.title")
    @Mapping(target = "scoringType", source = "quiz.scoringType")
    @Mapping(target = "passThreshold", source = "quiz.passThreshold")
    @Mapping(target = "required", source = "quiz.required")
    @Mapping(target = "questionCount", expression = "java(questions.size())")
    @Mapping(target = "questions", source = "questions")
    StudentQuizResponse toStudentResponse(Quiz quiz, List<StudentQuestionResponse> questions);
}
