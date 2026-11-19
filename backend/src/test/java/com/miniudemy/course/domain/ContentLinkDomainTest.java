package com.miniudemy.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.miniudemy.course.CourseFixtures;

/**
 * Dosya yükleme izinleri (#112): içerik yüklemenin ve kapak değiştirmenin kurs durumuna göre kuralı; kapak ve ders
 * dosyası değişince önceki dosyanın id'sinin dönmesi. Durum kararı State nesnelerinden gelir.
 */
class ContentLinkDomainTest {

    /** Durum × (derste zaten içerik var mı) → izinli mi. İçeriksiz derse ders eklemeyle aynı kural (FR-43). */
    @ParameterizedTest(name = "{0}, derste içerik var={1} → {2}")
    @CsvSource({
        "DRAFT,       false, OK",
        "DRAFT,       true,  OK",
        "IN_REVIEW,   false, 409",
        "IN_REVIEW,   true,  409",
        "PUBLISHED,   false, OK",
        "PUBLISHED,   true,  409",
        "UNPUBLISHED, false, OK",
        "UNPUBLISHED, true,  OK"
    })
    void assertContentUploadable_followsTheCourseState(CourseStatus status, boolean hasContent, String expected) {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", status);
        Lesson lesson = CourseFixtures.lesson(CourseFixtures.section(course, 1L, "Giriş", 0), 11L, "Kurulum", 60, 0,
                hasContent ? 900L : null);

        if (expected.equals("OK")) {
            assertThatCode(() -> course.assertContentUploadable(lesson)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> course.assertContentUploadable(lesson)).isInstanceOf(CourseStateException.class);
        }
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"DRAFT, OK", "UNPUBLISHED, OK", "IN_REVIEW, 409", "PUBLISHED, 409"})
    void replaceCover_isAllowedOnlyWhereTheCourseIsEditable(CourseStatus status, String expected) {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", status);

        if (expected.equals("OK")) {
            assertThat(course.replaceCover(50L)).isNull();
            assertThat(course.getCoverFileId()).isEqualTo(50L);
        } else {
            assertThatThrownBy(() -> course.replaceCover(50L)).isInstanceOf(CourseStateException.class);
            assertThat(course.getCoverFileId()).isNull();
        }
    }

    @Test
    void replaceCover_returnsThePreviousFileId() {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.DRAFT);
        course.replaceCover(50L);

        assertThat(course.replaceCover(51L)).isEqualTo(50L);
        assertThat(course.getCoverFileId()).isEqualTo(51L);
    }

    @Test
    void replaceContent_returnsThePreviousFileId() {
        Course course = CourseFixtures.course(5L, 7L, "Kurs", CourseStatus.DRAFT);
        Lesson lesson = CourseFixtures.lesson(CourseFixtures.section(course, 1L, "Giriş", 0), 11L, "Kurulum", 60, 0, null);

        assertThat(lesson.replaceContent(900L)).isNull();
        assertThat(lesson.replaceContent(901L)).isEqualTo(900L);
        assertThat(lesson.getContentFileId()).isEqualTo(901L);
    }
}
