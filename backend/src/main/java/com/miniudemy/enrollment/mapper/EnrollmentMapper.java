package com.miniudemy.enrollment.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.miniudemy.common.mapper.MapStructConfig;
import com.miniudemy.course.dto.CourseSummaryResponse;
import com.miniudemy.enrollment.domain.Enrollment;
import com.miniudemy.enrollment.dto.EnrollmentRef;
import com.miniudemy.enrollment.dto.EnrollmentResponse;

/** {@code Enrollment} -> {@code EnrollmentResponse}. Kurs kartı ve ilerleme başka modüllerden geldiği için hazır verilir. */
@Mapper(config = MapStructConfig.class)
public interface EnrollmentMapper {

    @Mapping(target = "id", source = "enrollment.id")
    @Mapping(target = "enrolledAt", source = "enrollment.enrolledAt")
    @Mapping(target = "course", source = "course")
    @Mapping(target = "progressPercent", source = "progressPercent")
    @Mapping(target = "lastLessonId", source = "lastLessonId")
    @Mapping(target = "lastAccessedAt", source = "lastAccessedAt")
    EnrollmentResponse toResponse(Enrollment enrollment, CourseSummaryResponse course, double progressPercent,
            Long lastLessonId, Instant lastAccessedAt);

    /** Kaydın kimliği (#130). */
    @Mapping(target = "enrollmentId", source = "id")
    EnrollmentRef toRef(Enrollment enrollment);
}
