package com.miniudemy.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.course.dto.OrderedLesson;
import com.miniudemy.course.dto.SectionLessons;
import com.miniudemy.course.service.CourseStructureService;
import com.miniudemy.enrollment.service.EnrollmentService;
import com.miniudemy.progress.domain.CourseProgress;
import com.miniudemy.progress.dto.CourseProgressResponse.SectionProgress;
import com.miniudemy.progress.dto.CourseProgressResponse;
import com.miniudemy.progress.dto.ResumeResponse;
import com.miniudemy.progress.repository.CourseProgressRepository;
import com.miniudemy.progress.repository.LessonProgressRepository;

class ProgressServiceTest {

    private static final long STUDENT = 42L;
    private static final long COURSE = 7L;
    private static final long ENROLLMENT = 9L;

    private static final List<Long> FIRST_SECTION_DONE = ids(101, 110);

    private final CourseStructureService structure = mock(CourseStructureService.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final LessonProgressRepository lessonProgress = mock(LessonProgressRepository.class);
    private final CourseProgressRepository courseProgress = mock(CourseProgressRepository.class);
    private final ProgressService service = new ProgressService(structure, enrollments, lessonProgress,
            courseProgress);

    private static List<Long> ids(long fromInclusive, long toInclusive) {
        return LongStream.rangeClosed(fromInclusive, toInclusive).boxed().toList();
    }

    private void enrolled() {
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willReturn(ENROLLMENT);
    }

    /** İki bölüm: 1. bölüm 10 ders (101-110), 2. bölüm 10 ders (201-210). */
    private void twentyLessons() {
        given(structure.sectionLessons(COURSE)).willReturn(
                List.of(new SectionLessons(1L, ids(101, 110)), new SectionLessons(2L, ids(201, 210))));
    }

    private static List<Long> completed(long secondSectionCount) {
        return java.util.stream.Stream.concat(FIRST_SECTION_DONE.stream(),
                ids(201, 200 + secondSectionCount).stream()).toList();
    }

    @Test
    void getMyProgress_15of20_returns75Percent() {
        enrolled();
        twentyLessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(completed(5));

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.progressPercent()).isEqualTo(75.0);
        assertThat(response.completedLessonIds()).hasSize(15);
    }

