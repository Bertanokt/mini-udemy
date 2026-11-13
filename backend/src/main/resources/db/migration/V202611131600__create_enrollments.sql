-- Kursa kayıt (ER: enrollments). Aynı öğrenci aynı kursa bir kez kaydolur.
CREATE TABLE enrollments (
    id          BIGSERIAL   PRIMARY KEY,
    student_id  BIGINT      NOT NULL REFERENCES users (id),
    course_id   BIGINT      NOT NULL REFERENCES courses (id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_enrollments_student_course UNIQUE (student_id, course_id)
);
CREATE INDEX ix_enrollments_course_id ON enrollments (course_id);
