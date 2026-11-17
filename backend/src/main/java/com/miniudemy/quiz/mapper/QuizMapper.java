package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.dto.InstructorQuizResponse;
import com.miniudemy.quiz.dto.QuestionResponse;

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
}
