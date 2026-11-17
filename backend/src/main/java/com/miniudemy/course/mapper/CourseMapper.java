package com.miniudemy.course.mapper;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.auth.dto.UserSummary;
import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.course.domain.Course;
import com.miniudemy.course.domain.Lesson;
import com.miniudemy.course.domain.Section;
import com.miniudemy.course.dto.CourseDetailResponse;
import com.miniudemy.course.dto.CourseStats;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.course.dto.LessonResponse;
import com.miniudemy.course.dto.SectionResponse;

/**
 * Kurs, bölüm ve ders entity'lerinden katalog DTO'larına dönüşüm (ADR-006). Eğitmen adı ve sayılar başka modüllerden
 * geldiği için hazır parametre olarak verilir; mapper veritabanına ya da başka servise gitmez.
 */
@Mapper(config = MapStructConfig.class, uses = CategoryMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CourseMapper {

    @Mapping(target = "id", source = "course.id")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "category", source = "course.category")
    @Mapping(target = "level", source = "course.level")
    @Mapping(target = "status", source = "course.status")
    @Mapping(target = "coverUrl", source = "course")
    @Mapping(target = "instructor", source = "instructor")
    @Mapping(target = "lessonCount", source = "stats.lessonCount")
    @Mapping(target = "totalDurationSeconds", source = "stats.totalDurationSeconds")
    @Mapping(target = "enrollmentCount", source = "stats.enrollmentCount")
    @Mapping(target = "averageRating", source = "stats.averageRating")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    CourseSummaryResponse toSummary(Course course, UserSummary instructor, CourseStats stats);

    @Mapping(target = "id", source = "course.id")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "category", source = "course.category")
    @Mapping(target = "level", source = "course.level")
    @Mapping(target = "status", source = "course.status")
    @Mapping(target = "coverUrl", source = "course")
    @Mapping(target = "instructor", source = "instructor")
    @Mapping(target = "lessonCount", source = "stats.lessonCount")
    @Mapping(target = "totalDurationSeconds", source = "stats.totalDurationSeconds")
    @Mapping(target = "enrollmentCount", source = "stats.enrollmentCount")
    @Mapping(target = "averageRating", source = "stats.averageRating")
    @Mapping(target = "reviewCount", source = "stats.reviewCount")
    @Mapping(target = "description", source = "course.description")
    @Mapping(target = "learningOutcomes", source = "course.learningOutcomes")
    @Mapping(target = "sections", source = "sections")
    CourseDetailResponse toDetail(Course course, UserSummary instructor, CourseStats stats,
            List<SectionResponse> sections);

    @Mapping(target = "courseId", source = "course.id")
    SectionResponse toSectionResponse(Section section);

    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "hasContent", expression = "java(lesson.getContentFileId() != null)")
    LessonResponse toLessonResponse(Lesson lesson);

    /** Kapak yüklenmişse kapağın adresi, yoksa null (docs/api/openapi.yaml › `GET /api/courses/{id}/cover`). */
    default String coverUrl(Course course) {
        return course.getCoverFileId() == null ? null : "/api/courses/" + course.getId() + "/cover";
    }
}
