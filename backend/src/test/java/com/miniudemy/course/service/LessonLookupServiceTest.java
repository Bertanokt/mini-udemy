package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.repository.LessonRepository;

class LessonLookupServiceTest {

    private final LessonRepository repository = mock(LessonRepository.class);
    private final LessonLookupService service = new LessonLookupService(repository);

    @Test
    void courseIdOfLesson_returnsCourseOfTheLesson() {
        given(repository.findCourseIdByLessonId(11L)).willReturn(Optional.of(3L));

        assertThat(service.courseIdOfLesson(11L)).isEqualTo(3L);
    }

    @Test
    void courseIdOfLesson_unknownLesson_returns404() {
        given(repository.findCourseIdByLessonId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.courseIdOfLesson(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ders bulunamadı.")
                .extracting(ex -> ((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
