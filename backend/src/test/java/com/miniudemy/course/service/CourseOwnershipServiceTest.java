package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.SectionRepository;

class CourseOwnershipServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final SectionRepository sectionRepository = mock(SectionRepository.class);
    private final CourseOwnershipService service = new CourseOwnershipService(courseRepository, sectionRepository);

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

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"DRAFT", "UNPUBLISHED"})
    void assertOwnerAndEditable_inEditableStatus_passes(CourseStatus status) {
        given(courseRepository.findById(5L)).willReturn(Optional.of(CourseFixtures.course(5L, 7L, "Kurs", status)));

        assertThatCode(() -> service.assertOwnerAndEditable(5L, 7L)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void assertOwnerAndEditable_inLockedStatus_throwsConflict(CourseStatus status) {
        given(courseRepository.findById(5L)).willReturn(Optional.of(CourseFixtures.course(5L, 7L, "Kurs", status)));

        assertThatThrownBy(() -> service.assertOwnerAndEditable(5L, 7L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void assertOwnerAndEditable_forAnotherInstructor_throwsForbiddenEvenIfLocked() {
        given(courseRepository.findById(5L))
                .willReturn(Optional.of(CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.assertOwnerAndEditable(5L, 8L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertOwnerAndEditable_whenCourseMissing_throwsNotFound() {
        given(courseRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertOwnerAndEditable(5L, 7L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findCourseIdOfSection_returnsCourseId() {
        given(sectionRepository.findCourseIdById(9L)).willReturn(Optional.of(5L));

        assertThat(service.findCourseIdOfSection(9L)).isEqualTo(5L);
    }

    @Test
    void findCourseIdOfSection_whenMissing_throwsNotFound() {
        given(sectionRepository.findCourseIdById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findCourseIdOfSection(9L)).isInstanceOf(NotFoundException.class);
    }
}
