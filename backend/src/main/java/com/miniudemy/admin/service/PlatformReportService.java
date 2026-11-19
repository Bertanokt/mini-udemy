package com.miniudemy.admin.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.admin.dto.PlatformReportResponse;
import com.miniudemy.admin.dto.PlatformReportResponse.CategoryCount;
import com.miniudemy.admin.dto.PlatformReportResponse.TopCourse;
import com.miniudemy.auth.service.UserStatsService;
import com.miniudemy.certificate.service.CertificateStatsService;
import com.miniudemy.course.service.CoursePlatformStatsService;
import com.miniudemy.enrollment.service.EnrollmentStatsService;
import com.miniudemy.progress.service.CourseProgressStatsService;

/**
 * Platform raporu (proje dokümanı K ve §5, #145; İnce kapsam 17.11.2026). Veriler yalnız modüllerin service
 * sınıflarından gelir; admin modülü kimsenin repository'sine dokunmaz. Tamamlanma = ilerlemesi %100 olan kayıt.
 */
@Service
@Transactional(readOnly = true)
public class PlatformReportService {

    /** Sözleşmedeki ilk 10 (docs/api/openapi.yaml › PlatformReportResponse.topCourses). */
    static final int TOP_COURSES = 10;

    private final UserStatsService users;
    private final CoursePlatformStatsService courses;
    private final EnrollmentStatsService enrollments;
    private final CourseProgressStatsService progress;
    private final CertificateStatsService certificates;

    public PlatformReportService(UserStatsService users, CoursePlatformStatsService courses,
            EnrollmentStatsService enrollments, CourseProgressStatsService progress,
            CertificateStatsService certificates) {
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.progress = progress;
        this.certificates = certificates;
    }

    public PlatformReportResponse report() {
        Map<Long, Long> top = enrollments.topCourses(TOP_COURSES);
        Map<Long, String> titles = courses.titlesOf(top.keySet());
        List<TopCourse> topCourses = top.entrySet().stream()
                .filter(e -> titles.containsKey(e.getKey()))
                .map(e -> new TopCourse(e.getKey(), titles.get(e.getKey()), e.getValue()))
                .toList();
        List<CategoryCount> categories = courses.categoryDistribution().stream()
                .map(c -> new CategoryCount(c.categoryId(), c.name(), c.courseCount()))
                .toList();
        return new PlatformReportResponse(null, null, users.countByRole(), courses.countByStatus(),
                enrollments.countAll(), progress.countCompletedEnrollments(), certificates.countAll(), topCourses,
                categories);
    }
}
