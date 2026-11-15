-- Sınav denemeleri ve cevapları (ER: quiz_attempts, attempt_answers).
-- Denemesi olan sınav, soru ya da şık silinemez (NO ACTION): sonuç geçmişi bozulmasın.
CREATE TABLE quiz_attempts (
    id           BIGSERIAL     PRIMARY KEY,
    quiz_id      BIGINT        NOT NULL REFERENCES quizzes (id),
    student_id   BIGINT        NOT NULL REFERENCES users (id),
    score        NUMERIC(7, 2) NOT NULL,
    max_score    NUMERIC(7, 2) NOT NULL,
    passed       BOOLEAN       NOT NULL,
    submitted_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_quiz_attempts_score CHECK (score >= 0 AND score <= max_score)
);
CREATE INDEX ix_quiz_attempts_quiz_student ON quiz_attempts (quiz_id, student_id, submitted_at DESC);

CREATE TABLE attempt_answers (
    id          BIGSERIAL PRIMARY KEY,
    attempt_id  BIGINT    NOT NULL REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    question_id BIGINT    NOT NULL REFERENCES questions (id),
    option_id   BIGINT    NOT NULL REFERENCES options (id),
    CONSTRAINT uk_attempt_answers UNIQUE (attempt_id, question_id, option_id)
);
