package com.miniudemy.enrollment.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.enrollment.dto.EnrollmentProgressSnapshot;
import com.miniudemy.enrollment.dto.EnrollmentResponse;
import com.miniudemy.enrollment.mapper.EnrollmentMapper;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

/** Kursa kayıt ve kayıtlı kurslar (#30). Rol denetimi controller'dadır; burada yalnız iş kuralları var. */
@Service
public class EnrollmentService {

    private static final String ALREADY_ENROLLED = "Bu kursa zaten kayıtlısınız.";

    private final EnrollmentRepository repository;
    private final CourseLookupService courseLookup;
    private final EnrollmentMapper mapper;
    private final ApplicationEventPublisher events;
    private final EnrollmentProgress progress;

    public EnrollmentService(EnrollmentRepository repository, CourseLookupService courseLookup,
            EnrollmentMapper mapper, ApplicationEventPublisher events, EnrollmentProgress progress) {
        this.repository = repository;
        this.courseLookup = courseLookup;
        this.mapper = mapper;
        this.events = events;
        this.progress = progress;
    }

    /**
     * Kursiyeri yayımlanmış bir kursa kaydeder. Kurs yok/yayımlanmamış 404, ikinci kayıt 409. Önce var mı diye
     * bakılır; iki istek aynı anda gelirse veritabanındaki UNIQUE (student_id, course_id) kısıtı ikincisini
     * reddeder ve o da 409'a çevrilir.
     */
    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long studentId) {
        courseLookup.assertPublished(courseId);
        if (false && repository.existsByStudentIdAndCourseId(studentId, courseId)) { // KIRMIZI-C2
            throw new ConflictException(ALREADY_ENROLLED);
        }
        Enrollment enrollment;
        try {
            enrollment = repository.saveAndFlush(new Enrollment(studentId, courseId));
        } catch (DataIntegrityViolationException e) {
            // Kurs ve kullanıcı yukarıda doğrulandığı için burada ihlal edilebilecek tek kısıt UNIQUE'tir.
            throw e; // KIRMIZI-C2
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
        Map<Long, EnrollmentProgressSnapshot> snapshots = progress.snapshots(
                enrollments.stream().map(Enrollment::getId).toList());
        return enrollments.stream()
                .filter(e -> courses.containsKey(e.getCourseId()))
                .map(e -> toResponse(e, courses.get(e.getCourseId()),
                        snapshots.getOrDefault(e.getId(), EnrollmentProgressSnapshot.NONE)))
                .toList();
    }

    /** Kursiyer kursa kayıtlı mı. Başka modüller kayıt denetimini yalnız buradan yapar. */
    @Transactional(readOnly = true)
    public boolean isEnrolled(Long studentId, Long courseId) {
        return repository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /** Kursiyer kursa kayıtlı değilse 403. */
    @Transactional(readOnly = true)
    public void assertEnrolled(Long studentId, Long courseId) {
        if (!isEnrolled(studentId, courseId)) {
            throw new ForbiddenException("Bu kursa kayıtlı değilsiniz.");
        }
    }

    /** Kursiyerin bu kurstaki kaydının id'si; kayıtlı değilse 403. İlerleme kaydı kayda bağlıdır. */
    @Transactional(readOnly = true)
    public Long requireEnrollmentId(Long studentId, Long courseId) {
        return repository.findIdByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ForbiddenException("Bu kursa kayıtlı değilsiniz."));
    }

    /** Kursa kayıtlı herkesin kayıt id'leri (ders silinince yüzdeleri yeniden hesaplamak için). */
    @Transactional(readOnly = true)
    public List<Long> findEnrollmentIdsByCourse(Long courseId) {
        return repository.findIdsByCourseId(courseId);
    }

    /** Yeni kayıt: henüz ilerleme yok. */
    private EnrollmentResponse toResponse(Enrollment enrollment, CourseSummaryResponse course) {
        return toResponse(enrollment, course, EnrollmentProgressSnapshot.NONE);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, CourseSummaryResponse course,
            EnrollmentProgressSnapshot snapshot) {
        return mapper.toResponse(enrollment, course, snapshot.progressPercent(), snapshot.lastLessonId(),
                snapshot.lastAccessedAt());
    }
}
