package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.miniudemy.common.exception.ApiException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.dto.LessonContentRef;
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

    // --- içerik bilgisi (#113) ---

    @Test
    void contentRefOf_returnsCourseInstructorTypeAndFile() {
        Course course = CourseFixtures.course(3L, 7L, "Kurs", CourseStatus.PUBLISHED);
        Lesson lesson = CourseFixtures.lesson(CourseFixtures.section(course, 1L, "Giriş", 0), 11L, "Kurulum", 300, 0, 900L);
        given(repository.findById(11L)).willReturn(Optional.of(lesson));

        assertThat(service.contentRefOf(11L))
                .isEqualTo(new LessonContentRef(11L, 3L, 7L, LessonContentType.VIDEO, 900L));
    }

    @Test
    void contentRefOf_unknownLesson_returns404() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.contentRefOf(99L)).isInstanceOf(NotFoundException.class)
                .hasMessage("Ders bulunamadı.");
    }
}
