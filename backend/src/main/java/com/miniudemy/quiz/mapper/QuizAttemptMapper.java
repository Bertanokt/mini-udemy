package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.GradedQuestion;
import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.dto.AttemptResultResponse;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;
import com.miniudemy.quiz.dto.QuestionResultResponse;

/** Deneme entity'sinden özet ve sonuç DTO'ları; yüzde entity'de hesaplanır. */
@Mapper(config = MapStructConfig.class)
public interface QuizAttemptMapper {

    AttemptSummaryResponse toSummary(QuizAttempt attempt);

    List<AttemptSummaryResponse> toSummaries(List<QuizAttempt> attempts);

    @Mapping(target = "questions", source = "graded")
    AttemptResultResponse toResult(QuizAttempt attempt, List<GradedQuestion> graded);

    @Mapping(target = "correct", expression = "java(graded.isCorrect())")
    QuestionResultResponse toQuestionResult(GradedQuestion graded);
}
