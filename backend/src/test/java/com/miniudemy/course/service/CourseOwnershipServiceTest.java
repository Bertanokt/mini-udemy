package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.repository.CourseRepository;

class CourseOwnershipServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseOwnershipService service = new CourseOwnershipService(courseRepository);

    private Course courseOf(long instructorId) {
        Course course = mock(Course.class);
        given(course.getInstructorId()).willReturn(instructorId);
        return course;
    }

    @Test
    void assertOwner_forTheInstructorOfTheCourse_passes() {
        Course course = courseOf(7L);
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));

        assertThatCode(() -> service.assertOwner(5L, 7L)).doesNotThrowAnyException();
    }

    @Test
    void assertOwner_forAnotherInstructor_throwsForbidden() {
        Course course = courseOf(7L);
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> service.assertOwner(5L, 8L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertOwner_whenCourseMissing_throwsNotFound() {
        given(courseRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertOwner(5L, 7L)).isInstanceOf(NotFoundException.class);
    }
}
