-- İlerleme (ER: lesson_progress, course_progress). İlerleme kişiye değil kayda (enrollment) bağlı.
CREATE TABLE lesson_progress (
    id            BIGSERIAL   PRIMARY KEY,
    enrollment_id BIGINT      NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    lesson_id     BIGINT      NOT NULL REFERENCES lessons (id) ON DELETE CASCADE,
    completed_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_lesson_progress_enrollment_lesson UNIQUE (enrollment_id, lesson_id)
);
CREATE INDEX ix_lesson_progress_lesson_id ON lesson_progress (lesson_id);

-- Kurs yüzdesi önceden hesaplanıp saklanır; ders silinince son izlenen ders boşa düşer.
CREATE TABLE course_progress (
    id               BIGSERIAL     PRIMARY KEY,
    enrollment_id    BIGINT        NOT NULL REFERENCES enrollments (id) ON DELETE CASCADE,
    progress_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    last_lesson_id   BIGINT        REFERENCES lessons (id) ON DELETE SET NULL,
    last_accessed_at TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_course_progress_enrollment UNIQUE (enrollment_id),
    CONSTRAINT ck_course_progress_percent CHECK (progress_percent BETWEEN 0 AND 100)
);
