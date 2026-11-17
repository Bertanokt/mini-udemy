package com.miniudemy.quiz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.quiz.domain.Option;
import com.miniudemy.quiz.domain.Question;
import com.miniudemy.quiz.domain.Quiz;
import com.miniudemy.quiz.domain.ScoringType;
import com.miniudemy.quiz.dto.InstructorQuizResponse;

class QuizMapperTest {

    private final QuizMapper mapper = Mappers.getMapper(QuizMapper.class);

    @Test
    void toInstructorResponse_copiesAllContractFieldsAndKeepsQuestionOrder() {
        Quiz quiz = new Quiz(11L, "Bölüm 1 sınavı", ScoringType.MULTI_ALL_OR_NOTHING, 70, true);
        ReflectionTestUtils.setField(quiz, "id", 31L);
        Question first = question(22L, "İkinci eklenen");
        Question second = question(21L, "Birinci eklenen");

        InstructorQuizResponse response = mapper.toInstructorResponse(quiz,
                Mappers.getMapper(QuestionMapper.class).toResponses(List.of(first, second)));

        assertThat(response.id()).isEqualTo(31L);
        assertThat(response.sectionId()).isEqualTo(11L);
        assertThat(response.title()).isEqualTo("Bölüm 1 sınavı");
        assertThat(response.scoringType()).isEqualTo(ScoringType.MULTI_ALL_OR_NOTHING);
        assertThat(response.passThreshold()).isEqualTo(70);
        assertThat(response.required()).isTrue();
        assertThat(response.questionCount()).isEqualTo(2);
        assertThat(response.questions()).extracting("id").containsExactly(22L, 21L);
        assertThat(response.questions().get(0).options()).extracting("correct").containsExactly(true, false);
    }

    private static Question question(long id, String text) {
        Question q = new Question(3L, text, 10);
        ReflectionTestUtils.setField(q, "id", id);
        q.replaceOptions(List.of(new Option("A", true), new Option("B", false)));
        return q;
    }
}
