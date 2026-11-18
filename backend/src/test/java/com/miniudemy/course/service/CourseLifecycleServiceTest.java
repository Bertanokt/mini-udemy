package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStateException;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CourseRepository;

/** Onaya gönder (#27): sahiplik → durum (409) → içerik (400) → İncelemede; yanıt güncel kurs. */
class CourseLifecycleServiceTest {

    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final CourseLifecycleService service = new CourseLifecycleService(ownership, courseRepository,
            userDirectory, new CourseMapperImpl(new CategoryMapperImpl()));

    @BeforeEach
    void instructor() {
        given(userDirectory.findSummaries(List.of(7L))).willReturn(Map.of(7L, new UserSummary(7L, "Mehmet Kaya")));
        given(courseRepository.saveAndFlush(any(Course.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private static Course submittable(CourseStatus status) {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", status);
        CourseFixtures.lesson(CourseFixtures.section(course, 1L, "Giriş", 0), 11L, "Kurulum", 300, 0, 900L);
        return course;
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"DRAFT", "UNPUBLISHED"})
    void submit_ownCourseWithContent_movesToInReview_savesAndReturnsIt(CourseStatus from) {
        Course course = submittable(from);
        given(ownership.requireOwnedCourse(5L, 7L)).willReturn(course);

        CourseResponse response = service.submit(5L, 7L);

        assertThat(course.getStatus()).isEqualTo(CourseStatus.IN_REVIEW);
        verify(courseRepository).saveAndFlush(course);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.status()).isEqualTo(CourseStatus.IN_REVIEW);
        assertThat(response.instructor()).isEqualTo(new UserSummary(7L, "Mehmet Kaya"));
        assertThat(response.rejectionReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void submit_inLockedStatus_returns409_andSavesNothing(CourseStatus from) {
        given(ownership.requireOwnedCourse(5L, 7L)).willReturn(submittable(from));

        assertThatThrownBy(() -> service.submit(5L, 7L)).isInstanceOf(CourseStateException.class);
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void submit_withoutContent_returns400_andSavesNothing() {
        given(ownership.requireOwnedCourse(5L, 7L))
                .willReturn(CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.DRAFT));

        assertThatThrownBy(() -> service.submit(5L, 7L)).isInstanceOf(BadRequestException.class);
        verify(courseRepository, never()).saveAndFlush(any());
    }

    @Test
    void submit_someoneElsesOrMissingCourse_propagates403And404() {
        given(ownership.requireOwnedCourse(5L, 8L)).willThrow(new ForbiddenException("Bu kurs size ait değil."));
        given(ownership.requireOwnedCourse(6L, 7L)).willThrow(new NotFoundException("Kurs bulunamadı."));

        assertThatThrownBy(() -> service.submit(5L, 8L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.submit(6L, 7L)).isInstanceOf(NotFoundException.class);
        verify(courseRepository, never()).saveAndFlush(any());
    }
}
