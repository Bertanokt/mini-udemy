package com.miniudemy.course.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.course.CourseFixtures;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.LessonContentType;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseStats;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.dto.SectionResponse;

/** Katalog DTO dönüşümleri: alanlar sözleşmedeki adlarla eşlenir, kapak adresi ve hasContent türetilir. */
class CourseMapperTest {

    private final CourseMapper mapper = new CourseMapperImpl(new CategoryMapperImpl());
    private final UserSummary instructor = new UserSummary(7L, "Mehmet Demir");
    private final CourseStats stats = new CourseStats(12, 3600, 41L, null, 0);

    @Test
    void toSummary_mapsCourseInstructorAndStats() {
        Course course = CourseFixtures.course(5, 7, "Java Temelleri", CourseStatus.PUBLISHED);

        CourseSummaryResponse summary = mapper.toSummary(course, instructor, stats);

        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.title()).isEqualTo("Java Temelleri");
        assertThat(summary.category()).isEqualTo(new CategoryResponse(1L, "Yazılım"));
        assertThat(summary.level()).isEqualTo(CourseLevel.BEGINNER);
        assertThat(summary.status()).isEqualTo(CourseStatus.PUBLISHED);
        assertThat(summary.instructor()).isEqualTo(instructor);
        assertThat(summary.lessonCount()).isEqualTo(12);
        assertThat(summary.totalDurationSeconds()).isEqualTo(3600);
        assertThat(summary.enrollmentCount()).isEqualTo(41L);
        assertThat(summary.averageRating()).isNull();
        assertThat(summary.reviewCount()).isZero();
    }

    @Test
    void toSummary_withoutCover_coverUrlIsNull() {
        Course course = CourseFixtures.course(5, 7, "Kapaksız", CourseStatus.PUBLISHED);

        assertThat(mapper.toSummary(course, instructor, stats).coverUrl()).isNull();
    }

    @Test
    void toSummary_withCover_coverUrlPointsToCoverEndpoint() {
        Course course = CourseFixtures.course(5, 7, "Kapaklı", CourseStatus.PUBLISHED);
        ReflectionTestUtils.setField(course, "coverFileId", 99L);

        assertThat(mapper.toSummary(course, instructor, stats).coverUrl()).isEqualTo("/api/courses/5/cover");
    }

    @Test
    void toDetail_addsDescriptionLearningOutcomesAndSections() {
        Course course = CourseFixtures.course(5, 7, "Java Temelleri", CourseStatus.PUBLISHED);
        Section section = CourseFixtures.section(course, 10, "Giriş", 0);
        CourseFixtures.lesson(section, 20, "Kurulum", 300, 0, 55L);
        List<SectionResponse> sections = List.of(mapper.toSectionResponse(section));

        CourseDetailResponse detail = mapper.toDetail(course, instructor, stats, sections);

        assertThat(detail.id()).isEqualTo(5L);
        assertThat(detail.instructor()).isEqualTo(instructor);
        assertThat(detail.description()).isEqualTo("Java Temelleri açıklaması");
        assertThat(detail.learningOutcomes()).containsExactly("Hedef 1", "Hedef 2");
        assertThat(detail.sections()).isEqualTo(sections);
        assertThat(detail.enrollmentCount()).isEqualTo(41L);
    }

    @Test
    void toSectionResponse_mapsCourseIdAndLessons() {
        Course course = CourseFixtures.course(5, 7, "Java", CourseStatus.PUBLISHED);
        Section section = CourseFixtures.section(course, 10, "Giriş", 2);
        CourseFixtures.lesson(section, 20, "Kurulum", 300, 0, null);

        SectionResponse response = mapper.toSectionResponse(section);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.courseId()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Giriş");
        assertThat(response.position()).isEqualTo(2);
        assertThat(response.lessons()).hasSize(1);
    }

    @Test
    void toLessonResponse_hasContentFollowsContentFile() {
        Course course = CourseFixtures.course(5, 7, "Java", CourseStatus.PUBLISHED);
        Section section = CourseFixtures.section(course, 10, "Giriş", 0);
        Lesson withFile = CourseFixtures.lesson(section, 20, "Dosyalı", 540, 0, 55L);
        Lesson withoutFile = CourseFixtures.lesson(section, 21, "Dosyasız", 60, 1, null);

        LessonResponse yes = mapper.toLessonResponse(withFile);
        LessonResponse no = mapper.toLessonResponse(withoutFile);

        assertThat(yes).isEqualTo(new LessonResponse(20L, 10L, "Dosyalı", LessonContentType.VIDEO, true, 540, 0, true));
        assertThat(no.hasContent()).isFalse();
    }
}
