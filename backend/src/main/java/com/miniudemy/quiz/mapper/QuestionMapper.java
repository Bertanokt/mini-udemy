package com.miniudemy.quiz.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.dto.OptionRequest;
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionResponse;
import com.miniudemy.quiz.dto.StudentOptionResponse;
import com.miniudemy.quiz.dto.StudentQuestionResponse;

/** Soru ve şık entity'leri ile DTO'ları arasındaki dönüşüm. */
@Mapper(config = MapStructConfig.class)
public interface QuestionMapper {

    QuestionResponse toResponse(Question question);

    List<QuestionResponse> toResponses(List<Question> questions);

    OptionResponse toResponse(Option option);

    /** Öğrenci görünümü: hedef tipte doğru cevap alanı olmadığı için o bilgi hiç kopyalanmaz. */
    StudentQuestionResponse toStudentResponse(Question question);

    List<StudentQuestionResponse> toStudentResponses(List<Question> questions);

    StudentOptionResponse toStudentResponse(Option option);

    default Option toEntity(OptionRequest request) {
        return new Option(request.text(), Boolean.TRUE.equals(request.correct()));
    }

    List<Option> toEntities(List<OptionRequest> requests);
}
