package com.miniudemy.course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CourseTest {

    private Course newCourse() {
        return new Course(1L, new Category("Yazılım"), "Spring Boot", "Temeller", CourseLevel.BEGINNER,
                List.of("REST API yazmak"));
    }

    @Test
    void totalDuration_sumsAllLessonsAcrossSections() {
        Course course = newCourse();
        Section intro = new Section("Giriş", 0);
        intro.addLesson(new Lesson("Kurulum", LessonContentType.VIDEO, true, 300, 0));
        intro.addLesson(new Lesson("Notlar", LessonContentType.PDF, false, 120, 1));
        Section advanced = new Section("İleri", 1);
        advanced.addLesson(new Lesson("Güvenlik", LessonContentType.VIDEO, true, 600, 0));
        course.addSection(intro);
        course.addSection(advanced);

        assertThat(intro.getTotalDurationSeconds()).isEqualTo(420);
        assertThat(course.getTotalDurationSeconds()).isEqualTo(1020);
    }

    @Test
    void totalDuration_isZeroWhenNoLessons() {
        Course course = newCourse();
        course.addSection(new Section("Boş bölüm", 0));

        assertThat(course.getTotalDurationSeconds()).isZero();
    }

    @Test
    void addSectionAndLesson_linkBothSidesOfRelation() {
        Course course = newCourse();
        Section section = new Section("Giriş", 0);
        Lesson lesson = new Lesson("Kurulum", LessonContentType.VIDEO, true, 300, 0);
        section.addLesson(lesson);
        course.addSection(section);

        assertThat(section.getCourse()).isSameAs(course);
        assertThat(lesson.getSection()).isSameAs(section);
        assertThat(course.getSections()).containsExactly(section);
        assertThat(section.getLessons()).containsExactly(lesson);
    }

    @Test
    void newCourse_startsAsDraft() {
        assertThat(newCourse().getStatus()).isEqualTo(CourseStatus.DRAFT);
    }
}
