package com.miniudemy.course.mapper;

import java.util.List;
import java.util.Map;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.course.dto.CourseStatsResponse;
import com.miniudemy.course.dto.CourseStatsResponse.ProgressBucket;
import com.miniudemy.course.dto.CourseStatsResponse.QuizStats;
import com.miniudemy.course.dto.CourseStatsResponse.RatingStats;
import com.miniudemy.course.dto.CourseStudentResponse;
import com.miniudemy.course.dto.CourseStudentResponse.QuizResult;
import com.miniudemy.course.dto.QuizAttemptStats;
import com.miniudemy.course.dto.QuizBestResult;
import com.miniudemy.course.dto.RosterEntry;

/**
 * Eğitmen istatistik DTO'ları (ADR-006). Oranlar ve toplamlar service'te hesaplanır, hazır parametre olarak verilir;
 * mapper hesap yapmaz, veritabanına ya da başka servise gitmez.
 */
@Mapper(config = MapStructConfig.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CourseAnalyticsMapper {

    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "enrollmentCount", source = "enrollmentCount")
    @Mapping(target = "averageProgressPercent", source = "averageProgressPercent")
    @Mapping(target = "completionRate", source = "completionRate")
    @Mapping(target = "progressDistribution", source = "progressDistribution")
    @Mapping(target = "quizzes", source = "quizzes")
    @Mapping(target = "rating", source = "rating")
    CourseStatsResponse toStats(Long courseId, long enrollmentCount, double averageProgressPercent,
            double completionRate, List<ProgressBucket> progressDistribution, List<QuizStats> quizzes,
            RatingStats rating);

    @Mapping(target = "quizId", source = "stats.quizId")
    @Mapping(target = "title", source = "stats.title")
    @Mapping(target = "attemptCount", source = "stats.attemptCount")
    @Mapping(target = "passRate", source = "passRate")
    QuizStats toQuizStats(QuizAttemptStats stats, double passRate);

    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "reviewCount", source = "reviewCount")
    @Mapping(target = "distribution", source = "distribution")
    RatingStats toRatingStats(Double averageRating, long reviewCount, Map<String, Long> distribution);

    @Mapping(target = "student", source = "student")
    @Mapping(target = "enrolledAt", source = "entry.enrolledAt")
    @Mapping(target = "progressPercent", source = "entry.progressPercent")
    @Mapping(target = "lastAccessedAt", source = "entry.lastAccessedAt")
    @Mapping(target = "quizResults", source = "quizResults")
    CourseStudentResponse toStudent(RosterEntry entry, UserSummary student, List<QuizBestResult> quizResults);

    QuizResult toQuizResult(QuizBestResult result);
}
