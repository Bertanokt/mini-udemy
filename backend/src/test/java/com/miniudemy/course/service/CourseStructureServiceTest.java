package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.OrderedLesson;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.repository.SectionRepository;

class CourseStructureServiceTest {

    private final SectionRepository sections = mock(SectionRepository.class);
    private final CourseStructureService service = new CourseStructureService(sections);

    @Test
    void sectionLessons_returnsLessonIdsPerSectionIncludingEmptySection() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED);
        Section first = CourseFixtures.section(course, 1L, "Giriş", 1);
        CourseFixtures.lesson(first, 11L, "Kurulum", 60, 1, null);
        CourseFixtures.lesson(first, 12L, "Merhaba", 60, 2, null);
        Section empty = CourseFixtures.section(course, 2L, "Boş", 2);
        given(sections.findAllWithLessonsByCourseId(5L)).willReturn(List.of(first, empty));

        List<SectionLessons> result = service.sectionLessons(5L);

        assertThat(result).containsExactly(new SectionLessons(1L, List.of(11L, 12L)),
                new SectionLessons(2L, List.of()));
    }

    @Test
    void sectionLessons_courseWithoutSections_returnsEmptyList() {
        given(sections.findAllWithLessonsByCourseId(5L)).willReturn(List.of());

        assertThat(service.sectionLessons(5L)).isEmpty();
    }

    // --- sıralı dersler (#119) ---

    @Test
    void orderedLessons_listsLessonsInSectionThenLessonOrder_withTitles() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED);
        Section first = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(first, 11L, "Kurulum", 60, 0, null);
        CourseFixtures.lesson(first, 12L, "Merhaba", 60, 1, null);
        CourseFixtures.section(course, 2L, "Boş", 1);
        Section third = CourseFixtures.section(course, 3L, "Güvenlik", 2);
        CourseFixtures.lesson(third, 31L, "JWT", 60, 0, null);
        given(sections.findAllWithLessonsByCourseId(5L)).willReturn(List.of(first, course.getSections().get(1), third));

        assertThat(service.orderedLessons(5L)).containsExactly(new OrderedLesson(11L, 1L, "Kurulum"),
                new OrderedLesson(12L, 1L, "Merhaba"), new OrderedLesson(31L, 3L, "JWT"));
    }

    // --- zorunlu dersler (#129) ---

    @Test
    void mandatoryLessonIds_returnsOnlyMandatoryLessonsOfAllSections() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED);
        Section first = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(first, 11L, "Kurulum", 60, 0, null);
        Lesson optional = CourseFixtures.lesson(first, 12L, "Ek okuma", 60, 1, null);
        optional.update("Ek okuma", LessonContentType.VIDEO, false, 60);
        Section second = CourseFixtures.section(course, 2L, "Güvenlik", 1);
        CourseFixtures.lesson(second, 21L, "JWT", 60, 0, null);
        given(sections.findAllWithLessonsByCourseId(5L)).willReturn(List.of(first, second));

        assertThat(service.mandatoryLessonIds(5L)).containsExactlyInAnyOrder(11L, 21L);
    }

    @Test
    void mandatoryLessonIds_courseWithoutLessons_isEmpty() {
        given(sections.findAllWithLessonsByCourseId(5L)).willReturn(List.of());

        assertThat(service.mandatoryLessonIds(5L)).isEmpty();
    }
}
