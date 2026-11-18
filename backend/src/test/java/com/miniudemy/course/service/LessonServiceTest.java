package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.miniudemy.common.exception.BadRequestException;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLessonsChanged;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.domain.LessonPublished;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.LessonRequest;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.mapper.CategoryMapperImpl;
import com.miniudemy.course.mapper.CourseMapperImpl;
import com.miniudemy.course.repository.LessonRepository;
import com.miniudemy.course.repository.SectionRepository;

/** Ders CRUD ve sıralama (#25). Depolar taklit; kurs durumu gerçek State nesneleriyle, mapper gerçek. */
class LessonServiceTest {

    private static final long OWNER = 7L;
    private static final long OTHER = 8L;

    private final SectionRepository sectionRepository = mock(SectionRepository.class);
    private final LessonRepository lessonRepository = mock(LessonRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final LessonService service = new LessonService(sectionRepository, lessonRepository,
            new CourseMapperImpl(new CategoryMapperImpl()), events);

    private Course course;
    private Section section;
    private Lesson first;
    private Lesson second;
    private Lesson third;

    @BeforeEach
    void setUp() {
        given(lessonRepository.saveAndFlush(any(Lesson.class))).willAnswer(call -> {
            Lesson lesson = call.getArgument(0);
            if (lesson.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(lesson, "id", 100L);
            }
            return lesson;
        });
        build(CourseStatus.DRAFT);
    }

    /** 3 dersli bir bölüm kurar; kurs verilen durumda. */
    private void build(CourseStatus status) {
        course = CourseFixtures.course(5L, OWNER, "Kurs", status);
        section = CourseFixtures.section(course, 20L, "Giriş", 0);
        first = CourseFixtures.lesson(section, 31L, "Bir", 60, 0, null);
        second = CourseFixtures.lesson(section, 32L, "İki", 60, 1, 900L);
        third = CourseFixtures.lesson(section, 33L, "Üç", 60, 2, null);
        given(sectionRepository.findById(20L)).willReturn(Optional.of(section));
        given(lessonRepository.findById(31L)).willReturn(Optional.of(first));
        given(lessonRepository.findById(32L)).willReturn(Optional.of(second));
        given(lessonRepository.findById(33L)).willReturn(Optional.of(third));
    }

    private static LessonRequest request(String title, LessonContentType type, boolean mandatory, int seconds) {
        return new LessonRequest(title, type, mandatory, seconds);
    }

    private List<String> titles() {
        return section.getLessons().stream().map(Lesson::getTitle).toList();
    }

    private List<Integer> positions() {
        return section.getLessons().stream().map(Lesson::getPosition).toList();
    }

    // create

    @Test
    void create_inDraftCourse_appendsAtTheEnd_withTheGivenFields_andPublishesOnlyCourseLessonsChanged() {
        LessonResponse response = service.create(OWNER, 20L, request("Dört", LessonContentType.PDF, false, 120));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.sectionId()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("Dört");
        assertThat(response.contentType()).isEqualTo(LessonContentType.PDF);
        assertThat(response.mandatory()).isFalse();
        assertThat(response.durationSeconds()).isEqualTo(120);
        assertThat(response.position()).isEqualTo(3);
        assertThat(response.hasContent()).isFalse();
        assertThat(titles()).containsExactly("Bir", "İki", "Üç", "Dört");
        verify(events).publishEvent(new CourseLessonsChanged(5L));
        verify(events, never()).publishEvent(any(LessonPublished.class));
    }

    @Test
    void create_inPublishedCourse_publishesLessonPublishedWithLessonAndCourseIds() {
        build(CourseStatus.PUBLISHED);

        service.create(OWNER, 20L, request("Dört", LessonContentType.VIDEO, true, 300));

        ArgumentCaptor<LessonPublished> event = ArgumentCaptor.forClass(LessonPublished.class);
        verify(events).publishEvent(event.capture());
        verify(events).publishEvent(new CourseLessonsChanged(5L));
        LessonPublished published = event.getValue();
        assertThat(published.lessonId()).isEqualTo(100L);
        assertThat(published.courseId()).isEqualTo(5L);
        assertThat(published.publishedAt()).isNotNull();
    }

    @Test
    void create_inUnpublishedCourse_works_andPublishesOnlyCourseLessonsChanged() {
        build(CourseStatus.UNPUBLISHED);

        service.create(OWNER, 20L, request("Dört", LessonContentType.VIDEO, true, 300));

        assertThat(section.getLessons()).hasSize(4);
        verify(events).publishEvent(new CourseLessonsChanged(5L));
        verify(events, never()).publishEvent(any(LessonPublished.class));
    }

    @Test
    void create_inCourseInReview_returnsConflict_andAddsNothing() {
        build(CourseStatus.IN_REVIEW);

        assertThatThrownBy(() -> service.create(OWNER, 20L, request("Dört", LessonContentType.VIDEO, true, 1)))
                .isInstanceOf(ConflictException.class);
        assertThat(section.getLessons()).hasSize(3);
        verify(lessonRepository, never()).saveAndFlush(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void create_byAnotherInstructor_returnsForbidden_evenWhenCourseIsPublished() {
        build(CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> service.create(OTHER, 20L, request("Dört", LessonContentType.VIDEO, true, 1)))
                .isInstanceOf(ForbiddenException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void create_inMissingSection_returnsNotFound() {
        given(sectionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(OWNER, 99L, request("Dört", LessonContentType.VIDEO, true, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    // update

    @Test
    void update_changesTheFields_andKeepsPositionAndContent() {
        LessonResponse response = service.update(OWNER, 32L, request("İki-b", LessonContentType.VIDEO, false, 75));

        assertThat(response.title()).isEqualTo("İki-b");
        assertThat(response.mandatory()).isFalse();
        assertThat(response.durationSeconds()).isEqualTo(75);
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.hasContent()).as("tür aynı: dosya kalır").isTrue();
    }

    @Test
    void update_withAnotherContentType_detachesTheUploadedFile() {
        LessonResponse response = service.update(OWNER, 32L, request("İki", LessonContentType.PDF, true, 60));

        assertThat(response.contentType()).isEqualTo(LessonContentType.PDF);
        assertThat(response.hasContent()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void update_inLockedCourse_returnsConflict_andChangesNothing(CourseStatus status) {
        build(status);

        assertThatThrownBy(() -> service.update(OWNER, 31L, request("Yeni", LessonContentType.PDF, false, 5)))
                .isInstanceOf(ConflictException.class);
        assertThat(first.getTitle()).isEqualTo("Bir");
        verify(lessonRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_byAnotherInstructor_returnsForbidden() {
        assertThatThrownBy(() -> service.update(OTHER, 31L, request("Yeni", LessonContentType.PDF, false, 5)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(first.getTitle()).isEqualTo("Bir");
    }

    @Test
    void update_missingLesson_returnsNotFound() {
        given(lessonRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(OWNER, 99L, request("Yeni", LessonContentType.PDF, false, 5)))
                .isInstanceOf(NotFoundException.class);
    }

    // delete

    @Test
    void delete_removesTheLesson_andClosesTheGapInPositions() {
        service.delete(OWNER, 31L);

        assertThat(titles()).containsExactly("İki", "Üç");
        assertThat(positions()).containsExactly(0, 1);
        verify(lessonRepository).flush();
        verify(events).publishEvent(new CourseLessonsChanged(5L));
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void delete_inLockedCourse_returnsConflict_andKeepsTheLesson(CourseStatus status) {
        build(status);

        assertThatThrownBy(() -> service.delete(OWNER, 31L)).isInstanceOf(ConflictException.class);
        assertThat(section.getLessons()).hasSize(3);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void delete_byAnotherInstructor_returnsForbidden_andKeepsTheLesson() {
        assertThatThrownBy(() -> service.delete(OTHER, 31L)).isInstanceOf(ForbiddenException.class);
        assertThat(section.getLessons()).hasSize(3);
    }

    @Test
    void delete_missingLesson_returnsNotFound() {
        given(lessonRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OWNER, 99L)).isInstanceOf(NotFoundException.class);
    }

    // move

    @Test
    void move_toTheEnd_shiftsTheOthersUp_andReturnsTheSectionInNewOrder() {
        List<LessonResponse> result = service.move(OWNER, 31L, 2);

        assertThat(titles()).containsExactly("İki", "Üç", "Bir");
        assertThat(positions()).containsExactly(0, 1, 2);
        assertThat(result).extracting(LessonResponse::id).containsExactly(32L, 33L, 31L);
        assertThat(result).extracting(LessonResponse::position).containsExactly(0, 1, 2);
    }

    @Test
    void move_toTheStart_shiftsTheOthersDown() {
        service.move(OWNER, 33L, 0);

        assertThat(titles()).containsExactly("Üç", "Bir", "İki");
        assertThat(positions()).containsExactly(0, 1, 2);
    }

    @Test
    void move_toTheSamePosition_changesNothing() {
        service.move(OWNER, 32L, 1);

        assertThat(titles()).containsExactly("Bir", "İki", "Üç");
    }

    @Test
    void move_beyondTheLastPosition_returnsBadRequestWithPositionError() {
        assertThatThrownBy(() -> service.move(OWNER, 31L, 3))
                .isInstanceOfSatisfying(BadRequestException.class,
                        ex -> assertThat(ex.getErrors()).containsKey("position"));
        assertThat(titles()).containsExactly("Bir", "İki", "Üç");
    }

    @ParameterizedTest
    @EnumSource(value = CourseStatus.class, names = {"IN_REVIEW", "PUBLISHED"})
    void move_inLockedCourse_returnsConflict(CourseStatus status) {
        build(status);

        assertThatThrownBy(() -> service.move(OWNER, 31L, 2)).isInstanceOf(ConflictException.class);
        assertThat(titles()).containsExactly("Bir", "İki", "Üç");
    }

    @Test
    void move_byAnotherInstructor_returnsForbidden() {
        assertThatThrownBy(() -> service.move(OTHER, 31L, 2)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void move_missingLesson_returnsNotFound() {
        given(lessonRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.move(OWNER, 99L, 0)).isInstanceOf(NotFoundException.class);
    }

    // ders sayısı değişmeyen işlemler (#249)

    @Test
    void updateAndMove_doNotChangeTheLessonCount_andPublishNothing() {
        service.update(OWNER, 31L, request("Bir+", LessonContentType.VIDEO, true, 60));
        service.move(OWNER, 31L, 2);

        verify(events, never()).publishEvent(any(Object.class));
    }
}
