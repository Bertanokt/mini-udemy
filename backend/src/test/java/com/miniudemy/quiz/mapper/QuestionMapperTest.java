package com.miniudemy.quiz.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.dto.OptionRequest;
import com.miniudemy.quiz.dto.OptionResponse;
import com.miniudemy.quiz.dto.QuestionResponse;

class QuestionMapperTest {

    private final QuestionMapper mapper = Mappers.getMapper(QuestionMapper.class);

    @Test
    void toResponse_copiesAllContractFieldsIncludingCorrectFlag() {
        Question question = new Question(3L, "HTTP'nin varsayılan portu hangisidir?", 10);
        ReflectionTestUtils.setField(question, "id", 21L);
        Option a = new Option("80", true);
        Option b = new Option("22", false);
        ReflectionTestUtils.setField(a, "id", 84L);
        ReflectionTestUtils.setField(b, "id", 85L);
        question.replaceOptions(List.of(a, b));

        QuestionResponse response = mapper.toResponse(question);

        assertThat(response).isEqualTo(new QuestionResponse(21L, 3L, "HTTP'nin varsayılan portu hangisidir?", 10,
                List.of(new OptionResponse(84L, "80", true), new OptionResponse(85L, "22", false))));
    }

    @Test
    void toEntities_buildsOptionsWithTextAndCorrectFlag() {
        List<Option> options =
                mapper.toEntities(List.of(new OptionRequest("80", true), new OptionRequest("22", false)));

        assertThat(options).extracting(Option::getText, Option::isCorrect)
                .containsExactly(tuple("80", true), tuple("22", false));
    }
}
