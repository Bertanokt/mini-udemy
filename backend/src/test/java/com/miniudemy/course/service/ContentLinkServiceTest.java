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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.ContentFileDetached;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.CourseResponse;
import com.miniudemy.course.dto.LessonContentRef;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.CourseRepository;
import com.miniudemy.course.repository.LessonRepository;
import com.miniudemy.course.repository.SectionRepository;

/** Yüklenen dosyanın derse/kursa bağlanması (#112): 404, 403, 409 sırası ve eski dosya için olay. */
class ContentLinkServiceTest {

    private static final long OWNER = 7L;
    private static final long OTHER = 8L;

    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ContentLinkService service = new ContentLinkService(lessonRepository,
            new CourseOwnershipService(courseRepository, mock(SectionRepository.class)), userDirectory,
            new CourseMapperImpl(new CategoryMapperImpl()), events);

    private Course course;
    private Lesson empty;
    private Lesson filled;

    @BeforeEach
    void setUp() {
        build(CourseStatus.DRAFT);
        given(userDirectory.findSummaries(List.of(OWNER))).willReturn(Map.of(OWNER, new UserSummary(OWNER, "Mehmet Kaya")));
    }

    private void build(CourseStatus status) {
        course = CourseFixtures.course(5L, OWNER, "Kurs", status);
        Section section = CourseFixtures.section(course, 20L, "Giriş", 0);
        empty = CourseFixtures.lesson(section, 31L, "Boş", 60, 0, null);
        filled = CourseFixtures.lesson(section, 32L, "Dolu", 60, 1, 900L);
        given(lessonRepository.findById(31L)).willReturn(Optional.of(empty));
        given(lessonRepository.findById(32L)).willReturn(Optional.of(filled));
        given(courseRepository.findById(5L)).willReturn(Optional.of(course));
    }

    // assertLessonContentUploadable

    @Test
    void assertLessonContentUploadable_forTheOwner_returnsTheLessonTypeForThePolicy() {
        LessonContentRef ref = service.assertLessonContentUploadable(OWNER, 31L);

        assertThat(ref.contentType()).isEqualTo(LessonContentType.VIDEO);
        assertThat(ref.courseId()).isEqualTo(5L);
    }

    @Test
    void assertLessonContentUploadable_missingLesson_returnsNotFound() {
        assertThatThrownBy(() -> service.assertLessonContentUploadable(OWNER, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void assertLessonContentUploadable_byAnotherInstructor_returnsForbidden_beforeLookingAtTheState() {
        build(CourseStatus.IN_REVIEW);

        assertThatThrownBy(() -> service.assertLessonContentUploadable(OTHER, 31L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void assertLessonContentUploadable_inReview_returnsConflict() {
        build(CourseStatus.IN_REVIEW);

        assertThatThrownBy(() -> service.assertLessonContentUploadable(OWNER, 31L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void assertLessonContentUploadable_published_allowsAnEmptyLesson_butNotOneWithContent() {
        build(CourseStatus.PUBLISHED);

        assertThat(service.assertLessonContentUploadable(OWNER, 31L).lessonId()).isEqualTo(31L);
        assertThatThrownBy(() -> service.assertLessonContentUploadable(OWNER, 32L)).isInstanceOf(ConflictException.class);
    }

    // attachLessonContent

    @Test
    void attachLessonContent_toAnEmptyLesson_linksTheFile_andPublishesNothing() {
        LessonResponse response = service.attachLessonContent(OWNER, 31L, 500L);

        assertThat(response.hasContent()).isTrue();
        assertThat(empty.getContentFileId()).isEqualTo(500L);
        verify(lessonRepository).saveAndFlush(empty);
        verify(events, never()).publishEvent(any(ContentFileDetached.class));
    }

    @Test
    void attachLessonContent_replacingAFile_publishesContentFileDetachedForTheOldOne() {
        service.attachLessonContent(OWNER, 32L, 500L);

        assertThat(filled.getContentFileId()).isEqualTo(500L);
        verify(events).publishEvent(new ContentFileDetached(900L));
    }

    @Test
    void attachLessonContent_byAnotherInstructor_returnsForbidden_andLinksNothing() {
        assertThatThrownBy(() -> service.attachLessonContent(OTHER, 31L, 500L)).isInstanceOf(ForbiddenException.class);

        assertThat(empty.getContentFileId()).isNull();
        verify(lessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void attachLessonContent_whenTheCourseChangedInTheMeantime_returnsConflict_andKeepsTheOldFile() {
        build(CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> service.attachLessonContent(OWNER, 32L, 500L)).isInstanceOf(ConflictException.class);

        assertThat(filled.getContentFileId()).isEqualTo(900L);
        verify(events, never()).publishEvent(any(ContentFileDetached.class));
    }

    // cover

    @Test
    void assertCoverUploadable_followsOwnershipAndTheState() {
        service.assertCoverUploadable(OWNER, 5L);

        assertThatThrownBy(() -> service.assertCoverUploadable(OTHER, 5L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.assertCoverUploadable(OWNER, 99L)).isInstanceOf(NotFoundException.class);
        build(CourseStatus.PUBLISHED);
        assertThatThrownBy(() -> service.assertCoverUploadable(OWNER, 5L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void attachCover_setsTheCover_andReturnsTheCourseWithItsUrl() {
        CourseResponse response = service.attachCover(OWNER, 5L, 500L);

        assertThat(course.getCoverFileId()).isEqualTo(500L);
        assertThat(response.coverUrl()).isEqualTo("/api/courses/5/cover");
        verify(events, never()).publishEvent(any(ContentFileDetached.class));
    }

    @Test
    void attachCover_replacingACover_publishesContentFileDetachedForTheOldOne() {
        service.attachCover(OWNER, 5L, 500L);

        service.attachCover(OWNER, 5L, 501L);

        verify(events).publishEvent(new ContentFileDetached(500L));
    }

    @Test
    void attachCover_inPublishedCourse_returnsConflict_andChangesNothing() {
        build(CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> service.attachCover(OWNER, 5L, 500L)).isInstanceOf(ConflictException.class);

        assertThat(course.getCoverFileId()).isNull();
    }

    @Test
    void attachCover_byAnotherInstructor_returnsForbidden() {
        assertThatThrownBy(() -> service.attachCover(OTHER, 5L, 500L)).isInstanceOf(ForbiddenException.class);
        assertThat(course.getCoverFileId()).isNull();
    }
}
