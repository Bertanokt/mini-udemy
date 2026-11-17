package com.miniudemy.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.exception.ConflictException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.domain.CourseLevel;
import com.miniudemy.course.domain.CourseStatus;
import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.service.CourseLookupService;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.domain.EnrollmentCreated;
import com.miniudemy.enrollment.dto.EnrollmentResponse;
import com.miniudemy.enrollment.mapper.EnrollmentMapperImpl;
import com.miniudemy.enrollment.repository.EnrollmentRepository;

/** Kursa kayıt ve kayıt listesi (#30): yayımlanma, mükerrer ve eşzamanlı kayıt, olay, liste. */
class EnrollmentServiceTest {

    private final EnrollmentRepository repository = mock(EnrollmentRepository.class);
    private final CourseLookupService courseLookup = mock(CourseLookupService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final EnrollmentService service = new EnrollmentService(repository, courseLookup,
            new EnrollmentMapperImpl(), events);

    private static CourseSummaryResponse card(long id, String title) {
        return new CourseSummaryResponse(id, title, new CategoryResponse(1L, "Yazılım"), CourseLevel.BEGINNER,
                CourseStatus.PUBLISHED, new UserSummary(7L, "Mehmet Demir"), null, 12, 3600, 1L, null, 0);
    }

    private static Enrollment enrollment(long id, long studentId, long courseId, Instant at) {
        Enrollment e = new Enrollment(studentId, courseId);
        ReflectionTestUtils.setField(e, "id", id);
        ReflectionTestUtils.setField(e, "enrolledAt", at);
        return e;
    }

    private void saveAssignsId99() {
        given(repository.saveAndFlush(any(Enrollment.class))).willAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 99L);
            return e;
        });
    }

    // --- enroll ---

    @Test
    void enroll_publishedCourse_savesAndReturnsEnrollmentWithCourseAndZeroProgress() {
        saveAssignsId99();
        given(courseLookup.findSummaries(List.of(5L))).willReturn(Map.of(5L, card(5L, "Java")));

        EnrollmentResponse response = service.enroll(5L, 42L);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.course().title()).isEqualTo("Java");
        assertThat(response.enrolledAt()).isNotNull();
        assertThat(response.progressPercent()).isZero();
        assertThat(response.lastLessonId()).isNull();
        assertThat(response.lastAccessedAt()).isNull();
        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStudentId()).isEqualTo(42L);
        assertThat(saved.getValue().getCourseId()).isEqualTo(5L);
    }

    @Test
    void enroll_publishesEnrollmentCreatedEvent() {
        saveAssignsId99();
        given(courseLookup.findSummaries(List.of(5L))).willReturn(Map.of(5L, card(5L, "Java")));

        service.enroll(5L, 42L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOfSatisfying(EnrollmentCreated.class, e -> {
            assertThat(e.enrollmentId()).isEqualTo(99L);
            assertThat(e.studentId()).isEqualTo(42L);
            assertThat(e.courseId()).isEqualTo(5L);
            assertThat(e.enrolledAt()).isNotNull();
        });
    }

    @Test
    void enroll_unpublishedOrMissingCourse_throwsNotFoundAndSavesNothing() {
        willThrow(new NotFoundException("Kurs bulunamadı.")).given(courseLookup).assertPublished(5L);

        assertThatThrownBy(() -> service.enroll(5L, 42L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void enroll_secondTime_throwsConflictWithoutSaving() {
        given(repository.existsByStudentIdAndCourseId(42L, 5L)).willReturn(true);

        assertThatThrownBy(() -> service.enroll(5L, 42L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Bu kursa zaten kayıtlısınız.");

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void enroll_concurrentDuplicate_uniqueViolationBecomesConflict() {
        given(repository.existsByStudentIdAndCourseId(42L, 5L)).willReturn(false);
        given(repository.saveAndFlush(any(Enrollment.class)))
                .willThrow(new DataIntegrityViolationException("uk_enrollments_student_course"));

        assertThatThrownBy(() -> service.enroll(5L, 42L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Bu kursa zaten kayıtlısınız.");

        verifyNoInteractions(events);
    }

    // --- listMyEnrollments ---

    @Test
    void listMyEnrollments_returnsCourseTitleAndProgress_inRepositoryOrder() {
        Instant now = Instant.parse("2026-11-10T10:00:00Z");
        given(repository.findAllByStudentIdOrderByEnrolledAtDescIdDesc(42L)).willReturn(List.of(
                enrollment(2L, 42L, 6L, now), enrollment(1L, 42L, 5L, now.minusSeconds(60))));
        given(courseLookup.findSummaries(anyCollection()))
                .willReturn(Map.of(5L, card(5L, "Java"), 6L, card(6L, "Spring")));

        List<EnrollmentResponse> result = service.listMyEnrollments(42L);

        assertThat(result).extracting(r -> r.course().title()).containsExactly("Spring", "Java");
        assertThat(result).extracting(EnrollmentResponse::progressPercent).containsExactly(0.0, 0.0);
        assertThat(result.get(0).enrolledAt()).isEqualTo(now);
    }

    @Test
    void listMyEnrollments_loadsAllCoursesInOneCall() {
        Instant now = Instant.parse("2026-11-10T10:00:00Z");
        given(repository.findAllByStudentIdOrderByEnrolledAtDescIdDesc(42L)).willReturn(List.of(
                enrollment(3L, 42L, 7L, now), enrollment(2L, 42L, 6L, now), enrollment(1L, 42L, 5L, now)));
        given(courseLookup.findSummaries(anyCollection())).willReturn(
                Map.of(5L, card(5L, "A"), 6L, card(6L, "B"), 7L, card(7L, "C")));

        service.listMyEnrollments(42L);

        verify(courseLookup, times(1)).findSummaries(List.of(7L, 6L, 5L));
        verify(repository, times(1)).findAllByStudentIdOrderByEnrolledAtDescIdDesc(42L);
    }

    @Test
    void listMyEnrollments_withNoEnrollments_returnsEmptyWithoutCourseLookup() {
        given(repository.findAllByStudentIdOrderByEnrolledAtDescIdDesc(42L)).willReturn(List.of());

        assertThat(service.listMyEnrollments(42L)).isEmpty();

        verifyNoInteractions(courseLookup);
    }

    @Test
    void isEnrolled_delegatesToRepository() {
        given(repository.existsByStudentIdAndCourseId(42L, 5L)).willReturn(true);

        assertThat(service.isEnrolled(42L, 5L)).isTrue();
        assertThat(service.isEnrolled(43L, 5L)).isFalse();
    }
}
