package com.miniudemy.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.certificate.domain.Certificate;
import com.miniudemy.certificate.dto.CertificateResponse;
import com.miniudemy.certificate.dto.CertificateVerificationResponse;
import com.miniudemy.certificate.mapper.CertificateMapperImpl;
import com.miniudemy.certificate.repository.CertificateRepository;
import com.miniudemy.common.exception.ApiException;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.dto.EnrollmentRef;
import com.miniudemy.enrollment.service.EnrollmentService;

/**
 * Sertifika görüntüleme ve herkese açık doğrulama (#130): kursiyer yalnız kendi sertifikasını görür (başkasınınki 404),
 * doğrulama yalnız ad, kurs, tarih ve geçerlilik döner, geçersiz kod 404.
 */
class CertificateQueryServiceTest {

    private static final long AYSE = 42L;
    private static final long CAN = 43L;
    private static final Instant OLDER = Instant.parse("2026-11-10T09:00:00Z");
    private static final Instant NEWER = Instant.parse("2026-11-18T09:00:00Z");

    private final CertificateRepository certificates = mock(CertificateRepository.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final CourseLookupService courses = mock(CourseLookupService.class);
    private final UserDirectoryService users = mock(UserDirectoryService.class);
    private final CertificateQueryService service = new CertificateQueryService(certificates, enrollments, courses,
            users, new CertificateMapperImpl());

    private final Certificate python = certificate(900L, 500L, "K7Q2-M9XA-4T", NEWER);
    private final Certificate java = certificate(901L, 501L, "ABCD-EFGH-JK", OLDER);

    @BeforeEach
    void setUp() {
        given(enrollments.findRefsByStudent(AYSE)).willReturn(List.of(new EnrollmentRef(500L, AYSE, 7L),
                new EnrollmentRef(501L, AYSE, 8L), new EnrollmentRef(502L, AYSE, 9L)));
        given(enrollments.findRef(500L)).willReturn(Optional.of(new EnrollmentRef(500L, AYSE, 7L)));
        given(enrollments.findRef(501L)).willReturn(Optional.of(new EnrollmentRef(501L, AYSE, 8L)));
        given(courses.findSummaries(anyCollection())).willReturn(Map.of(
                7L, course(7L, "Sıfırdan Python", "Mehmet Kaya"), 8L, course(8L, "Java ile OOP", "Zeynep Ak")));
        // Can'ın adı da bilinir: başkasının sertifikasındaki 404'ün sebebi sahiplik olmalı, ad bulunamaması değil.
        given(users.findSummaries(anyCollection())).willReturn(Map.of(AYSE, new UserSummary(AYSE, "Ayşe Yılmaz"),
                CAN, new UserSummary(CAN, "Can Demir")));
    }

    private static Certificate certificate(long id, long enrollmentId, String code, Instant issuedAt) {
        Certificate certificate = org.springframework.beans.BeanUtils.instantiateClass(Certificate.class);
        ReflectionTestUtils.setField(certificate, "id", id);
        ReflectionTestUtils.setField(certificate, "enrollmentId", enrollmentId);
        ReflectionTestUtils.setField(certificate, "verificationCode", code);
        ReflectionTestUtils.setField(certificate, "issuedAt", issuedAt);
        return certificate;
    }

    private static CourseSummaryResponse course(long id, String title, String instructor) {
        return new CourseSummaryResponse(id, title, null, null, null, new UserSummary(id + 100, instructor), null, 0, 0,
                0L, null, 0);
    }

    // --- sertifikalarım ---

    @Test
    void listMine_returnsOnlyCertificatesOfTheStudentsEnrollments_newestFirst_withNames() {
        given(certificates.findByEnrollmentIdInOrderByIssuedAtDescIdDesc(Set.of(500L, 501L, 502L)))
                .willReturn(List.of(python, java));

        List<CertificateResponse> result = service.listMine(AYSE);

        assertThat(result).containsExactly(
                new CertificateResponse(900L, 7L, "Sıfırdan Python", "Ayşe Yılmaz", "Mehmet Kaya", "K7Q2-M9XA-4T", NEWER),
                new CertificateResponse(901L, 8L, "Java ile OOP", "Ayşe Yılmaz", "Zeynep Ak", "ABCD-EFGH-JK", OLDER));
    }

    @Test
    void listMine_studentWithoutEnrollments_isEmpty_withoutQueryingCertificates() {
        given(enrollments.findRefsByStudent(CAN)).willReturn(List.of());

        assertThat(service.listMine(CAN)).isEmpty();
        verify(certificates, never()).findByEnrollmentIdInOrderByIssuedAtDescIdDesc(anyCollection());
    }

    @Test
    void getMine_ownCertificate_returnsIt() {
        given(certificates.findById(900L)).willReturn(Optional.of(python));

        assertThat(service.getMine(900L, AYSE)).isEqualTo(
                new CertificateResponse(900L, 7L, "Sıfırdan Python", "Ayşe Yılmaz", "Mehmet Kaya", "K7Q2-M9XA-4T", NEWER));
    }

    @Test
    void getMine_someoneElsesCertificate_returns404_likeAMissingOne() {
        given(certificates.findById(900L)).willReturn(Optional.of(python));
        given(certificates.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(900L, CAN)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(e.getMessage()).isEqualTo(CertificateQueryService.NOT_FOUND);
        });
        assertThatThrownBy(() -> service.getMine(999L, AYSE)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(e.getMessage()).isEqualTo(CertificateQueryService.NOT_FOUND);
        });
    }

    // --- doğrulama ---

    @Test
    void verify_validCode_returnsNameCourseDateAndValid() {
        given(certificates.findByVerificationCode("K7Q2-M9XA-4T")).willReturn(Optional.of(python));

        assertThat(service.verify("K7Q2-M9XA-4T"))
                .isEqualTo(new CertificateVerificationResponse(true, "Sıfırdan Python", "Ayşe Yılmaz", NEWER));
    }

    @Test
    void verify_codeIsCaseAndSurroundingSpaceInsensitive() {
        given(certificates.findByVerificationCode("K7Q2-M9XA-4T")).willReturn(Optional.of(python));

        assertThat(service.verify("  k7q2-m9xa-4t ").valid()).isTrue();
    }

    @Test
    void verify_unknownCode_returns404() {
        given(certificates.findByVerificationCode(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("XXXX-XXXX-XX")).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
