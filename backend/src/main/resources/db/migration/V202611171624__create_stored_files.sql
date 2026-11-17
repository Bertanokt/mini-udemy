-- Saklanan dosyaların meta verisi (ER: stored_files; #111, ADR-004).
-- Dosyanın kendisi diskte (Docker volume), sunucunun ürettiği UUID adıyla durur: storage_key.
-- original_name yalnız gösterim / indirme adıdır; diskte yol olarak kullanılmaz.

CREATE TABLE stored_files (
    id            BIGSERIAL    PRIMARY KEY,
    uploader_id   BIGINT       NOT NULL REFERENCES users (id),
    storage_key   VARCHAR(100) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_stored_files_storage_key UNIQUE (storage_key),
    CONSTRAINT ck_stored_files_size CHECK (size_bytes >= 0)
);
CREATE INDEX ix_stored_files_uploader_id ON stored_files (uploader_id);

-- Kurs yapısı migration'ı (V202611131449) bu yabancı anahtarları "dosya tablosu gelince ayrı migration'la"
-- eklemeyi bırakmıştı. Kullanımdaki bir dosya silinemez (varsayılan NO ACTION); dosya değiştirilirken önce kurs/ders
-- yeni dosyayı gösterir, sonra eski dosya silinir (#112).
ALTER TABLE courses
    ADD CONSTRAINT fk_courses_cover_file FOREIGN KEY (cover_file_id) REFERENCES stored_files (id);
ALTER TABLE lessons
    ADD CONSTRAINT fk_lessons_content_file FOREIGN KEY (content_file_id) REFERENCES stored_files (id);
