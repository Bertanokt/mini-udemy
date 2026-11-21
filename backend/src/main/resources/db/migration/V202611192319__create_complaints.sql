-- Kurs ve yorum şikâyetleri (ER: complaints, #144). Şikâyet ya bir kursa ya bir yoruma aittir, ikisine birden değil.
-- Yönetici durumu OPEN'dan REVIEWED ya da REJECTED'a çeker; çözen yönetici ve zaman saklanır.
-- Hedef kurs (Taslak'a dönüp silinebilir) ya da yorum silinirse şikâyeti de silinir.
CREATE TABLE complaints (
    id             BIGSERIAL   PRIMARY KEY,
    reporter_id    BIGINT      NOT NULL REFERENCES users (id),
    course_id      BIGINT      REFERENCES courses (id) ON DELETE CASCADE,
    review_id      BIGINT      REFERENCES reviews (id) ON DELETE CASCADE,
    resolved_by_id BIGINT      REFERENCES users (id),
    reason         TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at    TIMESTAMPTZ,
    CONSTRAINT ck_complaints_status CHECK (status IN ('OPEN', 'REVIEWED', 'REJECTED')),
    CONSTRAINT ck_complaints_single_target CHECK (num_nonnulls(course_id, review_id) = 1)
);
CREATE INDEX ix_complaints_status_created_at ON complaints (status, created_at DESC);
CREATE INDEX ix_complaints_course_id ON complaints (course_id);
CREATE INDEX ix_complaints_review_id ON complaints (review_id);
