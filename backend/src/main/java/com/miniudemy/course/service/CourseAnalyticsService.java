package com.miniudemy.course.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.auth.service.UserDirectoryService;
import com.miniudemy.course.dto.CourseStatsResponse;
import com.miniudemy.course.dto.CourseStatsResponse.QuizStats;
import com.miniudemy.course.dto.CourseStatsResponse.RatingStats;
import com.miniudemy.course.dto.CourseStudentPage;
import com.miniudemy.course.dto.CourseStudentResponse;
import com.miniudemy.course.dto.QuizBestResult;
import com.miniudemy.course.dto.RosterEntry;
import com.miniudemy.course.mapper.CourseAnalyticsMapper;
import com.miniudemy.course.service.CourseProgressStats.ProgressTotals;
import com.miniudemy.course.service.CourseRoster.RosterPage;

/**
 * Eğitmen paneli: kurs istatistikleri (#133) ve kursiyer listesi (#134). Kayıt, ilerleme, sınav ve puan verisi başka
 * modüllerde durur; bu modül yalnız kendi tanımladığı arayüzleri ({@link EnrollmentCounts}, {@link CourseRoster},
 * {@link CourseProgressStats}, {@link CourseQuizStats}, {@link CourseRatings}) çağırır, onları ilgili modüller uygular.
 *
 * <p>Hesap kuralları (yüzdeler iki ondalığa yuvarlanır, sıfıra bölme yok):
 * <ul>
 *   <li>Ortalama ilerleme = kayıtlıların kurs yüzdelerinin toplamı / kayıtlı sayısı; ilerleme satırı olmayan kayıt
 *       %0 sayılır, kayıtlı yoksa 0.</li>
 *   <li>Tamamlanma oranı = yüzdesi %100 olan kayıtlı / kayıtlı × 100; kayıtlı yoksa 0.</li>
 *   <li>Sınav başarı oranı = geçen deneme / tüm denemeler × 100 (her kursiyerin her denemesi sayılır); deneme yoksa 0.</li>
 * </ul>
 */
@Service
public class CourseAnalyticsService {

    private static final String UNKNOWN_STUDENT = "Kullanıcı";

    private final CourseOwnershipService ownership;
    private final EnrollmentCounts enrollmentCounts;
    private final CourseProgressStats progressStats;
    private final CourseQuizStats quizStats;
    private final CourseRatings ratings;
    private final CourseRoster roster;
    private final UserDirectoryService userDirectory;
    private final CourseAnalyticsMapper mapper;

    public CourseAnalyticsService(CourseOwnershipService ownership, EnrollmentCounts enrollmentCounts,
            CourseProgressStats progressStats, CourseQuizStats quizStats, CourseRatings ratings, CourseRoster roster,
            UserDirectoryService userDirectory, CourseAnalyticsMapper mapper) {
        this.ownership = ownership;
        this.enrollmentCounts = enrollmentCounts;
        this.progressStats = progressStats;
        this.quizStats = quizStats;
        this.ratings = ratings;
        this.roster = roster;
        this.userDirectory = userDirectory;
        this.mapper = mapper;
    }

    /** Kurs yoksa 404, kursun eğitmeni değilse 403. Kurs hangi durumda olursa olsun görülebilir. */
    @Transactional(readOnly = true)
    public CourseStatsResponse getStats(Long courseId, Long instructorId) {
        ownership.assertOwner(courseId, instructorId);
        long enrolled = enrollmentCounts.countByCourse(courseId);
        ProgressTotals totals = progressStats.totals(courseId);
        List<QuizStats> quizzes = quizStats.attemptStats(courseId).stream()
                .map(q -> mapper.toQuizStats(q, percentOf(q.passedCount(), q.attemptCount())))
                .toList();
        return mapper.toStats(courseId, enrolled, average(totals.percentSum(), enrolled),
                percentOf(totals.completedCount(), enrolled), quizzes,
                ratingStats(courseId));
    }

    /**
     * Kursa kayıtlı kursiyerler, en son kaydolan önce. Kurs yoksa 404, kursun eğitmeni değilse 403. Sorgu sayısı sayfa
     * boyutundan bağımsızdır: kayıt sayfası, ilerleme, kullanıcı adları ve sınav sonuçları için birer toplu sorgu.
     */
    @Transactional(readOnly = true)
    public CourseStudentPage listStudents(Long courseId, Long instructorId, int page, int size) {
        ownership.assertOwner(courseId, instructorId);
        RosterPage result = roster.page(courseId, page, size);
        List<Long> studentIds = result.entries().stream().map(RosterEntry::studentId).toList();
        Map<Long, UserSummary> names = userDirectory.findSummaries(studentIds);
        Map<Long, List<QuizBestResult>> quizResults = quizStats.bestResults(courseId, studentIds);
        List<CourseStudentResponse> content = result.entries().stream()
                .map(e -> mapper.toStudent(e,
                        names.getOrDefault(e.studentId(), new UserSummary(e.studentId(), UNKNOWN_STUDENT)),
                        quizResults.getOrDefault(e.studentId(), List.of())))
                .toList();
        return new CourseStudentPage(content, page, size, result.totalElements(), result.totalPages());
    }

    private RatingStats ratingStats(Long courseId) {
        CourseRating rating = ratings.ratingOf(courseId);
        return mapper.toRatingStats(rating.averageRating(), rating.reviewCount(), ratings.distributionOf(courseId));
    }

    /** Sıfıra bölme yok: payda 0 ise 0. */
    static double percentOf(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(part * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Yüzdelerin toplamından ortalama; kayıtlı yoksa 0. */
    static double average(BigDecimal sum, long count) {
        if (count == 0) {
            return 0.0;
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP).doubleValue();
    }
}
