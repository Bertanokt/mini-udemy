package com.miniudemy.enrollment.dto;

/**
 * Bir kaydın kimliği: kayıt id'si, kursiyer ve kurs. Kaydı id ile tutan başka modüller (sertifika, #130) kursiyeri ve
 * kursu buradan öğrenir; entity modül dışına çıkmaz.
 */
public record EnrollmentRef(Long enrollmentId, Long studentId, Long courseId) {
}
