package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;

/** Kurs detayı ve bölüm listesinin görünürlüğü (#123): yayımlanmamış kursu sahibi, yönetici ve kayıtlı kursiyer görür. */
class CourseVisibilityTest {

    private static final long OWNER = 7L;
    private static final long STUDENT = 42L;

    private final CourseRoster roster = mock(CourseRoster.class);
    private final CourseVisibility visibility = new CourseVisibility(roster);

    @Test
    void publishedCourse_isVisibleToEveryone_withoutAskingForEnrollment() {
        Course course = CourseFixtures.course(5L, OWNER, "Kurs", CourseStatus.PUBLISHED);

        assertThatCode(() -> visibility.assertVisible(course, null, false)).doesNotThrowAnyException();
        assertThatCode(() -> visibility.assertVisible(course, STUDENT, false)).doesNotThrowAnyException();
        verifyNoInteractions(roster);
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = "PUBLISHED", mode = EnumSource.Mode.EXCLUDE)
    void unpublishedCourse_isVisibleToOwnerAdminAndEnrolledStudent_only(CourseStatus status) {
        Course course = CourseFixtures.course(5L, OWNER, "Kurs", status);
        given(roster.isEnrolled(5L, STUDENT)).willReturn(true);

        assertThatCode(() -> visibility.assertVisible(course, OWNER, false)).doesNotThrowAnyException();
        assertThatCode(() -> visibility.assertVisible(course, 1L, true)).doesNotThrowAnyException();
        assertThatCode(() -> visibility.assertVisible(course, STUDENT, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> visibility.assertVisible(course, 43L, false))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Kurs bulunamadı.");
        assertThatThrownBy(() -> visibility.assertVisible(course, null, false)).isInstanceOf(NotFoundException.class);
    }
}
