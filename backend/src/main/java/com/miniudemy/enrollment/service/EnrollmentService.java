package com.miniudemy.enrollment.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.enrollment.dto.EnrollmentResponse;
import com.miniudemy.enrollment.mapper.EnrollmentMapper;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

/** Kursa kayıt ve kayıtlı kurslar (#30). Rol denetimi controller'dadır; burada yalnız iş kuralları var. */
@Service
public class EnrollmentService {

    private static final String ALREADY_ENROLLED = "Bu kursa zaten kayıtlısınız.";

    /**
     * İlerleme modülü (#43, #119) gelene dek her kayıt %0 ve "son izlenen ders yok" döner; modül gelince bu üç değer
     * course_progress'ten (tek toplu sorguyla) doldurulur.
     */
    private static final double PROGRESS_PLACEHOLDER = 0.0;

    private final EnrollmentRepository repository;
    private final CourseLookupService courseLookup;
    private final EnrollmentMapper mapper;
    private final ApplicationEventPublisher events;

    public EnrollmentService(EnrollmentRepository repository, CourseLookupService courseLookup,
            EnrollmentMapper mapper, ApplicationEventPublisher events) {
        this.repository = repository;
        this.courseLookup = courseLookup;
        this.mapper = mapper;
        this.events = events;
    }

    /**
     * Kursiyeri yayımlanmış bir kursa kaydeder. Kurs yok/yayımlanmamış 404, ikinci kayıt 409. Önce var mı diye
     * bakılır; iki istek aynı anda gelirse veritabanındaki UNIQUE (student_id, course_id) kısıtı ikincisini
     * reddeder ve o da 409'a çevrilir.
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long studentId) {
        courseLookup.assertPublished(courseId);
        if (repository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException(ALREADY_ENROLLED);
        }
        Enrollment enrollment;
        try {
            enrollment = repository.saveAndFlush(new Enrollment(studentId, courseId));
        } catch (DataIntegrityViolationException e) {
            // Kurs ve kullanıcı yukarıda doğrulandığı için burada ihlal edilebilecek tek kısıt UNIQUE'tir.
            throw new ConflictException(ALREADY_ENROLLED);
        }
        events.publishEvent(new EnrollmentCreated(enrollment.getId(), studentId, courseId,
                enrollment.getEnrolledAt()));
        CourseSummaryResponse course = courseLookup.findSummaries(List.of(courseId)).get(courseId);
        return toResponse(enrollment, course);
    }

    /** Kursiyerin kayıtları, en son kaydolunan önce. Sorgu sayısı kayıt sayısından bağımsızdır. */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listMyEnrollments(Long studentId) {
        List<Enrollment> enrollments = repository.findAllByStudentIdOrderByEnrolledAtDescIdDesc(studentId);
        if (enrollments.isEmpty()) {
            return List.of();
        }
        Map<Long, CourseSummaryResponse> courses = courseLookup.findSummaries(
                enrollments.stream().map(Enrollment::getCourseId).toList());
        return enrollments.stream()
                .filter(e -> courses.containsKey(e.getCourseId()))
                .map(e -> toResponse(e, courses.get(e.getCourseId())))
                .toList();
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, CourseSummaryResponse course) {
        return mapper.toResponse(enrollment, course, PROGRESS_PLACEHOLDER, null, null);
    }
}
