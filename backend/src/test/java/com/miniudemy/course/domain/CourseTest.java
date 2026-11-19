package com.miniudemy.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.CourseFixtures;

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

    // --- düzenleme ve silme (#23) ---

    @Test
    void update_inDraft_changesFields_andNullOutcomesBecomeEmpty() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.DRAFT);
        Category design = CourseFixtures.category(2, "Tasarım");

        course.update(design, "Yeni başlık", "Yeni açıklama metni.", CourseLevel.INTERMEDIATE, null);

        assertThat(course.getCategory()).isSameAs(design);
        assertThat(course.getTitle()).isEqualTo("Yeni başlık");
        assertThat(course.getDescription()).isEqualTo("Yeni açıklama metni.");
        assertThat(course.getLevel()).isEqualTo(CourseLevel.INTERMEDIATE);
        assertThat(course.getLearningOutcomes()).isEmpty();
    }

    @Test
    void update_inPublished_isRejectedByTheState_andChangesNothing() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> course.update(CourseFixtures.category(2, "Tasarım"), "Yeni başlık",
                "Yeni açıklama metni.", CourseLevel.INTERMEDIATE, List.of()))
                .isInstanceOf(CourseStateException.class);
        assertThat(course.getTitle()).isEqualTo("Java");
    }

    @Test
    void attachedFileIds_coverAndLessonContents_withoutLessonsThatHaveNoFile() {
        Course course = CourseFixtures.course(5L, 7L, "Java", CourseStatus.DRAFT);
        ReflectionTestUtils.setField(course, "coverFileId", 300L);
        Section section = CourseFixtures.section(course, 1L, "Giriş", 0);
        CourseFixtures.lesson(section, 11L, "Video", 60, 0, 301L);
        CourseFixtures.lesson(section, 12L, "İçeriksiz", 60, 1, null);

        assertThat(course.attachedFileIds()).containsExactlyInAnyOrder(300L, 301L);
    }

    @Test
    void attachedFileIds_courseWithoutCoverOrFiles_isEmpty() {
        assertThat(CourseFixtures.course(5L, 7L, "Java", CourseStatus.DRAFT).attachedFileIds()).isEmpty();
    }
}
