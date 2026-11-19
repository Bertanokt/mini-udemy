package com.miniudemy.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.miniudemy.admin.dto.PlatformReportResponse;
import com.miniudemy.admin.dto.PlatformReportResponse.CategoryCount;
import com.miniudemy.admin.dto.PlatformReportResponse.TopCourse;
import com.miniudemy.auth.service.UserStatsService;
import com.miniudemy.certificate.service.CertificateStatsService;
import com.miniudemy.course.dto.CategoryCourseCount;
import com.miniudemy.course.service.CoursePlatformStatsService;
import com.miniudemy.enrollment.service.EnrollmentStatsService;
import com.miniudemy.progress.service.CourseProgressStatsService;

/** Platform raporu (#145): modül servislerinden gelen sayıların rapora doğru yerleşmesi. */
class PlatformReportServiceTest {

    private final UserStatsService users = mock(UserStatsService.class);
    private final CoursePlatformStatsService courses = mock(CoursePlatformStatsService.class);
    private final EnrollmentStatsService enrollments = mock(EnrollmentStatsService.class);
    private final CourseProgressStatsService progress = mock(CourseProgressStatsService.class);
    private final CertificateStatsService certificates = mock(CertificateStatsService.class);
    private final PlatformReportService service =
            new PlatformReportService(users, courses, enrollments, progress, certificates);

    @BeforeEach
    void stubs() {
        given(users.countByRole()).willReturn(Map.of("STUDENT", 6L, "INSTRUCTOR", 2L, "ADMIN", 1L));
        given(courses.countByStatus()).willReturn(Map.of("DRAFT", 1L, "PUBLISHED", 2L));
        given(enrollments.countAll()).willReturn(7L);
        given(progress.countCompletedEnrollments()).willReturn(1L);
        given(certificates.countAll()).willReturn(1L);
        given(courses.categoryDistribution()).willReturn(List.of(new CategoryCourseCount(1L, "Yazılım", 4)));
    }

    @Test
    void report_putsEveryModulesCountInItsField_andLeavesTheDateRangeEmpty() {
        given(enrollments.topCourses(10)).willReturn(Map.of());
        given(courses.titlesOf(any())).willReturn(Map.of());

        PlatformReportResponse report = service.report();

        assertThat(report.from()).isNull();
        assertThat(report.to()).isNull();
        assertThat(report.usersByRole()).containsEntry("STUDENT", 6L).containsEntry("ADMIN", 1L);
        assertThat(report.coursesByStatus()).containsEntry("PUBLISHED", 2L);
        assertThat(report.enrollmentCount()).isEqualTo(7);
        assertThat(report.completionCount()).isEqualTo(1);
        assertThat(report.certificateCount()).isEqualTo(1);
        assertThat(report.categoryDistribution()).containsExactly(new CategoryCount(1L, "Yazılım", 4));
    }

    @Test
    void topCourses_keepTheEnrollmentOrder_withTitles_upToTen() {
        Map<Long, Long> top = new LinkedHashMap<>();
        top.put(5L, 30L);
        top.put(2L, 12L);
        top.put(9L, 3L);
        given(enrollments.topCourses(10)).willReturn(top);
        given(courses.titlesOf(top.keySet())).willReturn(Map.of(2L, "Java", 5L, "Python", 9L, "SQL"));

        assertThat(service.report().topCourses()).containsExactly(
                new TopCourse(5L, "Python", 30), new TopCourse(2L, "Java", 12), new TopCourse(9L, "SQL", 3));
    }

    @Test
    void topCourses_skipACourseWhoseTitleCannotBeFound() {
        Map<Long, Long> top = new LinkedHashMap<>();
        top.put(5L, 30L);
        top.put(6L, 10L);
        given(enrollments.topCourses(10)).willReturn(top);
        given(courses.titlesOf(top.keySet())).willReturn(Map.of(5L, "Python"));

        assertThat(service.report().topCourses()).containsExactly(new TopCourse(5L, "Python", 30));
    }
}
