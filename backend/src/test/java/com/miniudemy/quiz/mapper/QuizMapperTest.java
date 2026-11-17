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
import com.miniudemy.quiz.dto.QuizSummaryResponse;
import com.miniudemy.quiz.dto.StudentOptionResponse;
import com.miniudemy.quiz.dto.StudentQuestionResponse;
import com.miniudemy.quiz.dto.StudentQuizResponse;

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

    @Test
    void toStudentResponse_copiesQuestionsAndOptionsWithoutCorrectFlag() {
        Quiz quiz = new Quiz(11L, "Bölüm 1 sınavı", ScoringType.SINGLE_ANSWER, 70, true);
        ReflectionTestUtils.setField(quiz, "id", 31L);
        QuestionMapper questionMapper = Mappers.getMapper(QuestionMapper.class);

        StudentQuizResponse response = mapper.toStudentResponse(quiz,
                questionMapper.toStudentResponses(List.of(question(22L, "Soru"))));

        assertThat(response.id()).isEqualTo(31L);
        assertThat(response.questionCount()).isEqualTo(1);
        assertThat(response.questions().get(0).options()).extracting("text").containsExactly("A", "B");
        // Öğrenci DTO'larında doğru cevabı taşıyabilecek hiçbir alan olmamalı.
        assertThat(recordFields(StudentOptionResponse.class)).containsExactly("id", "text");
        assertThat(recordFields(StudentQuestionResponse.class)).containsExactly("id", "text", "points", "options");
        assertThat(recordFields(StudentQuizResponse.class)).noneMatch(n -> n.toLowerCase().contains("correct"));
    }

    @Test
    void toSummary_countsQuestionsFromQuiz() {
        Quiz quiz = new Quiz(11L, "Bölüm 1 sınavı", ScoringType.MULTI_PARTIAL, 60, false);
        ReflectionTestUtils.setField(quiz, "id", 31L);
        quiz.replaceQuestions(List.of(21L, 22L, 23L));

        QuizSummaryResponse summary = mapper.toSummary(quiz);

        assertThat(summary.questionCount()).isEqualTo(3);
        assertThat(summary.scoringType()).isEqualTo(ScoringType.MULTI_PARTIAL);
        assertThat(summary.passThreshold()).isEqualTo(60);
        assertThat(summary.required()).isFalse();
    }

    private static List<String> recordFields(Class<? extends Record> type) {
        return java.util.Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)
                .toList();
    }

    private static Question question(long id, String text) {
        Question q = new Question(3L, text, 10);
        ReflectionTestUtils.setField(q, "id", id);
        q.replaceOptions(List.of(new Option("A", true), new Option("B", false)));
        return q;
    }
}
