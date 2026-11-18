package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.repository.CourseRepository;

class CourseLookupServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseCatalogService catalogService = mock(CourseCatalogService.class);
    private final CourseLookupService service = new CourseLookupService(courseRepository, catalogService);

    @Test
    void assertPublished_publishedCourse_passes() {
        given(courseRepository.findById(5L))
                .willReturn(Optional.of(CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED)));

        assertThatCode(() -> service.assertPublished(5L)).doesNotThrowAnyException();
    }

    @Test
    void assertPublished_draftCourse_throwsNotFound() {
        given(courseRepository.findById(5L))
                .willReturn(Optional.of(CourseFixtures.course(5L, 7L, "Java", CourseStatus.DRAFT)));

        assertThatThrownBy(() -> service.assertPublished(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void assertPublished_missingCourse_throwsNotFound() {
        given(courseRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertPublished(5L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findSummaries_keysCardsByCourseId() {
        List<Course> courses = List.of(CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED));
        given(courseRepository.findAllByIdIn(List.of(5L))).willReturn(courses);
        CourseSummaryResponse card = new CourseSummaryResponse(5L, "Java", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, CourseStatus.PUBLISHED, new UserSummary(7L, "Mehmet Demir"), null, 0, 0, 0L,
                null, 0);
        given(catalogService.summaries(courses)).willReturn(List.of(card));

        assertThat(service.findSummaries(List.of(5L))).containsOnlyKeys(5L).containsValue(card);
    }

    @Test
    void findSummaries_withNoIds_returnsEmptyWithoutQuery() {
        assertThat(service.findSummaries(List.of())).isEmpty();

        verifyNoInteractions(courseRepository, catalogService);
    }

    // --- kapak (#113) ---

    @Test
    void coverFileIdOf_courseWithCover_returnsItsFile_courseWithoutCover_returnsEmpty() {
        Course withCover = CourseFixtures.course(5L, 7L, "Java", CourseStatus.DRAFT);
        ReflectionTestUtils.setField(withCover, "coverFileId", 900L);
        given(courseRepository.findById(5L)).willReturn(Optional.of(withCover));
        given(courseRepository.findById(6L)).willReturn(Optional.of(CourseFixtures.course(6L, 7L, "Go", CourseStatus.PUBLISHED)));

        assertThat(service.coverFileIdOf(5L)).contains(900L);
        assertThat(service.coverFileIdOf(6L)).isEmpty();
    }

    @Test
    void coverFileIdOf_unknownCourse_returns404() {
        given(courseRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.coverFileIdOf(9L)).isInstanceOf(NotFoundException.class)
                .hasMessage("Kurs bulunamadı.");
    }
}
