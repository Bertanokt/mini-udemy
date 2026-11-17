-- Kurs değerlendirmesi (ER: reviews). Kullanıcı başına kurs başına tek değerlendirme; hidden/moderation_reason
-- yönetici moderasyonu (#142) içindir, gizli yorumlar listede ve ortalamada yer almaz.
CREATE TABLE reviews (
    id                BIGSERIAL   PRIMARY KEY,
    user_id           BIGINT      NOT NULL REFERENCES users (id),
    course_id         BIGINT      NOT NULL REFERENCES courses (id),
    rating            SMALLINT    NOT NULL,
    comment           TEXT,
    hidden            BOOLEAN     NOT NULL DEFAULT FALSE,
    moderation_reason TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_reviews_user_course UNIQUE (user_id, course_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
CREATE INDEX ix_reviews_course_id ON reviews (course_id);
