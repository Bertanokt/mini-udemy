package com.miniudemy.certificate.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.certificate.domain.Certificate;
import com.miniudemy.certificate.dto.CertificateResponse;
import com.miniudemy.certificate.dto.CertificateVerificationResponse;
import com.miniudemy.certificate.mapper.CertificateMapper;
import com.miniudemy.certificate.repository.CertificateRepository;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.dto.EnrollmentRef;
import com.miniudemy.enrollment.service.EnrollmentService;

/**
 * Sertifika görüntüleme ve herkese açık doğrulama (#130). Sertifika kaydı (enrollment) id'siyle tutulur; kursiyer ve
 * kurs {@code enrollment}'tan, kurs adı ve eğitmen {@code course}'tan, kursiyer adı {@code auth}'tan gelir.
 */
@Service
@Transactional(readOnly = true)
public class CertificateQueryService {

    static final String NOT_FOUND = "Sertifika bulunamadı.";

    private final CertificateRepository certificates;
    private final EnrollmentService enrollments;
    private final CourseLookupService courses;
    private final UserDirectoryService users;
    private final CertificateMapper mapper;

    public CertificateQueryService(CertificateRepository certificates, EnrollmentService enrollments,
            CourseLookupService courses, UserDirectoryService users, CertificateMapper mapper) {
        this.certificates = certificates;
        this.enrollments = enrollments;
        this.courses = courses;
        this.users = users;
        this.mapper = mapper;
    }

    /** Kursiyerin sertifikaları, en yeni önce. */
    public List<CertificateResponse> listMine(Long studentId) {
        Map<Long, EnrollmentRef> refs = enrollments.findRefsByStudent(studentId).stream()
                .collect(Collectors.toMap(EnrollmentRef::enrollmentId, ref -> ref));
        if (refs.isEmpty()) {
            return List.of();
        }
        List<Certificate> found = certificates.findByEnrollmentIdInOrderByIssuedAtDescIdDesc(refs.keySet());
        if (found.isEmpty()) {
            return List.of();
        }
        Map<Long, CourseSummaryResponse> courseCards = courses.findSummaries(found.stream()
                .map(c -> refs.get(c.getEnrollmentId()).courseId()).collect(Collectors.toSet()));
        String studentName = studentName(studentId);
        return found.stream().map(c -> mapper.toResponse(c,
                courseCards.get(refs.get(c.getEnrollmentId()).courseId()), studentName)).toList();
    }

    /** Kursiyerin bir sertifikası; yoksa ya da başkasınınsa 404 (ikisi ayırt edilmez). */
    public CertificateResponse getMine(Long certificateId, Long studentId) {
        Certificate certificate = certificates.findById(certificateId).orElseThrow(CertificateQueryService::notFound);
        EnrollmentRef ref = enrollments.findRef(certificate.getEnrollmentId())
                .filter(r -> r.studentId().equals(studentId))
                .orElseThrow(CertificateQueryService::notFound);
        return mapper.toResponse(certificate, course(ref.courseId()), studentName(studentId));
    }

    /** Doğrulama koduyla sertifika; kod geçersizse 404. Kod büyük/küçük harf ve baştaki/sondaki boşluktan bağımsız. */
    public CertificateVerificationResponse verify(String code) {
        Certificate certificate = certificates.findByVerificationCode(code.strip().toUpperCase(Locale.ROOT))
                .orElseThrow(CertificateQueryService::notFound);
        EnrollmentRef ref = enrollments.findRef(certificate.getEnrollmentId())
                .orElseThrow(CertificateQueryService::notFound);
        return mapper.toVerification(certificate, course(ref.courseId()), studentName(ref.studentId()));
    }

    private CourseSummaryResponse course(Long courseId) {
        CourseSummaryResponse course = courses.findSummaries(Set.of(courseId)).get(courseId);
        if (course == null) {
            // Kursiyeri olan kurs silinemez (#23); buraya yalnız tutarsız veride düşülür.
            throw notFound();
        }
        return course;
    }

    private String studentName(Long studentId) {
        UserSummary student = users.findSummaries(Set.of(studentId)).get(studentId);
        if (student == null) {
            throw notFound();
        }
        return student.fullName();
    }

    private static NotFoundException notFound() {
        return new NotFoundException(NOT_FOUND);
    }
}
