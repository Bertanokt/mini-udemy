-- Kullanıcılar: kimlik, rol ve profil bilgisi (ER: users).
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    bio           TEXT,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN'))
);

-- Aynı e-posta büyük/küçük harf farkıyla ikinci kez kaydedilemesin (ali@x.com = ALI@x.com).
CREATE UNIQUE INDEX uk_users_email ON users (lower(email));
