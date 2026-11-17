package com.miniudemy.course;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.course.domain.Category;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.domain.Section;

/** Katalog testleri için hazır kurs, bölüm ve ders nesneleri (id ve durum yansıtmayla atanır). */
public final class CourseFixtures {

    private CourseFixtures() {
    }

    public static Category category(long id, String name) {
        Category category = new Category(name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static Course course(long id, long instructorId, String title, CourseStatus status) {
        Course course = new Course(instructorId, category(1, "Yazılım"), title, title + " açıklaması",
                CourseLevel.BEGINNER, List.of("Hedef 1", "Hedef 2"));
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "status", status);
        return course;
    }

    /** Bölümü kursa bağlar ve id verir; dersler {@link #lesson} ile eklenir. */
    public static Section section(Course course, long id, String title, int position) {
        Section section = new Section(title, position);
        ReflectionTestUtils.setField(section, "id", id);
        course.addSection(section);
        return section;
    }

    public static Lesson lesson(Section section, long id, String title, int durationSeconds, int position,
            Long contentFileId) {
        Lesson lesson = new Lesson(title, LessonContentType.VIDEO, true, durationSeconds, position);
        ReflectionTestUtils.setField(lesson, "id", id);
        ReflectionTestUtils.setField(lesson, "contentFileId", contentFileId);
        section.addLesson(lesson);
        return lesson;
    }
}
