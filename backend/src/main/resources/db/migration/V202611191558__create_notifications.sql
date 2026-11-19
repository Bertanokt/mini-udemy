-- Uygulama içi bildirimler (ER: notifications, #124). Domain olaylarını dinleyen notification modülü yazar.
-- read_at boşsa bildirim okunmamıştır. Liste (#125) kullanıcının bildirimlerini en yeniden eskiye getirir.
CREATE TABLE notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(40)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT         NOT NULL,
    link       VARCHAR(255),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_notifications_type CHECK (type IN ('ENROLLMENT_CREATED', 'LESSON_PUBLISHED', 'COURSE_APPROVED',
                                                     'COURSE_REJECTED', 'QUIZ_COMPLETED', 'CERTIFICATE_ISSUED'))
);

CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC);
