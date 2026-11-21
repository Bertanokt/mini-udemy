-- Parola yenileme token'ları (ER: password_reset_tokens, #115). Token'ın kendisi saklanmaz, yalnız SHA-256 özeti
-- (64 hex karakter); veritabanı okunsa bile bağlantı kurulamaz. Tek kullanımlık (used_at) ve süreli (expires_at).
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);

-- Yeni istekte kullanıcının eski, kullanılmamış token'ları geçersiz kılınır (user_id ile aranır).
CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id);
