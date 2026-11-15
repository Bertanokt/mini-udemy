-- Soru bankası ve bölüm sınavları (ER: questions, options, quizzes, quiz_questions).
CREATE TABLE questions (
    id         BIGSERIAL   PRIMARY KEY,
    course_id  BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    text       TEXT        NOT NULL,
    points     INT         NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_questions_points CHECK (points > 0)
);
CREATE INDEX ix_questions_course_id ON questions (course_id);

CREATE TABLE options (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT    NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    text        TEXT      NOT NULL,
    is_correct  BOOLEAN   NOT NULL
);
CREATE INDEX ix_options_question_id ON options (question_id);

CREATE TABLE quizzes (
    id             BIGSERIAL    PRIMARY KEY,
    section_id     BIGINT       NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    scoring_type   VARCHAR(30)  NOT NULL,
    pass_threshold INT          NOT NULL,
    required       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_quizzes_scoring_type CHECK (scoring_type IN ('SINGLE_ANSWER', 'MULTI_ALL_OR_NOTHING', 'MULTI_PARTIAL')),
    CONSTRAINT ck_quizzes_pass_threshold CHECK (pass_threshold BETWEEN 0 AND 100)
);
CREATE INDEX ix_quizzes_section_id ON quizzes (section_id);

-- Sınavda kullanılan soru tek başına silinemez. Kontrol işlem sonunda yapılır (DEFERRABLE),
-- böylece taslak kurs silinirken soru ve sınav aynı işlemde birlikte silinebilir.
CREATE TABLE quiz_questions (
    quiz_id     BIGINT NOT NULL REFERENCES quizzes (id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions (id) DEFERRABLE INITIALLY DEFERRED,
    position    INT    NOT NULL,
    PRIMARY KEY (quiz_id, question_id),
    CONSTRAINT ck_quiz_questions_position CHECK (position >= 0)
);
CREATE INDEX ix_quiz_questions_question_id ON quiz_questions (question_id);