    @Test
    void getMyProgress_noLessonCompleted_isZero_forCourseAndEverySection() {
        enrolled();
        twentyLessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of());

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.progressPercent()).isZero();
        assertThat(response.completedLessonIds()).isEmpty();
        assertThat(response.sections()).containsExactly(new SectionProgress(1L, 0.0), new SectionProgress(2L, 0.0));
    }

    @Test
    void getMyProgress_allTwentyCompleted_is100_forCourseAndEverySection() {
        enrolled();
        twentyLessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(completed(10));

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.progressPercent()).isEqualTo(100.0);
        assertThat(response.completedLessonIds()).hasSize(20);
        assertThat(response.sections()).containsExactly(new SectionProgress(1L, 100.0),
                new SectionProgress(2L, 100.0));
    }

    @Test
    void getMyProgress_perSection_usesSameRule() {
        enrolled();
        twentyLessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(completed(3));

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.sections()).containsExactly(new SectionProgress(1L, 100.0),
                new SectionProgress(2L, 30.0));
        assertThat(response.progressPercent()).isEqualTo(65.0);
    }

    @Test
    void getMyProgress_roundsToTwoDecimals() {
        enrolled();
        given(structure.sectionLessons(COURSE)).willReturn(List.of(new SectionLessons(1L, List.of(1L, 2L, 3L))));
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(1L));

        assertThat(service.getMyProgress(STUDENT, COURSE).progressPercent()).isEqualTo(33.33);
    }

    @Test
    void getMyProgress_courseWithoutLessons_returnsZeroWithoutDividingByZero() {
        enrolled();
        given(structure.sectionLessons(COURSE)).willReturn(List.of());
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of());

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.progressPercent()).isZero();
        assertThat(response.sections()).isEmpty();
        assertThat(response.completedLessonIds()).isEmpty();
    }

    @Test
    void getMyProgress_sectionWithoutLessons_isZeroAndDoesNotAffectCourse() {
        enrolled();
        given(structure.sectionLessons(COURSE)).willReturn(
                List.of(new SectionLessons(1L, List.of(1L, 2L)), new SectionLessons(2L, List.of())));
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(1L, 2L));

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.sections()).containsExactly(new SectionProgress(1L, 100.0),
                new SectionProgress(2L, 0.0));
        assertThat(response.progressPercent()).isEqualTo(100.0);
    }

    @Test
    void getMyProgress_ignoresCompletedLessonsNoLongerInCourse() {
        enrolled();
        given(structure.sectionLessons(COURSE)).willReturn(List.of(new SectionLessons(1L, List.of(1L, 2L))));
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(1L, 999L));

        CourseProgressResponse response = service.getMyProgress(STUDENT, COURSE);

        assertThat(response.completedLessonIds()).containsExactly(1L);
        assertThat(response.progressPercent()).isEqualTo(50.0);
    }

    @Test
    void getMyProgress_notEnrolled_throwsForbiddenAndReadsNothing() {
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willThrow(new ForbiddenException("x"));

        assertThatThrownBy(() -> service.getMyProgress(STUDENT, COURSE)).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(structure, lessonProgress);
    }

    @Test
    void refreshCourseProgress_writes75Percent() {
        enrolled();
        twentyLessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(completed(5));

        service.refreshCourseProgress(STUDENT, COURSE);

        verify(courseProgress).upsertPercent(ENROLLMENT, BigDecimal.valueOf(75.0));
    }

    @Test
    void refreshCourseProgress_notEnrolled_throwsForbiddenAndWritesNothing() {
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willThrow(new ForbiddenException("x"));

        assertThatThrownBy(() -> service.refreshCourseProgress(STUDENT, COURSE))
                .isInstanceOf(ForbiddenException.class);

        verify(courseProgress, never()).upsertPercent(anyLong(), any());
    }

    @Test
    void recalculateCourse_afterLessonDeleted_rewritesEveryEnrollment() {
        // Ders 2 silindi: kursta 1, 3, 4 kaldı. Kayıt 9'un tamamladığı 1 ve 2'den yalnız 1 sayılır.
        given(structure.sectionLessons(COURSE)).willReturn(List.of(new SectionLessons(1L, List.of(1L, 3L, 4L))));
        given(enrollments.findEnrollmentIdsByCourse(COURSE)).willReturn(List.of(9L, 10L));
        given(lessonProgress.findLessonIdsByEnrollmentId(9L)).willReturn(List.of(1L, 2L));
        given(lessonProgress.findLessonIdsByEnrollmentId(10L)).willReturn(List.of());

        service.recalculateCourse(COURSE);

        verify(courseProgress).upsertPercent(9L, BigDecimal.valueOf(33.33));
        verify(courseProgress).upsertPercent(10L, BigDecimal.valueOf(0.0));
    }

    @Test
    void recalculateCourse_courseNowEmpty_writesZero() {
        given(structure.sectionLessons(COURSE)).willReturn(List.of());
        given(enrollments.findEnrollmentIdsByCourse(COURSE)).willReturn(List.of(9L));
        given(lessonProgress.findLessonIdsByEnrollmentId(9L)).willReturn(List.of(2L));

        service.recalculateCourse(COURSE);

        verify(courseProgress).upsertPercent(9L, BigDecimal.valueOf(0.0));
    }

    // --- son izlenen ders ve kaldığı yerden devam (#119) ---

    private static final Instant OPENED = Instant.parse("2026-11-18T09:00:00Z");

    private void lessons(OrderedLesson... ordered) {
        given(structure.orderedLessons(COURSE)).willReturn(List.of(ordered));
    }

    private void lastOpened(Long lessonId) {
        CourseProgress row = mock(CourseProgress.class);
        given(row.getLastLessonId()).willReturn(lessonId);
        given(row.getLastAccessedAt()).willReturn(lessonId == null ? null : OPENED);
        given(courseProgress.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.of(row));
    }

    private static final OrderedLesson L1 = new OrderedLesson(11L, 1L, "Kurulum");
    private static final OrderedLesson L2 = new OrderedLesson(12L, 1L, "Merhaba");
    private static final OrderedLesson L3 = new OrderedLesson(31L, 3L, "JWT");

    @Test
    void recordLessonOpened_writesLastLessonForTheEnrollment() {
        enrolled();

        service.recordLessonOpened(STUDENT, COURSE, 12L, OPENED);

        verify(courseProgress).upsertLastLesson(ENROLLMENT, 12L, OPENED);
    }

    @Test
    void recordLessonOpened_notEnrolled_writesNothing() {
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        assertThatThrownBy(() -> service.recordLessonOpened(STUDENT, COURSE, 12L, OPENED))
                .isInstanceOf(ForbiddenException.class);
        verify(courseProgress, never()).upsertLastLesson(any(), any(), any());
    }

    @Test
    void resume_lastOpenedLesson_isReturnedWithPercentAndTime() {
        enrolled();
        lessons(L1, L2, L3);
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(11L));
        lastOpened(31L);

        assertThat(service.getMyResumePoint(STUDENT, COURSE))
                .isEqualTo(new ResumeResponse(COURSE, 31L, "JWT", 3L, 33.33, OPENED));
    }

    @Test
    void resume_nothingOpened_returnsFirstIncompleteLesson() {
        enrolled();
        lessons(L1, L2, L3);
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(11L));
        given(courseProgress.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.empty());

        assertThat(service.getMyResumePoint(STUDENT, COURSE))
                .isEqualTo(new ResumeResponse(COURSE, 12L, "Merhaba", 1L, 33.33, null));
    }

    @Test
    void resume_lastLessonNoLongerInCourse_fallsBackToFirstIncomplete() {
        enrolled();
        lessons(L1, L2);
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of());
        lastOpened(99L);

        assertThat(service.getMyResumePoint(STUDENT, COURSE).lessonId()).isEqualTo(11L);
    }

    @Test
    void resume_allCompletedAndNothingOpened_returnsTheFirstLesson() {
        enrolled();
        lessons(L1, L2);
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of(11L, 12L));
        given(courseProgress.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.empty());

        assertThat(service.getMyResumePoint(STUDENT, COURSE))
                .isEqualTo(new ResumeResponse(COURSE, 11L, "Kurulum", 1L, 100.0, null));
    }

    @Test
    void resume_courseWithoutLessons_hasNoLesson() {
        enrolled();
        lessons();
        given(lessonProgress.findLessonIdsByEnrollmentId(ENROLLMENT)).willReturn(List.of());
        given(courseProgress.findByEnrollmentId(ENROLLMENT)).willReturn(Optional.empty());

        assertThat(service.getMyResumePoint(STUDENT, COURSE))
                .isEqualTo(new ResumeResponse(COURSE, null, null, null, 0.0, null));
    }

    @Test
    void resume_notEnrolled_returns403() {
        given(enrollments.requireEnrollmentId(STUDENT, COURSE)).willThrow(new ForbiddenException("Bu kursa kayıtlı değilsiniz."));

        assertThatThrownBy(() -> service.getMyResumePoint(STUDENT, COURSE)).isInstanceOf(ForbiddenException.class);
    }
}
