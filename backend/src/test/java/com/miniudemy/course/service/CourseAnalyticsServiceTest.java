package com.miniudemy.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.common.exception.ForbiddenException;
import com.miniudemy.common.exception.NotFoundException;
import com.miniudemy.course.dto.CourseStatsResponse;
import com.miniudemy.course.dto.CourseStatsResponse.QuizStats;
import com.miniudemy.course.dto.CourseStudentPage;
import com.miniudemy.course.dto.CourseStudentResponse.QuizResult;
import com.miniudemy.course.dto.QuizAttemptStats;
import com.miniudemy.course.dto.QuizBestResult;
import com.miniudemy.course.dto.RosterEntry;
import com.miniudemy.course.mapper.CourseAnalyticsMapper;
import com.miniudemy.course.mapper.CourseAnalyticsMapperImpl;
import com.miniudemy.course.service.CourseProgressStats.ProgressTotals;
import com.miniudemy.course.service.CourseRoster.RosterPage;

/** Eğitmen istatistiklerinin hesap kuralları ve sahiplik denetimi; bağımlı modüller taklit. */
class CourseAnalyticsServiceTest {

    private final CourseOwnershipService ownership = mock(CourseOwnershipService.class);
    private final EnrollmentCounts enrollmentCounts = mock(EnrollmentCounts.class);
    private final CourseProgressStats progressStats = mock(CourseProgressStats.class);
    private final CourseQuizStats quizStats = mock(CourseQuizStats.class);
    private final CourseRatings ratings = mock(CourseRatings.class);
    private final CourseRoster roster = mock(CourseRoster.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final CourseAnalyticsMapper mapper = new CourseAnalyticsMapperImpl();
    private final CourseAnalyticsService service = new CourseAnalyticsService(ownership, enrollmentCounts,
            progressStats, quizStats, ratings, roster, userDirectory, mapper);

    // --- getStats ---

    @Test
    void getStats_computesAverageCompletionAndQuizRates() {
        given(enrollmentCounts.countByCourse(5L)).willReturn(3L);
        given(progressStats.totals(5L)).willReturn(new ProgressTotals(new BigDecimal("150.00"), 1));
        given(quizStats.attemptStats(5L)).willReturn(List.of(new QuizAttemptStats(1L, "Giriş sınavı", 3, 1),
                new QuizAttemptStats(2L, "Final", 0, 0)));
        given(ratings.ratingOf(5L)).willReturn(new CourseRating(4.3, 12));
        given(ratings.distributionOf(5L)).willReturn(Map.of("1", 0L, "5", 12L));

        CourseStatsResponse stats = service.getStats(5L, 7L);

        assertThat(stats.courseId()).isEqualTo(5L);
        assertThat(stats.enrollmentCount()).isEqualTo(3);
        assertThat(stats.averageProgressPercent()).isEqualTo(50.0);
        assertThat(stats.completionRate()).isEqualTo(33.33);
        assertThat(stats.quizzes()).containsExactly(new QuizStats(1L, "Giriş sınavı", 3, 33.33),
                new QuizStats(2L, "Final", 0, 0.0));
        assertThat(stats.rating().averageRating()).isEqualTo(4.3);
        assertThat(stats.rating().reviewCount()).isEqualTo(12);
        assertThat(stats.rating().distribution()).containsEntry("5", 12L);
    }

    @Test
    void getStats_courseWithoutEnrollmentsOrQuizzes_isAllZeroWithoutDividingByZero() {
        given(enrollmentCounts.countByCourse(5L)).willReturn(0L);
        given(progressStats.totals(5L)).willReturn(new ProgressTotals(BigDecimal.ZERO, 0));
        given(quizStats.attemptStats(5L)).willReturn(List.of());
        given(ratings.ratingOf(5L)).willReturn(new CourseRating(null, 0));
        given(ratings.distributionOf(5L)).willReturn(Map.of());

        CourseStatsResponse stats = service.getStats(5L, 7L);

        assertThat(stats.enrollmentCount()).isZero();
        assertThat(stats.averageProgressPercent()).isZero();
        assertThat(stats.completionRate()).isZero();
        assertThat(stats.quizzes()).isEmpty();
        assertThat(stats.rating().averageRating()).isNull();
    }

    @Test
    void getStats_enrolledButNoProgressRows_countsThemAsZeroPercent() {
        given(enrollmentCounts.countByCourse(5L)).willReturn(4L);
        given(progressStats.totals(5L)).willReturn(new ProgressTotals(BigDecimal.ZERO, 0));
        given(quizStats.attemptStats(5L)).willReturn(List.of());
        given(ratings.ratingOf(5L)).willReturn(new CourseRating(null, 0));
        given(ratings.distributionOf(5L)).willReturn(Map.of());

        CourseStatsResponse stats = service.getStats(5L, 7L);

        assertThat(stats.enrollmentCount()).isEqualTo(4);
        assertThat(stats.averageProgressPercent()).isZero();
        assertThat(stats.completionRate()).isZero();
    }

    @Test
    void getStats_everyoneCompleted_isHundredPercent() {
        given(enrollmentCounts.countByCourse(5L)).willReturn(2L);
        given(progressStats.totals(5L)).willReturn(new ProgressTotals(new BigDecimal("200.00"), 2));
        given(quizStats.attemptStats(5L)).willReturn(List.of(new QuizAttemptStats(1L, "Sınav", 5, 5)));
        given(ratings.ratingOf(5L)).willReturn(new CourseRating(null, 0));
        given(ratings.distributionOf(5L)).willReturn(Map.of());

        CourseStatsResponse stats = service.getStats(5L, 7L);

        assertThat(stats.averageProgressPercent()).isEqualTo(100.0);
        assertThat(stats.completionRate()).isEqualTo(100.0);
        assertThat(stats.quizzes().get(0).passRate()).isEqualTo(100.0);
    }

    @Test
    void getStats_forAnotherInstructor_throwsForbiddenWithoutReadingData() {
        doThrow(new ForbiddenException("Bu kurs size ait değil.")).when(ownership).assertOwner(5L, 8L);

        assertThatThrownBy(() -> service.getStats(5L, 8L)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(enrollmentCounts, progressStats, quizStats, ratings);
    }

    @Test
    void getStats_whenCourseMissing_throwsNotFound() {
        doThrow(new NotFoundException("Kurs bulunamadı.")).when(ownership).assertOwner(99L, 7L);

        assertThatThrownBy(() -> service.getStats(99L, 7L)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(enrollmentCounts, progressStats, quizStats, ratings);
    }

    // --- listStudents ---

    @Test
    void listStudents_combinesRosterNamesAndBestQuizResults() {
        Instant enrolled = Instant.parse("2026-11-17T10:00:00Z");
        Instant accessed = Instant.parse("2026-11-18T09:00:00Z");
        given(roster.page(5L, 0, 20)).willReturn(new RosterPage(List.of(
                new RosterEntry(11L, 100L, enrolled, 75.0, accessed),
                new RosterEntry(12L, 101L, enrolled, 0.0, null)), 2, 1));
        given(userDirectory.findSummaries(List.of(100L, 101L))).willReturn(Map.of(
                100L, new UserSummary(100L, "Ayşe Yılmaz"), 101L, new UserSummary(101L, "Burak Demir")));
        given(quizStats.bestResults(5L, List.of(100L, 101L))).willReturn(
                Map.of(100L, List.of(new QuizBestResult(1L, 80.5, true))));

        CourseStudentPage page = service.listStudents(5L, 7L, 0, 20);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).student()).isEqualTo(new UserSummary(100L, "Ayşe Yılmaz"));
        assertThat(page.content().get(0).enrolledAt()).isEqualTo(enrolled);
        assertThat(page.content().get(0).progressPercent()).isEqualTo(75.0);
        assertThat(page.content().get(0).lastAccessedAt()).isEqualTo(accessed);
        assertThat(page.content().get(0).quizResults()).containsExactly(new QuizResult(1L, 80.5, true));
        assertThat(page.content().get(1).student().fullName()).isEqualTo("Burak Demir");
        assertThat(page.content().get(1).lastAccessedAt()).isNull();
        assertThat(page.content().get(1).quizResults()).isEmpty();
    }

    @Test
    void listStudents_studentMissingFromDirectory_getsPlaceholderName() {
        given(roster.page(5L, 0, 20)).willReturn(new RosterPage(List.of(
                new RosterEntry(11L, 100L, Instant.parse("2026-11-17T10:00:00Z"), 0.0, null)), 1, 1));
        given(userDirectory.findSummaries(List.of(100L))).willReturn(Map.of());
        given(quizStats.bestResults(5L, List.of(100L))).willReturn(Map.of());

        CourseStudentPage page = service.listStudents(5L, 7L, 0, 20);

        assertThat(page.content().get(0).student()).isEqualTo(new UserSummary(100L, "Kullanıcı"));
    }

    @Test
    void listStudents_courseWithoutEnrollments_returnsEmptyPage() {
        given(roster.page(5L, 0, 20)).willReturn(new RosterPage(List.of(), 0, 0));
        given(userDirectory.findSummaries(List.of())).willReturn(Map.of());
        given(quizStats.bestResults(5L, List.of())).willReturn(Map.of());

        CourseStudentPage page = service.listStudents(5L, 7L, 0, 20);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void listStudents_forAnotherInstructor_throwsForbiddenWithoutReadingData() {
        doThrow(new ForbiddenException("Bu kurs size ait değil.")).when(ownership).assertOwner(5L, 8L);

        assertThatThrownBy(() -> service.listStudents(5L, 8L, 0, 20)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(roster, userDirectory, quizStats);
    }

    @Test
    void listStudents_whenCourseMissing_throwsNotFound() {
        doThrow(new NotFoundException("Kurs bulunamadı.")).when(ownership).assertOwner(99L, 7L);

        assertThatThrownBy(() -> service.listStudents(99L, 7L, 0, 20)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(roster, userDirectory, quizStats);
    }

    // --- hesap yardımcıları ---

    @Test
    void percentOf_roundsToTwoDecimals_andZeroDenominatorIsZero() {
        assertThat(CourseAnalyticsService.percentOf(1, 3)).isEqualTo(33.33);
        assertThat(CourseAnalyticsService.percentOf(2, 3)).isEqualTo(66.67);
        assertThat(CourseAnalyticsService.percentOf(0, 0)).isZero();
        assertThat(CourseAnalyticsService.percentOf(5, 0)).isZero();
    }

    @Test
    void average_roundsToTwoDecimals_andZeroCountIsZero() {
        assertThat(CourseAnalyticsService.average(new BigDecimal("100.00"), 3)).isEqualTo(33.33);
        assertThat(CourseAnalyticsService.average(new BigDecimal("100.00"), 0)).isZero();
    }
}
