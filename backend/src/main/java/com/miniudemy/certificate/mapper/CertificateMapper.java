package com.miniudemy.certificate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.certificate.domain.Certificate;
import com.miniudemy.certificate.dto.CertificateResponse;
import com.miniudemy.certificate.dto.CertificateVerificationResponse;
import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.course.dto.CourseSummaryResponse;

/**
 * Sertifikadan yanıt DTO'larına dönüşüm (ADR-006, #130). Kurs adı ve eğitmen {@code course}'tan, kursiyer adı
 * {@code auth}'tan hazır parametre olarak gelir; mapper başka servise gitmez.
 */
@Mapper(config = MapStructConfig.class)
public interface CertificateMapper {

    @Mapping(target = "id", source = "certificate.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "studentName", source = "studentName")
    @Mapping(target = "instructorName", source = "course.instructor.fullName")
    @Mapping(target = "verificationCode", source = "certificate.verificationCode")
    @Mapping(target = "issuedAt", source = "certificate.issuedAt")
    CertificateResponse toResponse(Certificate certificate, CourseSummaryResponse course, String studentName);

    @Mapping(target = "valid", constant = "true")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "studentName", source = "studentName")
    @Mapping(target = "issuedAt", source = "certificate.issuedAt")
    CertificateVerificationResponse toVerification(Certificate certificate, CourseSummaryResponse course,
            String studentName);
}
