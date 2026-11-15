package com.miniudemy.course.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Enum değerleri migration'daki CHECK kısıtlarıyla aynı olmalı; biri değişip diğeri unutulursa kayıt
 * veritabanında reddedilir. Bu test iki kaynağı karşılaştırır.
 */
class CourseEnumsTest {

    private static final String MIGRATION = "db/migration/V202611131449__create_course_structure.sql";

    private static List<String> checkValues(String constraint) throws IOException {
        String sql;
        try (InputStream in = CourseEnumsTest.class.getClassLoader().getResourceAsStream(MIGRATION)) {
            assertThat(in).as("migration dosyası classpath'te").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = Pattern.compile(constraint + "\\s+CHECK\\s*\\(\\s*\\w+\\s+IN\\s*\\(([^)]*)\\)").matcher(sql);
        assertThat(m.find()).as("%s kısıtı migration'da", constraint).isTrue();
        return Arrays.stream(m.group(1).split(",")).map(s -> s.trim().replace("'", "")).toList();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    @Test
    void courseStatus_matchesMigrationCheckConstraint() throws IOException {
        assertThat(names(CourseStatus.values())).containsExactlyElementsOf(checkValues("ck_courses_status"));
    }

    @Test
    void courseLevel_matchesMigrationCheckConstraint() throws IOException {
        assertThat(names(CourseLevel.values())).containsExactlyElementsOf(checkValues("ck_courses_level"));
    }

    @Test
    void lessonContentType_matchesMigrationCheckConstraint() throws IOException {
        assertThat(names(LessonContentType.values()))
                .containsExactlyElementsOf(checkValues("ck_lessons_content_type"));
    }
}
