package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.QuizAttempt;
import com.miniudemy.quiz.dto.AttemptSummaryResponse;

/** Deneme entity'sinden özet DTO'su; yüzde entity'de hesaplanır. */
@Mapper(config = MapStructConfig.class)
public interface QuizAttemptMapper {

    AttemptSummaryResponse toSummary(QuizAttempt attempt);

    List<AttemptSummaryResponse> toSummaries(List<QuizAttempt> attempts);
}
