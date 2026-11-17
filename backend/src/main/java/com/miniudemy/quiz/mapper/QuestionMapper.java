package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.dto.OptionRequest;
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionResponse;

/** Soru ve şık entity'leri ile DTO'ları arasındaki dönüşüm. */
@Mapper(config = MapStructConfig.class)
public interface QuestionMapper {

    QuestionResponse toResponse(Question question);

    List<QuestionResponse> toResponses(List<Question> questions);

    OptionResponse toResponse(Option option);

    default Option toEntity(OptionRequest request) {
        return new Option(request.text(), Boolean.TRUE.equals(request.correct()));
    }

    List<Option> toEntities(List<OptionRequest> requests);
}
