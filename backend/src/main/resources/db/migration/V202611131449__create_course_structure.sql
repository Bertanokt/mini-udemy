-- Kurs yapısı: kategori, kurs, bölüm, ders (ER: categories, courses, sections, lessons).
-- cover_file_id ve content_file_id stored_files'a işaret eder; o tablo içerik modülüyle
-- geldiğinde yabancı anahtarlar ayrı migration'la eklenecek.

CREATE TABLE categories (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE courses (
    id                BIGSERIAL    PRIMARY KEY,
    instructor_id     BIGINT       NOT NULL REFERENCES users (id),
    category_id       BIGINT       NOT NULL REFERENCES categories (id),
    cover_file_id     BIGINT,
    title             VARCHAR(200) NOT NULL,
    description       TEXT         NOT NULL,
    level             VARCHAR(20)  NOT NULL,
    learning_outcomes TEXT[],
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    rejection_reason  TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_courses_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_courses_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'PUBLISHED', 'UNPUBLISHED'))
);
CREATE INDEX ix_courses_instructor_id ON courses (instructor_id);
CREATE INDEX ix_courses_category_id ON courses (category_id);
CREATE INDEX ix_courses_status ON courses (status);

CREATE TABLE sections (
    id         BIGSERIAL    PRIMARY KEY,
    course_id  BIGINT       NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    position   INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_sections_position CHECK (position >= 0)
);
CREATE INDEX ix_sections_course_id ON sections (course_id);

CREATE TABLE lessons (
    id               BIGSERIAL    PRIMARY KEY,
    section_id       BIGINT       NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
    content_file_id  BIGINT,
    title            VARCHAR(200) NOT NULL,
    content_type     VARCHAR(20)  NOT NULL,
    mandatory        BOOLEAN      NOT NULL DEFAULT TRUE,
    duration_seconds INT          NOT NULL DEFAULT 0,
    position         INT          NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_lessons_content_type CHECK (content_type IN ('VIDEO', 'PDF', 'DOCUMENT')),
    CONSTRAINT ck_lessons_duration CHECK (duration_seconds >= 0),
    CONSTRAINT ck_lessons_position CHECK (position >= 0)
);
CREATE INDEX ix_lessons_section_id ON lessons (section_id);
