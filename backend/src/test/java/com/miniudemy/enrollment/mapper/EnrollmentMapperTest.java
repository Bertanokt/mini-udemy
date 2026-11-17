package com.miniudemy.enrollment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.dto.EnrollmentResponse;

class EnrollmentMapperTest {

    private final EnrollmentMapper mapper = new EnrollmentMapperImpl();

    @Test
    void toResponse_mapsEnrollmentCourseAndProgress() {
        Enrollment enrollment = new Enrollment(42L, 5L);
        ReflectionTestUtils.setField(enrollment, "id", 9L);
        Instant at = Instant.parse("2026-11-10T10:00:00Z");
        ReflectionTestUtils.setField(enrollment, "enrolledAt", at);
        CourseSummaryResponse course = new CourseSummaryResponse(5L, "Java", new CategoryResponse(1L, "Yazılım"),
                CourseLevel.BEGINNER, CourseStatus.PUBLISHED, new UserSummary(7L, "Mehmet Demir"), null, 12, 3600,
                1L, null, 0);
        Instant last = Instant.parse("2026-11-11T09:00:00Z");

        EnrollmentResponse response = mapper.toResponse(enrollment, course, 75.0, 11L, last);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.course()).isSameAs(course);
        assertThat(response.enrolledAt()).isEqualTo(at);
        assertThat(response.progressPercent()).isEqualTo(75.0);
        assertThat(response.lastLessonId()).isEqualTo(11L);
        assertThat(response.lastAccessedAt()).isEqualTo(last);
    }
}
