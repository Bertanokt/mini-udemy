package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Section;
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
}
