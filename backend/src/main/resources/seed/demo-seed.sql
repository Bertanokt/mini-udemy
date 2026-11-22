-- Demo ve manuel test verisi (#67). Yalnız dev profilinde, veritabanı boşken DemoSeedRunner çalıştırır.
-- BU DOSYA ÜRETİLİR: scripts/seed-content/generate.mjs (içerik: icerik.mjs, kullanıcı verisi: kullanici-verisi.sql).
-- Elle değiştirmeyin; kaynağı değiştirip betiği yeniden çalıştırın.
-- Bütün hesapların parolası README'de; {{PASSWORD_HASH}} çalışma anında BCrypt özetiyle değiştirilir.
-- Kimlikler sabit yazılmaz; satırlar e-posta, başlık ve depolama anahtarıyla bulunur (sıralar ilerler).
-- Dosyaların kendisi seed/files/<storage_key> altında; DemoSeedRunner depolama dizinine kopyalar ve boyutlarını yazar.

-- ---------------------------------------------------------------------------
-- Kullanıcılar: 1 yönetici, 3 eğitmen, 6 kursiyer
-- ---------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, bio, role) VALUES
    ('admin@miniudemy.dev', '{{PASSWORD_HASH}}', 'Ali Yönetici', NULL, 'ADMIN'),
    ('mehmet.kaya@miniudemy.dev', '{{PASSWORD_HASH}}', 'Mehmet Kaya', 'Python, veri analizi ve veri tabanları eğitmeni.', 'INSTRUCTOR'),
    ('zeynep.ak@miniudemy.dev', '{{PASSWORD_HASH}}', 'Zeynep Ak', 'Java, web geliştirme ve arayüz tasarımı eğitmeni.', 'INSTRUCTOR'),
    ('ece.yildiz@miniudemy.dev', '{{PASSWORD_HASH}}', 'Ece Yıldız', 'İş İngilizcesi ve çevik proje yönetimi eğitmeni.', 'INSTRUCTOR'),
    ('ayse.yilmaz@miniudemy.dev', '{{PASSWORD_HASH}}', 'Ayşe Yılmaz', NULL, 'STUDENT'),
    ('can.demir@miniudemy.dev', '{{PASSWORD_HASH}}', 'Can Demir', NULL, 'STUDENT'),
    ('elif.sahin@miniudemy.dev', '{{PASSWORD_HASH}}', 'Elif Şahin', NULL, 'STUDENT'),
    ('burak.celik@miniudemy.dev', '{{PASSWORD_HASH}}', 'Burak Çelik', NULL, 'STUDENT'),
    ('deniz.arslan@miniudemy.dev', '{{PASSWORD_HASH}}', 'Deniz Arslan', NULL, 'STUDENT'),
    ('selin.koc@miniudemy.dev', '{{PASSWORD_HASH}}', 'Selin Koç', NULL, 'STUDENT');

-- ---------------------------------------------------------------------------
-- Dosyalar: 103 (size_bytes'ı DemoSeedRunner dosyayı kopyalarken gerçek boyutla günceller)
-- ---------------------------------------------------------------------------
INSERT INTO stored_files (uploader_id, storage_key, original_name, mime_type, size_bytes)
SELECT u.id, f.storage_key, f.original_name, f.mime_type, 0
FROM (VALUES
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000001', 'kapak-sifirdan-python.png', 'image/png'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000002', 'sifirdan-python-python-nedir.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000003', 'sifirdan-python-kurulum.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000004', 'sifirdan-python-ilk-program.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000005', 'sifirdan-python-sayilar-ve-metinler.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000006', 'sifirdan-python-listeler-ve-sozlukler.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000007', 'sifirdan-python-kosullar-ve-donguler.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000008', 'sifirdan-python-fonksiyon-tanimlama.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000009', 'sifirdan-python-parametreler-ve-donus-degeri.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000010', 'sifirdan-python-moduller.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000011', 'sifirdan-python-not-ortalamasi-hesaplayici.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000012', 'sifirdan-python-alistirmalar.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000013', 'sifirdan-python-sonraki-adimlar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000014', 'kapak-java-ile-nesne-yonelimli-programlama.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000015', 'java-ile-nesne-yonelimli-programlama-sinif-nedir.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000016', 'java-ile-nesne-yonelimli-programlama-alanlar-ve-metotlar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000017', 'java-ile-nesne-yonelimli-programlama-kapsulleme.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000018', 'java-ile-nesne-yonelimli-programlama-kalitim.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000019', 'java-ile-nesne-yonelimli-programlama-soyut-siniflar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000020', 'java-ile-nesne-yonelimli-programlama-arayuzler.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000021', 'java-ile-nesne-yonelimli-programlama-cok-bicimlilik.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000022', 'java-ile-nesne-yonelimli-programlama-kutuphane-uygulamasi.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000023', 'java-ile-nesne-yonelimli-programlama-sik-yapilan-hatalar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000024', 'java-ile-nesne-yonelimli-programlama-proje-odevi.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000025', 'kapak-pandas-ile-veri-analizine-giris.png', 'image/png'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000026', 'pandas-ile-veri-analizine-giris-dataframe-nedir.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000027', 'pandas-ile-veri-analizine-giris-veriyi-okumak.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000028', 'pandas-ile-veri-analizine-giris-sutun-secme-ve-suzme.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000029', 'pandas-ile-veri-analizine-giris-eksik-veriler.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000030', 'pandas-ile-veri-analizine-giris-tipleri-duzeltmek.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000031', 'pandas-ile-veri-analizine-giris-yeni-sutunlar.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000032', 'pandas-ile-veri-analizine-giris-groupby-ile-ozet.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000033', 'pandas-ile-veri-analizine-giris-grafik-cizmek.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000034', 'pandas-ile-veri-analizine-giris-analiz-projesi.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000035', 'kapak-arayuz-tasariminin-temelleri.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000036', 'arayuz-tasariminin-temelleri-gorsel-hiyerarsi.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000037', 'arayuz-tasariminin-temelleri-bosluk-ve-hizalama.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000038', 'arayuz-tasariminin-temelleri-tipografi.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000039', 'arayuz-tasariminin-temelleri-renk-paleti.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000040', 'arayuz-tasariminin-temelleri-kontrast-orani.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000041', 'arayuz-tasariminin-temelleri-erisilebilir-formlar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000042', 'arayuz-tasariminin-temelleri-tasarim-token-lari.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000043', 'arayuz-tasariminin-temelleri-bilesenler.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000044', 'arayuz-tasariminin-temelleri-tasarim-odevi.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000045', 'kapak-is-ingilizcesi-e-posta-ve-toplantilar.png', 'image/png'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000046', 'is-ingilizcesi-e-posta-ve-toplantilar-e-postanin-yapisi.mp4', 'video/mp4'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000047', 'is-ingilizcesi-e-posta-ve-toplantilar-rica-ve-tesekkur-kaliplari.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000048', 'is-ingilizcesi-e-posta-ve-toplantilar-sik-yapilan-hatalar.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000049', 'is-ingilizcesi-e-posta-ve-toplantilar-toplantiyi-acmak.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000050', 'is-ingilizcesi-e-posta-ve-toplantilar-fikir-bildirmek.mp4', 'video/mp4'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000051', 'is-ingilizcesi-e-posta-ve-toplantilar-toplantiyi-kapatmak.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000052', 'is-ingilizcesi-e-posta-ve-toplantilar-sunumun-yapisi.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000053', 'is-ingilizcesi-e-posta-ve-toplantilar-sorulari-yanitlamak.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000054', 'is-ingilizcesi-e-posta-ve-toplantilar-pratik-calisma-k-gidi.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000055', 'kapak-scrum-ile-cevik-proje-yonetimi.png', 'image/png'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000056', 'scrum-ile-cevik-proje-yonetimi-cevik-nedir.mp4', 'video/mp4'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000057', 'scrum-ile-cevik-proje-yonetimi-selale-ile-karsilastirma.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000058', 'scrum-ile-cevik-proje-yonetimi-scrum-rolleri.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000059', 'scrum-ile-cevik-proje-yonetimi-urun-is-listesi.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000060', 'scrum-ile-cevik-proje-yonetimi-sprint-planlama.mp4', 'video/mp4'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000061', 'scrum-ile-cevik-proje-yonetimi-gunluk-scrum-ve-inceleme.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000062', 'scrum-ile-cevik-proje-yonetimi-kanban-panosu.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000063', 'scrum-ile-cevik-proje-yonetimi-retrospektif.pdf', 'application/pdf'),
    ('ece.yildiz@miniudemy.dev', '5eed0000-0000-4000-8000-000000000064', 'scrum-ile-cevik-proje-yonetimi-sprint-planlama-sablonu.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000065', 'kapak-git-ile-surum-kontrolu.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000066', 'git-ile-surum-kontrolu-git-neden-gerekli.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000067', 'git-ile-surum-kontrolu-ilk-commit.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000068', 'git-ile-surum-kontrolu-iyi-commit-mesaji.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000069', 'git-ile-surum-kontrolu-branch-acmak.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000070', 'git-ile-surum-kontrolu-merge-ve-cakisma.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000071', 'git-ile-surum-kontrolu-geri-almak.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000072', 'git-ile-surum-kontrolu-uzak-depo.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000073', 'git-ile-surum-kontrolu-pull-request-akisi.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000074', 'git-ile-surum-kontrolu-git-alistirmalari.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000075', 'kapak-sql-ve-veritabanlari.png', 'image/png'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000076', 'sql-ve-veritabanlari-iliskisel-veritabani.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000077', 'sql-ve-veritabanlari-select.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000078', 'sql-ve-veritabanlari-veri-eklemek-ve-guncellemek.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000079', 'sql-ve-veritabanlari-toplama-fonksiyonlari.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000080', 'sql-ve-veritabanlari-group-by.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000081', 'sql-ve-veritabanlari-having.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000082', 'sql-ve-veritabanlari-inner-join.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000083', 'sql-ve-veritabanlari-left-join.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000084', 'sql-ve-veritabanlari-sql-alistirmalari.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000085', 'kapak-web-gelistirmeye-giris.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000086', 'web-gelistirmeye-giris-etiketler.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000087', 'web-gelistirmeye-giris-sayfa-iskeleti.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000088', 'web-gelistirmeye-giris-formlar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000089', 'web-gelistirmeye-giris-seciciler.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000090', 'web-gelistirmeye-giris-kutu-modeli.mp4', 'video/mp4'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000091', 'web-gelistirmeye-giris-flexbox.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000092', 'web-gelistirmeye-giris-degiskenler-ve-fonksiyonlar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000093', 'web-gelistirmeye-giris-olaylar.pdf', 'application/pdf'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000094', 'web-gelistirmeye-giris-mini-proje.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000095', 'python-2-ile-programlama-eski-print-ifadesi.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000096', 'python-2-ile-programlama-eski-kurulum-python-2-7.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000097', 'python-2-ile-programlama-eski-raw-input.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000098', 'python-2-ile-programlama-eski-tam-sayi-bolmesi.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000099', 'python-2-ile-programlama-eski-unicode.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000100', 'python-2-ile-programlama-eski-2to3-araci.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000101', 'python-2-ile-programlama-eski-neden-python-3.mp4', 'video/mp4'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000102', 'python-2-ile-programlama-eski-tasima-kontrol-listesi.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000103', 'python-2-ile-programlama-eski-tasima-alistirmasi.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
) AS f (email, storage_key, original_name, mime_type)
JOIN users u ON u.email = f.email;

-- ---------------------------------------------------------------------------
-- Kurslar: 10 (her durumdan en az bir tane)
-- ---------------------------------------------------------------------------
INSERT INTO courses (instructor_id, category_id, cover_file_id, title, description, level, learning_outcomes, status,
                     rejection_reason, unpublish_reason)
SELECT u.id, c.id, sf.id, x.title, x.description, x.level, x.outcomes, x.status, x.rejection_reason, x.unpublish_reason
FROM (VALUES
    ('mehmet.kaya@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000001', 'Sıfırdan Python', 'Hiç programlama bilmeyenler için Python: kurulumdan veri tiplerine, koşul ve döngülerden fonksiyonlara. Kurs, küçük bir not ortalaması hesaplayıcısıyla biter.', 'BEGINNER', ARRAY['Python kurup ilk programını çalıştırır', 'Temel veri tiplerini ve listeleri kullanır', 'Koşul ve döngülerle akış kurar', 'Fonksiyon yazar ve modül kullanır', 'Küçük bir komut satırı programı geliştirir'], 'PUBLISHED', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000014', 'Java ile Nesne Yönelimli Programlama', 'Sınıflar, nesneler, kapsülleme, kalıtım ve arayüzlerle Java''da nesne yönelimli tasarım. Her bölüm küçük bir kütüphane uygulamasına yeni bir parça ekler.', 'INTERMEDIATE', ARRAY['Sınıf ve nesne tasarlar', 'Kapsüllemeyle nesnenin durumunu korur', 'Kalıtım ve arayüz kullanır', 'Çok biçimliliği gerçek bir örnekte uygular'], 'PUBLISHED', NULL, NULL),
    ('mehmet.kaya@miniudemy.dev', 'Veri Bilimi', '5eed0000-0000-4000-8000-000000000025', 'Pandas ile Veri Analizine Giriş', 'CSV dosyalarını okuyup temizlemekten gruplayıp özetlemeye kadar Pandas ile veri analizinin temel adımları. Örnek veri seti: bir kitabevinin bir yıllık satışları.', 'INTERMEDIATE', ARRAY['CSV dosyasını DataFrame olarak okur', 'Eksik ve hatalı verileri temizler', 'Satırları süzer ve sıralar', 'groupby ile özet tablolar çıkarır', 'Sonuçları grafikle gösterir'], 'PUBLISHED', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Tasarım', '5eed0000-0000-4000-8000-000000000035', 'Arayüz Tasarımının Temelleri', 'İyi bir arayüzü iyi yapan şeyler: hiyerarşi, boşluk, tipografi, renk ve erişilebilirlik. Kurs boyunca bir kurs platformunun katalog sayfasını adım adım tasarlıyoruz.', 'BEGINNER', ARRAY['Görsel hiyerarşi kurar', 'Tutarlı bir boşluk ölçeği kullanır', 'Okunaklı bir tipografi seçer', 'Kontrast oranını hesaplar ve erişilebilir renk seçer', 'Basit bir tasarım sistemi oluşturur'], 'PUBLISHED', NULL, NULL),
    ('ece.yildiz@miniudemy.dev', 'Dil', '5eed0000-0000-4000-8000-000000000045', 'İş İngilizcesi: E-posta ve Toplantılar', 'İş hayatında en çok ihtiyaç duyulan İngilizce: açık ve kibar e-postalar yazmak, toplantıya katılmak ve kısa bir sunum yapmak. Her derste hazır kalıplar ve örnek cümleler var.', 'INTERMEDIATE', ARRAY['Resmî ve yarı resmî e-posta yazar', 'Toplantıda söz alır ve fikir belirtir', 'Kibar rica ve öneri kalıplarını kullanır', 'Kısa bir sunumu yapılandırır'], 'PUBLISHED', NULL, NULL),
    ('ece.yildiz@miniudemy.dev', 'İş', '5eed0000-0000-4000-8000-000000000055', 'Scrum ile Çevik Proje Yönetimi', 'Scrum''ın rolleri, etkinlikleri ve çıktıları; Kanban panosuyla işi görünür kılmak. Örnek bir ekip üzerinden bir sprinti baştan sona planlıyoruz.', 'BEGINNER', ARRAY['Scrum rollerini ve sorumluluklarını açıklar', 'Ürün iş listesini önceliklendirir', 'Sprint planlaması yapar', 'Kanban panosu kurar', 'Retrospektif yönetir'], 'PUBLISHED', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000065', 'Git ile Sürüm Kontrolü', 'Kodunu kaybetmeden değiştirmek ve ekiple birlikte çalışmak için Git: commit, branch, merge, uzak depo ve pull request akışı.', 'BEGINNER', ARRAY['Bir depo oluşturup commit atar', 'Branch açıp birleştirir', 'Çakışmaları çözer', 'Uzak depoyla çalışır ve pull request açar'], 'PUBLISHED', NULL, NULL),
    ('mehmet.kaya@miniudemy.dev', 'Veri Bilimi', '5eed0000-0000-4000-8000-000000000075', 'SQL ve Veritabanları', 'SELECT''ten JOIN''e ilişkisel veritabanlarıyla çalışma: tablo tasarımı, sorgular, gruplama ve tabloları birleştirme.', 'BEGINNER', ARRAY['SELECT ile veri sorgular', 'WHERE ve ORDER BY ile süzer ve sıralar', 'GROUP BY ile özet çıkarır', 'JOIN ile tabloları birleştirir'], 'IN_REVIEW', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000085', 'Web Geliştirmeye Giriş', 'HTML, CSS ve biraz JavaScript ile ilk web sayfanı yap.', 'BEGINNER', ARRAY['HTML sayfası hazırlar', 'CSS ile biçimlendirir', 'Basit etkileşim ekler', 'Sayfayı mobil uyumlu yapar'], 'DRAFT', 'Bölümlerde örnek proje eksik. Her bölüme kısa bir uygulama ekleyin.', NULL),
    ('mehmet.kaya@miniudemy.dev', 'Yazılım', NULL, 'Python 2 ile Programlama (eski)', 'Python 2 için hazırlanmış eski kurs.', 'BEGINNER', ARRAY['Python 2 sözdizimini tanır', 'print ifadesini kullanır', 'Python 3''e geçişteki farkları bilir', 'Basit betikler yazar'], 'UNPUBLISHED', NULL, 'Python 2 artık desteklenmiyor. Yerine Sıfırdan Python kursunu önerin.')
) AS x (email, category, cover_key, title, description, level, outcomes, status, rejection_reason, unpublish_reason)
JOIN users u ON u.email = x.email
JOIN categories c ON c.name = x.category
LEFT JOIN stored_files sf ON sf.storage_key = x.cover_key;

-- ---------------------------------------------------------------------------
-- Bölümler ve dersler (videoların süresi gerçek video süresi; PDF ve dokümanda okuma süresi)
-- ---------------------------------------------------------------------------
INSERT INTO sections (course_id, title, position)
SELECT co.id, s.title, s.position
FROM (VALUES
    ('Sıfırdan Python', 'Giriş', 0),
    ('Sıfırdan Python', 'Veri Tipleri', 1),
    ('Sıfırdan Python', 'Fonksiyonlar', 2),
    ('Sıfırdan Python', 'Mini Proje', 3),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 0),
    ('Java ile Nesne Yönelimli Programlama', 'Kalıtım ve Arayüzler', 1),
    ('Java ile Nesne Yönelimli Programlama', 'Çok Biçimlilik ve Uygulama', 2),
    ('Pandas ile Veri Analizine Giriş', 'Pandas''a Giriş', 0),
    ('Pandas ile Veri Analizine Giriş', 'Veri Temizleme', 1),
    ('Pandas ile Veri Analizine Giriş', 'Gruplama ve Görselleştirme', 2),
    ('Arayüz Tasarımının Temelleri', 'Temel İlkeler', 0),
    ('Arayüz Tasarımının Temelleri', 'Renk ve Erişilebilirlik', 1),
    ('Arayüz Tasarımının Temelleri', 'Tasarım Sistemi', 2),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-posta Yazmak', 0),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Toplantılar', 1),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Kısa Sunum', 2),
    ('Scrum ile Çevik Proje Yönetimi', 'Çevik Yaklaşım', 0),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint', 1),
    ('Scrum ile Çevik Proje Yönetimi', 'Kanban ve İyileştirme', 2),
    ('Git ile Sürüm Kontrolü', 'Temeller', 0),
    ('Git ile Sürüm Kontrolü', 'Branch ve Merge', 1),
    ('Git ile Sürüm Kontrolü', 'Ekiple Çalışmak', 2),
    ('SQL ve Veritabanları', 'İlk Sorgular', 0),
    ('SQL ve Veritabanları', 'Gruplama', 1),
    ('SQL ve Veritabanları', 'Birleştirmeler', 2),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 0),
    ('Web Geliştirmeye Giriş', 'CSS ile Biçimlendirme', 1),
    ('Web Geliştirmeye Giriş', 'Biraz JavaScript', 2),
    ('Python 2 ile Programlama (eski)', 'Giriş', 0),
    ('Python 2 ile Programlama (eski)', 'Farklar', 1),
    ('Python 2 ile Programlama (eski)', 'Kapanış', 2)
) AS s (course_title, title, position)
JOIN courses co ON co.title = s.course_title;

INSERT INTO lessons (section_id, content_file_id, title, content_type, mandatory, duration_seconds, position)
SELECT se.id, sf.id, l.title, l.content_type, l.mandatory, l.duration, l.position
FROM (VALUES
    ('Sıfırdan Python', 'Giriş', 'Python Nedir?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000002'),
    ('Sıfırdan Python', 'Giriş', 'Kurulum', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000003'),
    ('Sıfırdan Python', 'Giriş', 'İlk Program', 'VIDEO', TRUE, 35, 2, '5eed0000-0000-4000-8000-000000000004'),
    ('Sıfırdan Python', 'Veri Tipleri', 'Sayılar ve Metinler', 'PDF', TRUE, 480, 0, '5eed0000-0000-4000-8000-000000000005'),
    ('Sıfırdan Python', 'Veri Tipleri', 'Listeler ve Sözlükler', 'PDF', TRUE, 540, 1, '5eed0000-0000-4000-8000-000000000006'),
    ('Sıfırdan Python', 'Veri Tipleri', 'Koşullar ve Döngüler', 'PDF', TRUE, 600, 2, '5eed0000-0000-4000-8000-000000000007'),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Fonksiyon Tanımlama', 'PDF', TRUE, 480, 0, '5eed0000-0000-4000-8000-000000000008'),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Parametreler ve Dönüş Değeri', 'PDF', TRUE, 480, 1, '5eed0000-0000-4000-8000-000000000009'),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Modüller', 'PDF', TRUE, 420, 2, '5eed0000-0000-4000-8000-000000000010'),
    ('Sıfırdan Python', 'Mini Proje', 'Not Ortalaması Hesaplayıcı', 'PDF', TRUE, 600, 0, '5eed0000-0000-4000-8000-000000000011'),
    ('Sıfırdan Python', 'Mini Proje', 'Alıştırmalar', 'DOCUMENT', FALSE, 900, 1, '5eed0000-0000-4000-8000-000000000012'),
    ('Sıfırdan Python', 'Mini Proje', 'Sonraki Adımlar', 'PDF', TRUE, 300, 2, '5eed0000-0000-4000-8000-000000000013'),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 'Sınıf Nedir?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000015'),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 'Alanlar ve Metotlar', 'PDF', TRUE, 480, 1, '5eed0000-0000-4000-8000-000000000016'),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 'Kapsülleme', 'PDF', TRUE, 420, 2, '5eed0000-0000-4000-8000-000000000017'),
    ('Java ile Nesne Yönelimli Programlama', 'Kalıtım ve Arayüzler', 'Kalıtım', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000018'),
    ('Java ile Nesne Yönelimli Programlama', 'Kalıtım ve Arayüzler', 'Soyut Sınıflar', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000019'),
    ('Java ile Nesne Yönelimli Programlama', 'Kalıtım ve Arayüzler', 'Arayüzler', 'PDF', TRUE, 480, 2, '5eed0000-0000-4000-8000-000000000020'),
    ('Java ile Nesne Yönelimli Programlama', 'Çok Biçimlilik ve Uygulama', 'Çok Biçimlilik', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000021'),
    ('Java ile Nesne Yönelimli Programlama', 'Çok Biçimlilik ve Uygulama', 'Kütüphane Uygulaması', 'PDF', TRUE, 720, 1, '5eed0000-0000-4000-8000-000000000022'),
    ('Java ile Nesne Yönelimli Programlama', 'Çok Biçimlilik ve Uygulama', 'Sık Yapılan Hatalar', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000023'),
    ('Java ile Nesne Yönelimli Programlama', 'Çok Biçimlilik ve Uygulama', 'Proje Ödevi', 'DOCUMENT', FALSE, 1200, 3, '5eed0000-0000-4000-8000-000000000024'),
    ('Pandas ile Veri Analizine Giriş', 'Pandas''a Giriş', 'DataFrame Nedir?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000026'),
    ('Pandas ile Veri Analizine Giriş', 'Pandas''a Giriş', 'Veriyi Okumak', 'PDF', TRUE, 480, 1, '5eed0000-0000-4000-8000-000000000027'),
    ('Pandas ile Veri Analizine Giriş', 'Pandas''a Giriş', 'Sütun Seçme ve Süzme', 'PDF', TRUE, 480, 2, '5eed0000-0000-4000-8000-000000000028'),
    ('Pandas ile Veri Analizine Giriş', 'Veri Temizleme', 'Eksik Veriler', 'PDF', TRUE, 540, 0, '5eed0000-0000-4000-8000-000000000029'),
    ('Pandas ile Veri Analizine Giriş', 'Veri Temizleme', 'Tipleri Düzeltmek', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000030'),
    ('Pandas ile Veri Analizine Giriş', 'Veri Temizleme', 'Yeni Sütunlar', 'PDF', TRUE, 420, 2, '5eed0000-0000-4000-8000-000000000031'),
    ('Pandas ile Veri Analizine Giriş', 'Gruplama ve Görselleştirme', 'groupby ile Özet', 'PDF', TRUE, 540, 0, '5eed0000-0000-4000-8000-000000000032'),
    ('Pandas ile Veri Analizine Giriş', 'Gruplama ve Görselleştirme', 'Grafik Çizmek', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000033'),
    ('Pandas ile Veri Analizine Giriş', 'Gruplama ve Görselleştirme', 'Analiz Projesi', 'DOCUMENT', FALSE, 1500, 2, '5eed0000-0000-4000-8000-000000000034'),
    ('Arayüz Tasarımının Temelleri', 'Temel İlkeler', 'Görsel Hiyerarşi', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000036'),
    ('Arayüz Tasarımının Temelleri', 'Temel İlkeler', 'Boşluk ve Hizalama', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000037'),
    ('Arayüz Tasarımının Temelleri', 'Temel İlkeler', 'Tipografi', 'PDF', TRUE, 480, 2, '5eed0000-0000-4000-8000-000000000038'),
    ('Arayüz Tasarımının Temelleri', 'Renk ve Erişilebilirlik', 'Renk Paleti', 'PDF', TRUE, 420, 0, '5eed0000-0000-4000-8000-000000000039'),
    ('Arayüz Tasarımının Temelleri', 'Renk ve Erişilebilirlik', 'Kontrast Oranı', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000040'),
    ('Arayüz Tasarımının Temelleri', 'Renk ve Erişilebilirlik', 'Erişilebilir Formlar', 'PDF', TRUE, 480, 2, '5eed0000-0000-4000-8000-000000000041'),
    ('Arayüz Tasarımının Temelleri', 'Tasarım Sistemi', 'Tasarım Token''ları', 'PDF', TRUE, 420, 0, '5eed0000-0000-4000-8000-000000000042'),
    ('Arayüz Tasarımının Temelleri', 'Tasarım Sistemi', 'Bileşenler', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000043'),
    ('Arayüz Tasarımının Temelleri', 'Tasarım Sistemi', 'Tasarım Ödevi', 'DOCUMENT', FALSE, 1200, 2, '5eed0000-0000-4000-8000-000000000044'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-posta Yazmak', 'E-postanın Yapısı', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000046'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-posta Yazmak', 'Rica ve Teşekkür Kalıpları', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000047'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-posta Yazmak', 'Sık Yapılan Hatalar', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000048'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Toplantılar', 'Toplantıyı Açmak', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000049'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Toplantılar', 'Fikir Bildirmek', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000050'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Toplantılar', 'Toplantıyı Kapatmak', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000051'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Kısa Sunum', 'Sunumun Yapısı', 'PDF', TRUE, 420, 0, '5eed0000-0000-4000-8000-000000000052'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Kısa Sunum', 'Soruları Yanıtlamak', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000053'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Kısa Sunum', 'Pratik Çalışma Kâğıdı', 'DOCUMENT', FALSE, 900, 2, '5eed0000-0000-4000-8000-000000000054'),
    ('Scrum ile Çevik Proje Yönetimi', 'Çevik Yaklaşım', 'Çevik Nedir?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000056'),
    ('Scrum ile Çevik Proje Yönetimi', 'Çevik Yaklaşım', 'Şelale ile Karşılaştırma', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000057'),
    ('Scrum ile Çevik Proje Yönetimi', 'Çevik Yaklaşım', 'Scrum Rolleri', 'PDF', TRUE, 420, 2, '5eed0000-0000-4000-8000-000000000058'),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint', 'Ürün İş Listesi', 'PDF', TRUE, 420, 0, '5eed0000-0000-4000-8000-000000000059'),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint', 'Sprint Planlama', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000060'),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint', 'Günlük Scrum ve İnceleme', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000061'),
    ('Scrum ile Çevik Proje Yönetimi', 'Kanban ve İyileştirme', 'Kanban Panosu', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000062'),
    ('Scrum ile Çevik Proje Yönetimi', 'Kanban ve İyileştirme', 'Retrospektif', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000063'),
    ('Scrum ile Çevik Proje Yönetimi', 'Kanban ve İyileştirme', 'Sprint Planlama Şablonu', 'DOCUMENT', FALSE, 900, 2, '5eed0000-0000-4000-8000-000000000064'),
    ('Git ile Sürüm Kontrolü', 'Temeller', 'Git Neden Gerekli?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000066'),
    ('Git ile Sürüm Kontrolü', 'Temeller', 'İlk Commit', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000067'),
    ('Git ile Sürüm Kontrolü', 'Temeller', 'İyi Commit Mesajı', 'PDF', TRUE, 300, 2, '5eed0000-0000-4000-8000-000000000068'),
    ('Git ile Sürüm Kontrolü', 'Branch ve Merge', 'Branch Açmak', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000069'),
    ('Git ile Sürüm Kontrolü', 'Branch ve Merge', 'Merge ve Çakışma', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000070'),
    ('Git ile Sürüm Kontrolü', 'Branch ve Merge', 'Geri Almak', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000071'),
    ('Git ile Sürüm Kontrolü', 'Ekiple Çalışmak', 'Uzak Depo', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000072'),
    ('Git ile Sürüm Kontrolü', 'Ekiple Çalışmak', 'Pull Request Akışı', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000073'),
    ('Git ile Sürüm Kontrolü', 'Ekiple Çalışmak', 'Git Alıştırmaları', 'DOCUMENT', FALSE, 900, 2, '5eed0000-0000-4000-8000-000000000074'),
    ('SQL ve Veritabanları', 'İlk Sorgular', 'İlişkisel Veritabanı', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000076'),
    ('SQL ve Veritabanları', 'İlk Sorgular', 'SELECT', 'PDF', TRUE, 420, 1, '5eed0000-0000-4000-8000-000000000077'),
    ('SQL ve Veritabanları', 'İlk Sorgular', 'Veri Eklemek ve Güncellemek', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000078'),
    ('SQL ve Veritabanları', 'Gruplama', 'Toplama Fonksiyonları', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000079'),
    ('SQL ve Veritabanları', 'Gruplama', 'GROUP BY', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000080'),
    ('SQL ve Veritabanları', 'Gruplama', 'HAVING', 'PDF', TRUE, 300, 2, '5eed0000-0000-4000-8000-000000000081'),
    ('SQL ve Veritabanları', 'Birleştirmeler', 'INNER JOIN', 'PDF', TRUE, 420, 0, '5eed0000-0000-4000-8000-000000000082'),
    ('SQL ve Veritabanları', 'Birleştirmeler', 'LEFT JOIN', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000083'),
    ('SQL ve Veritabanları', 'Birleştirmeler', 'SQL Alıştırmaları', 'DOCUMENT', FALSE, 900, 2, '5eed0000-0000-4000-8000-000000000084'),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 'Etiketler', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000086'),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 'Sayfa İskeleti', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000087'),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 'Formlar', 'PDF', TRUE, 360, 2, '5eed0000-0000-4000-8000-000000000088'),
    ('Web Geliştirmeye Giriş', 'CSS ile Biçimlendirme', 'Seçiciler', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000089'),
    ('Web Geliştirmeye Giriş', 'CSS ile Biçimlendirme', 'Kutu Modeli', 'VIDEO', TRUE, 35, 1, '5eed0000-0000-4000-8000-000000000090'),
    ('Web Geliştirmeye Giriş', 'CSS ile Biçimlendirme', 'Flexbox', 'PDF', TRUE, 420, 2, '5eed0000-0000-4000-8000-000000000091'),
    ('Web Geliştirmeye Giriş', 'Biraz JavaScript', 'Değişkenler ve Fonksiyonlar', 'PDF', TRUE, 360, 0, '5eed0000-0000-4000-8000-000000000092'),
    ('Web Geliştirmeye Giriş', 'Biraz JavaScript', 'Olaylar', 'PDF', TRUE, 360, 1, '5eed0000-0000-4000-8000-000000000093'),
    ('Web Geliştirmeye Giriş', 'Biraz JavaScript', 'Mini Proje', 'DOCUMENT', FALSE, 1200, 2, '5eed0000-0000-4000-8000-000000000094'),
    ('Python 2 ile Programlama (eski)', 'Giriş', 'print ifadesi', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000095'),
    ('Python 2 ile Programlama (eski)', 'Giriş', 'Kurulum (Python 2.7)', 'PDF', TRUE, 300, 1, '5eed0000-0000-4000-8000-000000000096'),
    ('Python 2 ile Programlama (eski)', 'Giriş', 'raw_input', 'PDF', TRUE, 300, 2, '5eed0000-0000-4000-8000-000000000097'),
    ('Python 2 ile Programlama (eski)', 'Farklar', 'Tam Sayı Bölmesi', 'PDF', TRUE, 300, 0, '5eed0000-0000-4000-8000-000000000098'),
    ('Python 2 ile Programlama (eski)', 'Farklar', 'Unicode', 'PDF', TRUE, 300, 1, '5eed0000-0000-4000-8000-000000000099'),
    ('Python 2 ile Programlama (eski)', 'Farklar', '2to3 Aracı', 'PDF', TRUE, 300, 2, '5eed0000-0000-4000-8000-000000000100'),
    ('Python 2 ile Programlama (eski)', 'Kapanış', 'Neden Python 3?', 'VIDEO', TRUE, 35, 0, '5eed0000-0000-4000-8000-000000000101'),
    ('Python 2 ile Programlama (eski)', 'Kapanış', 'Taşıma Kontrol Listesi', 'PDF', TRUE, 300, 1, '5eed0000-0000-4000-8000-000000000102'),
    ('Python 2 ile Programlama (eski)', 'Kapanış', 'Taşıma Alıştırması', 'DOCUMENT', FALSE, 900, 2, '5eed0000-0000-4000-8000-000000000103')
) AS l (course_title, section_title, title, content_type, mandatory, duration, position, file_key)
JOIN courses co ON co.title = l.course_title
JOIN sections se ON se.course_id = co.id AND se.title = l.section_title
LEFT JOIN stored_files sf ON sf.storage_key = l.file_key;

-- ---------------------------------------------------------------------------
-- Soru bankası ve bölüm sınavları (her kursta en az bir zorunlu sınav)
-- ---------------------------------------------------------------------------
INSERT INTO questions (course_id, text, points)
SELECT co.id, q.text, 10
FROM (VALUES
    ('Sıfırdan Python', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?'),
    ('Sıfırdan Python', 'Python sürümünü terminalde hangi komut gösterir?'),
    ('Sıfırdan Python', 'Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?'),
    ('Sıfırdan Python', 'Python hangi tür bir dildir?'),
    ('Sıfırdan Python', 'Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?'),
    ('Sıfırdan Python', 'return ifadesi ne yapar?'),
    ('Sıfırdan Python', 'Varsayılan parametre değeri ne zaman kullanılır?'),
    ('Sıfırdan Python', 'math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?'),
    ('Java ile Nesne Yönelimli Programlama', 'Java''da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?'),
    ('Java ile Nesne Yönelimli Programlama', 'Soyut (abstract) bir sınıf için hangisi doğrudur?'),
    ('Java ile Nesne Yönelimli Programlama', 'Kapsüllemenin amacı nedir?'),
    ('Java ile Nesne Yönelimli Programlama', 'Bir sınıf kaç arayüz uygulayabilir?'),
    ('Java ile Nesne Yönelimli Programlama', '@Override anotasyonu neyi belirtir?'),
    ('Pandas ile Veri Analizine Giriş', 'Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?'),
    ('Pandas ile Veri Analizine Giriş', 'Her sütundaki eksik değer sayısını hangi ifade verir?'),
    ('Pandas ile Veri Analizine Giriş', 'Tutarı 100''den büyük satırları süzen ifade hangisidir?'),
    ('Pandas ile Veri Analizine Giriş', 'Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?'),
    ('Arayüz Tasarımının Temelleri', 'WCAG AA''ya göre normal metin için en düşük kontrast oranı nedir?'),
    ('Arayüz Tasarımının Temelleri', 'Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?'),
    ('Arayüz Tasarımının Temelleri', 'İlişkili öğeleri gruplamanın en basit yolu hangisidir?'),
    ('Arayüz Tasarımının Temelleri', 'Form alanı için hangisi yeterli değildir?'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Kibar bir rica için hangisi daha uygundur?'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'Resmî bir e-postanın kapanışında hangisi kullanılır?'),
    ('İş İngilizcesi: E-posta ve Toplantılar', '"Yanıtınızı bekliyorum" anlamındaki kalıp hangisidir?'),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-postanın amacı nerede yazılmalıdır?'),
    ('Scrum ile Çevik Proje Yönetimi', 'Ürün iş listesindeki önceliklere kim karar verir?'),
    ('Scrum ile Çevik Proje Yönetimi', 'Günlük Scrum toplantısı en fazla ne kadar sürer?'),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?'),
    ('Scrum ile Çevik Proje Yönetimi', 'Scrum Master''ın temel görevi nedir?'),
    ('Scrum ile Çevik Proje Yönetimi', 'Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?'),
    ('Git ile Sürüm Kontrolü', 'Bir değişikliği hazırlık alanına alan komut hangisidir?'),
    ('Git ile Sürüm Kontrolü', 'Yeni bir branch açıp ona geçen komut hangisidir?'),
    ('Git ile Sürüm Kontrolü', 'Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?'),
    ('Git ile Sürüm Kontrolü', 'Çakışma çözüldükten sonra ne yapılır?'),
    ('SQL ve Veritabanları', 'Bir tablodan veri okumak için hangi komut kullanılır?'),
    ('SQL ve Veritabanları', 'Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?'),
    ('SQL ve Veritabanları', 'İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?'),
    ('SQL ve Veritabanları', 'WHERE koşulu yazılmayan bir UPDATE ne yapar?'),
    ('Web Geliştirmeye Giriş', 'Sayfanın en önemli başlığı için hangi etiket kullanılır?'),
    ('Web Geliştirmeye Giriş', 'Görünen içerik hangi etiketin içinde yer alır?'),
    ('Web Geliştirmeye Giriş', 'Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?'),
    ('Web Geliştirmeye Giriş', 'Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?'),
    ('Python 2 ile Programlama (eski)', 'Python 2''de print nasıl kullanılır?'),
    ('Python 2 ile Programlama (eski)', 'Python 2''de klavyeden metin almanın güvenli yolu hangisidir?'),
    ('Python 2 ile Programlama (eski)', 'Python 2''nin desteği hangi yıl sona erdi?'),
    ('Python 2 ile Programlama (eski)', 'Python 2''de 7 / 2 ifadesinin sonucu nedir?')
) AS q (course_title, text)
JOIN courses co ON co.title = q.course_title;

INSERT INTO options (question_id, text, is_correct)
SELECT q.id, o.text, o.correct
FROM (VALUES
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()', TRUE),
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'echo()', FALSE),
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'write()', FALSE),
    ('Python sürümünü terminalde hangi komut gösterir?', 'python --version', TRUE),
    ('Python sürümünü terminalde hangi komut gösterir?', 'python --help', FALSE),
    ('Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'input()', TRUE),
    ('Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'read()', FALSE),
    ('Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'scan()', FALSE),
    ('Python hangi tür bir dildir?', 'Yorumlanan, genel amaçlı bir dil', TRUE),
    ('Python hangi tür bir dildir?', 'Yalnız web sayfaları için bir işaretleme dili', FALSE),
    ('Python hangi tür bir dildir?', 'Yalnız veritabanı sorgu dili', FALSE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'def', TRUE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'function', FALSE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'func', FALSE),
    ('return ifadesi ne yapar?', 'Fonksiyondan değer döndürür', TRUE),
    ('return ifadesi ne yapar?', 'Programı kapatır', FALSE),
    ('Varsayılan parametre değeri ne zaman kullanılır?', 'Çağıran o argümanı vermediğinde', TRUE),
    ('Varsayılan parametre değeri ne zaman kullanılır?', 'Her çağrıda, verilen değerin yerine', FALSE),
    ('Varsayılan parametre değeri ne zaman kullanılır?', 'Yalnız fonksiyon ilk kez çağrıldığında', FALSE),
    ('math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?', 'import math', TRUE),
    ('math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?', 'include math', FALSE),
    ('math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?', 'using math', FALSE),
    ('Java''da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?', 'extends', TRUE),
    ('Java''da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?', 'implements', FALSE),
    ('Java''da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?', 'inherits', FALSE),
    ('Soyut (abstract) bir sınıf için hangisi doğrudur?', 'Doğrudan new ile örneklenemez', TRUE),
    ('Soyut (abstract) bir sınıf için hangisi doğrudur?', 'Hiç metot içeremez', FALSE),
    ('Soyut (abstract) bir sınıf için hangisi doğrudur?', 'Yalnız static alanları olabilir', FALSE),
    ('Kapsüllemenin amacı nedir?', 'Nesnenin iç durumunu yalnız kurallı metotlarla değiştirmek', TRUE),
    ('Kapsüllemenin amacı nedir?', 'Bütün alanları public yapmak', FALSE),
    ('Kapsüllemenin amacı nedir?', 'Sınıf sayısını azaltmak', FALSE),
    ('Bir sınıf kaç arayüz uygulayabilir?', 'Birden fazla', TRUE),
    ('Bir sınıf kaç arayüz uygulayabilir?', 'En fazla bir', FALSE),
    ('Bir sınıf kaç arayüz uygulayabilir?', 'Hiç; arayüzler yalnız kalıtılır', FALSE),
    ('@Override anotasyonu neyi belirtir?', 'Metodun üst sınıftaki ya da arayüzdeki bir metodu yeniden yazdığını', TRUE),
    ('@Override anotasyonu neyi belirtir?', 'Metodun statik olduğunu', FALSE),
    ('@Override anotasyonu neyi belirtir?', 'Metodun hiç çağrılmadığını', FALSE),
    ('Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?', 'pd.read_csv()', TRUE),
    ('Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?', 'pd.open_csv()', FALSE),
    ('Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?', 'pd.load()', FALSE),
    ('Her sütundaki eksik değer sayısını hangi ifade verir?', 'df.isna().sum()', TRUE),
    ('Her sütundaki eksik değer sayısını hangi ifade verir?', 'df.count_missing()', FALSE),
    ('Her sütundaki eksik değer sayısını hangi ifade verir?', 'df.empty()', FALSE),
    ('Tutarı 100''den büyük satırları süzen ifade hangisidir?', 'df[df["tutar"] > 100]', TRUE),
    ('Tutarı 100''den büyük satırları süzen ifade hangisidir?', 'df.filter(tutar > 100)', FALSE),
    ('Tutarı 100''den büyük satırları süzen ifade hangisidir?', 'df.where("tutar", 100)', FALSE),
    ('Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?', 'pd.to_datetime()', TRUE),
    ('Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?', 'pd.as_date()', FALSE),
    ('Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?', 'pd.parse()', FALSE),
    ('WCAG AA''ya göre normal metin için en düşük kontrast oranı nedir?', '4,5:1', TRUE),
    ('WCAG AA''ya göre normal metin için en düşük kontrast oranı nedir?', '2:1', FALSE),
    ('WCAG AA''ya göre normal metin için en düşük kontrast oranı nedir?', '10:1', FALSE),
    ('Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?', '44 piksel', TRUE),
    ('Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?', '16 piksel', FALSE),
    ('Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?', '24 piksel', FALSE),
    ('İlişkili öğeleri gruplamanın en basit yolu hangisidir?', 'Aralarındaki boşluğu azaltmak', TRUE),
    ('İlişkili öğeleri gruplamanın en basit yolu hangisidir?', 'Hepsini farklı renk yapmak', FALSE),
    ('İlişkili öğeleri gruplamanın en basit yolu hangisidir?', 'Hepsine gölge eklemek', FALSE),
    ('Form alanı için hangisi yeterli değildir?', 'Yalnız yer tutucu (placeholder) metin', TRUE),
    ('Form alanı için hangisi yeterli değildir?', 'Görünür bir etiket', FALSE),
    ('Form alanı için hangisi yeterli değildir?', 'Alanın altında hata mesajı', FALSE),
    ('Kibar bir rica için hangisi daha uygundur?', 'Could you please send me the report?', TRUE),
    ('Kibar bir rica için hangisi daha uygundur?', 'Send me the report.', FALSE),
    ('Kibar bir rica için hangisi daha uygundur?', 'You must send the report.', FALSE),
    ('Resmî bir e-postanın kapanışında hangisi kullanılır?', 'Best regards', TRUE),
    ('Resmî bir e-postanın kapanışında hangisi kullanılır?', 'See ya', FALSE),
    ('Resmî bir e-postanın kapanışında hangisi kullanılır?', 'Bye bye', FALSE),
    ('"Yanıtınızı bekliyorum" anlamındaki kalıp hangisidir?', 'I look forward to hearing from you.', TRUE),
    ('"Yanıtınızı bekliyorum" anlamındaki kalıp hangisidir?', 'I wait your answer.', FALSE),
    ('"Yanıtınızı bekliyorum" anlamındaki kalıp hangisidir?', 'Answer me soon.', FALSE),
    ('E-postanın amacı nerede yazılmalıdır?', 'İlk paragrafta, mümkünse ilk cümlede', TRUE),
    ('E-postanın amacı nerede yazılmalıdır?', 'Son paragrafta', FALSE),
    ('E-postanın amacı nerede yazılmalıdır?', 'Yalnız konu satırında', FALSE),
    ('Ürün iş listesindeki önceliklere kim karar verir?', 'Ürün sahibi', TRUE),
    ('Ürün iş listesindeki önceliklere kim karar verir?', 'Scrum Master', FALSE),
    ('Ürün iş listesindeki önceliklere kim karar verir?', 'Şirket yöneticisi', FALSE),
    ('Günlük Scrum toplantısı en fazla ne kadar sürer?', '15 dakika', TRUE),
    ('Günlük Scrum toplantısı en fazla ne kadar sürer?', '1 saat', FALSE),
    ('Günlük Scrum toplantısı en fazla ne kadar sürer?', 'Yarım gün', FALSE),
    ('Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?', 'Sprint incelemesi', TRUE),
    ('Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?', 'Sprint planlama', FALSE),
    ('Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?', 'Günlük Scrum', FALSE),
    ('Scrum Master''ın temel görevi nedir?', 'Süreci kolaylaştırmak ve engelleri kaldırmak', TRUE),
    ('Scrum Master''ın temel görevi nedir?', 'Görevleri geliştiricilere dağıtmak', FALSE),
    ('Scrum Master''ın temel görevi nedir?', 'Ürünün özelliklerine karar vermek', FALSE),
    ('Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?', 'Kabul kriterleri', TRUE),
    ('Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?', 'Hikâye puanı', FALSE),
    ('Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?', 'Başlık', FALSE),
    ('Bir değişikliği hazırlık alanına alan komut hangisidir?', 'git add', TRUE),
    ('Bir değişikliği hazırlık alanına alan komut hangisidir?', 'git push', FALSE),
    ('Bir değişikliği hazırlık alanına alan komut hangisidir?', 'git init', FALSE),
    ('Yeni bir branch açıp ona geçen komut hangisidir?', 'git switch -c yeni-branch', TRUE),
    ('Yeni bir branch açıp ona geçen komut hangisidir?', 'git merge yeni-branch', FALSE),
    ('Yeni bir branch açıp ona geçen komut hangisidir?', 'git log yeni-branch', FALSE),
    ('Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?', 'git revert', TRUE),
    ('Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?', 'git reset --hard', FALSE),
    ('Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?', 'Dosyayı silmek', FALSE),
    ('Çakışma çözüldükten sonra ne yapılır?', 'Dosya git add ile hazırlanır ve commit atılır', TRUE),
    ('Çakışma çözüldükten sonra ne yapılır?', 'Depo silinip yeniden klonlanır', FALSE),
    ('Çakışma çözüldükten sonra ne yapılır?', 'Hiçbir şey; Git kendisi çözer', FALSE),
    ('Bir tablodan veri okumak için hangi komut kullanılır?', 'SELECT', TRUE),
    ('Bir tablodan veri okumak için hangi komut kullanılır?', 'INSERT', FALSE),
    ('Bir tablodan veri okumak için hangi komut kullanılır?', 'CREATE', FALSE),
    ('Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?', 'HAVING', TRUE),
    ('Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?', 'WHERE', FALSE),
    ('Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?', 'ORDER BY', FALSE),
    ('İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?', 'JOIN', TRUE),
    ('İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?', 'UNION', FALSE),
    ('İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?', 'MERGE INTO', FALSE),
    ('WHERE koşulu yazılmayan bir UPDATE ne yapar?', 'Tablodaki bütün satırları günceller', TRUE),
    ('WHERE koşulu yazılmayan bir UPDATE ne yapar?', 'Hiçbir satırı güncellemez', FALSE),
    ('WHERE koşulu yazılmayan bir UPDATE ne yapar?', 'Hata verir', FALSE),
    ('Sayfanın en önemli başlığı için hangi etiket kullanılır?', '<h1>', TRUE),
    ('Sayfanın en önemli başlığı için hangi etiket kullanılır?', '<head>', FALSE),
    ('Sayfanın en önemli başlığı için hangi etiket kullanılır?', '<title>', FALSE),
    ('Görünen içerik hangi etiketin içinde yer alır?', '<body>', TRUE),
    ('Görünen içerik hangi etiketin içinde yer alır?', '<head>', FALSE),
    ('Görünen içerik hangi etiketin içinde yer alır?', '<meta>', FALSE),
    ('Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?', '<label for="…">', TRUE),
    ('Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?', '<span>', FALSE),
    ('Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?', '<div class="label">', FALSE),
    ('Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?', '<meta charset="utf-8">', TRUE),
    ('Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?', '<html lang="en">', FALSE),
    ('Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?', '<link rel="font">', FALSE),
    ('Python 2''de print nasıl kullanılır?', 'Parantezsiz bir ifade olarak', TRUE),
    ('Python 2''de print nasıl kullanılır?', 'Yalnız print() fonksiyonu olarak', FALSE),
    ('Python 2''de print nasıl kullanılır?', 'Python 2''de print yoktur', FALSE),
    ('Python 2''de klavyeden metin almanın güvenli yolu hangisidir?', 'raw_input()', TRUE),
    ('Python 2''de klavyeden metin almanın güvenli yolu hangisidir?', 'scanf()', FALSE),
    ('Python 2''de klavyeden metin almanın güvenli yolu hangisidir?', 'readline()', FALSE),
    ('Python 2''nin desteği hangi yıl sona erdi?', '2020', TRUE),
    ('Python 2''nin desteği hangi yıl sona erdi?', '2010', FALSE),
    ('Python 2''nin desteği hangi yıl sona erdi?', '2025', FALSE),
    ('Python 2''de 7 / 2 ifadesinin sonucu nedir?', '3', TRUE),
    ('Python 2''de 7 / 2 ifadesinin sonucu nedir?', '3.5', FALSE),
    ('Python 2''de 7 / 2 ifadesinin sonucu nedir?', '4', FALSE)
) AS o (question, text, correct)
JOIN questions q ON q.text = o.question;

INSERT INTO quizzes (section_id, title, scoring_type, pass_threshold, required)
SELECT se.id, z.title, 'SINGLE_ANSWER', z.threshold, TRUE
FROM (VALUES
    ('Sıfırdan Python', 'Giriş', 'Giriş sınavı', 70),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Fonksiyonlar sınavı', 60),
    ('Java ile Nesne Yönelimli Programlama', 'Kalıtım ve Arayüzler', 'Nesne yönelimli tasarım sınavı', 70),
    ('Pandas ile Veri Analizine Giriş', 'Veri Temizleme', 'Pandas temelleri sınavı', 70),
    ('Arayüz Tasarımının Temelleri', 'Renk ve Erişilebilirlik', 'Arayüz ilkeleri sınavı', 70),
    ('İş İngilizcesi: E-posta ve Toplantılar', 'E-posta Yazmak', 'İş e-postası sınavı', 70),
    ('Scrum ile Çevik Proje Yönetimi', 'Sprint', 'Scrum temelleri sınavı', 70),
    ('Git ile Sürüm Kontrolü', 'Branch ve Merge', 'Git temelleri sınavı', 70),
    ('SQL ve Veritabanları', 'Gruplama', 'SQL temelleri sınavı', 70),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 'HTML temelleri sınavı', 70),
    ('Python 2 ile Programlama (eski)', 'Giriş', 'Python 2 farkları sınavı', 70)
) AS z (course_title, section_title, title, threshold)
JOIN courses co ON co.title = z.course_title
JOIN sections se ON se.course_id = co.id AND se.title = z.section_title;

INSERT INTO quiz_questions (quiz_id, question_id, position)
SELECT qz.id, q.id, x.position
FROM (VALUES
    ('Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 0),
    ('Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 1),
    ('Giriş sınavı', 'Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 2),
    ('Giriş sınavı', 'Python hangi tür bir dildir?', 3),
    ('Fonksiyonlar sınavı', 'Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 0),
    ('Fonksiyonlar sınavı', 'return ifadesi ne yapar?', 1),
    ('Fonksiyonlar sınavı', 'Varsayılan parametre değeri ne zaman kullanılır?', 2),
    ('Fonksiyonlar sınavı', 'math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?', 3),
    ('Nesne yönelimli tasarım sınavı', 'Java''da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?', 0),
    ('Nesne yönelimli tasarım sınavı', 'Soyut (abstract) bir sınıf için hangisi doğrudur?', 1),
    ('Nesne yönelimli tasarım sınavı', 'Kapsüllemenin amacı nedir?', 2),
    ('Nesne yönelimli tasarım sınavı', 'Bir sınıf kaç arayüz uygulayabilir?', 3),
    ('Nesne yönelimli tasarım sınavı', '@Override anotasyonu neyi belirtir?', 4),
    ('Pandas temelleri sınavı', 'Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?', 0),
    ('Pandas temelleri sınavı', 'Her sütundaki eksik değer sayısını hangi ifade verir?', 1),
    ('Pandas temelleri sınavı', 'Tutarı 100''den büyük satırları süzen ifade hangisidir?', 2),
    ('Pandas temelleri sınavı', 'Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?', 3),
    ('Arayüz ilkeleri sınavı', 'WCAG AA''ya göre normal metin için en düşük kontrast oranı nedir?', 0),
    ('Arayüz ilkeleri sınavı', 'Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?', 1),
    ('Arayüz ilkeleri sınavı', 'İlişkili öğeleri gruplamanın en basit yolu hangisidir?', 2),
    ('Arayüz ilkeleri sınavı', 'Form alanı için hangisi yeterli değildir?', 3),
    ('İş e-postası sınavı', 'Kibar bir rica için hangisi daha uygundur?', 0),
    ('İş e-postası sınavı', 'Resmî bir e-postanın kapanışında hangisi kullanılır?', 1),
    ('İş e-postası sınavı', '"Yanıtınızı bekliyorum" anlamındaki kalıp hangisidir?', 2),
    ('İş e-postası sınavı', 'E-postanın amacı nerede yazılmalıdır?', 3),
    ('Scrum temelleri sınavı', 'Ürün iş listesindeki önceliklere kim karar verir?', 0),
    ('Scrum temelleri sınavı', 'Günlük Scrum toplantısı en fazla ne kadar sürer?', 1),
    ('Scrum temelleri sınavı', 'Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?', 2),
    ('Scrum temelleri sınavı', 'Scrum Master''ın temel görevi nedir?', 3),
    ('Scrum temelleri sınavı', 'Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?', 4),
    ('Git temelleri sınavı', 'Bir değişikliği hazırlık alanına alan komut hangisidir?', 0),
    ('Git temelleri sınavı', 'Yeni bir branch açıp ona geçen komut hangisidir?', 1),
    ('Git temelleri sınavı', 'Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?', 2),
    ('Git temelleri sınavı', 'Çakışma çözüldükten sonra ne yapılır?', 3),
    ('SQL temelleri sınavı', 'Bir tablodan veri okumak için hangi komut kullanılır?', 0),
    ('SQL temelleri sınavı', 'Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?', 1),
    ('SQL temelleri sınavı', 'İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?', 2),
    ('SQL temelleri sınavı', 'WHERE koşulu yazılmayan bir UPDATE ne yapar?', 3),
    ('HTML temelleri sınavı', 'Sayfanın en önemli başlığı için hangi etiket kullanılır?', 0),
    ('HTML temelleri sınavı', 'Görünen içerik hangi etiketin içinde yer alır?', 1),
    ('HTML temelleri sınavı', 'Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?', 2),
    ('HTML temelleri sınavı', 'Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?', 3),
    ('Python 2 farkları sınavı', 'Python 2''de print nasıl kullanılır?', 0),
    ('Python 2 farkları sınavı', 'Python 2''de klavyeden metin almanın güvenli yolu hangisidir?', 1),
    ('Python 2 farkları sınavı', 'Python 2''nin desteği hangi yıl sona erdi?', 2),
    ('Python 2 farkları sınavı', 'Python 2''de 7 / 2 ifadesinin sonucu nedir?', 3)
) AS x (quiz_title, question, position)
JOIN quizzes qz ON qz.title = x.quiz_title
JOIN questions q ON q.text = x.question;

-- ---------------------------------------------------------------------------
-- Kullanıcı verisi (scripts/seed-content/kullanici-verisi.sql). Demo senaryolarının (docs/demo.md) beklediği durumlar:
-- Python'da Ayşe %100 + sertifika, Can %66,67 (son ders Fonksiyon Tanımlama), Elif %50 ve Giriş sınavından kaldı,
-- Burak %16,67, Deniz %0; Python'da 5 kayıt, 3 yorum (ortalama 4,3). Java'da Selin %50.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- Kayıtlar
-- ---------------------------------------------------------------------------
INSERT INTO enrollments (student_id, course_id, enrolled_at)
SELECT u.id, co.id, now() - make_interval(days => e.days_ago)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 20),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 15),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 10),
    ('burak.celik@miniudemy.dev', 'Sıfırdan Python', 6),
    ('deniz.arslan@miniudemy.dev', 'Sıfırdan Python', 2),
    ('ayse.yilmaz@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 5),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 8),
    ('can.demir@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 12),
    ('selin.koc@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 9),
    ('elif.sahin@miniudemy.dev', 'Arayüz Tasarımının Temelleri', 4),
    ('ayse.yilmaz@miniudemy.dev', 'Scrum ile Çevik Proje Yönetimi', 7),
    ('burak.celik@miniudemy.dev', 'Git ile Sürüm Kontrolü', 3),
    ('selin.koc@miniudemy.dev', 'İş İngilizcesi: E-posta ve Toplantılar', 1)
) AS e (email, course_title, days_ago)
JOIN users u ON u.email = e.email
JOIN courses co ON co.title = e.course_title;

-- Tamamlanan dersler: kursun ilk N dersi (bölüm ve ders sırasıyla).
-- Python 12 ders: Ayşe 12, Can 8, Elif 6, Burak 2. Java 10 ders: Selin 5. Diğerleri 9 ders.
INSERT INTO lesson_progress (enrollment_id, lesson_id, completed_at)
SELECT en.id, ol.lesson_id, now() - make_interval(days => p.days_ago) + make_interval(hours => ol.rn::int)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 12, 19),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 8, 14),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 6, 9),
    ('burak.celik@miniudemy.dev', 'Sıfırdan Python', 2, 5),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 5, 7),
    ('can.demir@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 4, 10),
    ('selin.koc@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 3, 8),
    ('elif.sahin@miniudemy.dev', 'Arayüz Tasarımının Temelleri', 2, 3),
    ('ayse.yilmaz@miniudemy.dev', 'Scrum ile Çevik Proje Yönetimi', 3, 6),
    ('burak.celik@miniudemy.dev', 'Git ile Sürüm Kontrolü', 5, 2)
) AS p (email, course_title, n, days_ago)
JOIN users u ON u.email = p.email
JOIN courses co ON co.title = p.course_title
JOIN enrollments en ON en.student_id = u.id AND en.course_id = co.id
JOIN LATERAL (
    SELECT l.id AS lesson_id, row_number() OVER (ORDER BY s.position, l.position) AS rn
    FROM lessons l
    JOIN sections s ON s.id = l.section_id
    WHERE s.course_id = co.id
) ol ON ol.rn <= p.n;

-- İlerleme önbelleği ve son izlenen ders (yüzde = tamamlanan ders / toplam ders, iki ondalık)
INSERT INTO course_progress (enrollment_id, progress_percent, last_lesson_id, last_accessed_at)
SELECT en.id, c.percent, le.id, CASE WHEN c.lesson_title IS NULL THEN NULL ELSE now() - make_interval(days => c.days_ago) END
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 100.00, 'Sonraki Adımlar', 11),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 66.67, 'Fonksiyon Tanımlama', 1),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 50.00, 'Listeler ve Sözlükler', 2),
    ('burak.celik@miniudemy.dev', 'Sıfırdan Python', 16.67, 'İlk Program', 5),
    ('deniz.arslan@miniudemy.dev', 'Sıfırdan Python', 0.00, NULL, 0),
    ('ayse.yilmaz@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 0.00, NULL, 0),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 50.00, 'Kalıtım', 3),
    ('can.demir@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 44.44, 'Eksik Veriler', 4),
    ('selin.koc@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 33.33, 'Sütun Seçme ve Süzme', 6),
    ('elif.sahin@miniudemy.dev', 'Arayüz Tasarımının Temelleri', 22.22, 'Boşluk ve Hizalama', 1),
    ('ayse.yilmaz@miniudemy.dev', 'Scrum ile Çevik Proje Yönetimi', 33.33, 'Scrum Rolleri', 2),
    ('burak.celik@miniudemy.dev', 'Git ile Sürüm Kontrolü', 55.56, 'Merge ve Çakışma', 1),
    ('selin.koc@miniudemy.dev', 'İş İngilizcesi: E-posta ve Toplantılar', 0.00, NULL, 0)
) AS c (email, course_title, percent, lesson_title, days_ago)
JOIN users u ON u.email = c.email
JOIN courses co ON co.title = c.course_title
JOIN enrollments en ON en.student_id = u.id AND en.course_id = co.id
LEFT JOIN sections se ON se.course_id = co.id AND c.lesson_title IS NOT NULL
    AND EXISTS (SELECT 1 FROM lessons l2 WHERE l2.section_id = se.id AND l2.title = c.lesson_title)
LEFT JOIN lessons le ON le.section_id = se.id AND le.title = c.lesson_title;

-- ---------------------------------------------------------------------------
-- Sınav denemeleri (soru başına 10 puan): Ayşe Python'un iki sınavını da geçti; Can Giriş'i ve Pandas'ı geçti;
-- Elif Giriş'ten kaldı (%50 < %70); Burak Git sınavından kaldı.
-- ---------------------------------------------------------------------------
INSERT INTO quiz_attempts (quiz_id, student_id, score, max_score, passed, submitted_at)
SELECT qz.id, u.id, a.score, a.max_score, a.passed, now() - make_interval(days => a.days_ago)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 40, 40, TRUE, 17),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 40, 40, TRUE, 11),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 40, 40, TRUE, 12),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 20, 40, FALSE, 6),
    ('can.demir@miniudemy.dev', 'Pandas temelleri sınavı', 30, 40, TRUE, 4),
    ('burak.celik@miniudemy.dev', 'Git temelleri sınavı', 20, 40, FALSE, 1)
) AS a (email, quiz_title, score, max_score, passed, days_ago)
JOIN users u ON u.email = a.email
JOIN quizzes qz ON qz.title = a.quiz_title;

INSERT INTO attempt_answers (attempt_id, question_id, option_id)
SELECT at.id, q.id, o.id
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --version'),
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'input()'),
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Python hangi tür bir dildir?', 'Yorumlanan, genel amaçlı bir dil'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'def'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'return ifadesi ne yapar?', 'Fonksiyondan değer döndürür'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'Varsayılan parametre değeri ne zaman kullanılır?', 'Çağıran o argümanı vermediğinde'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?', 'import math'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --version'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'input()'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Python hangi tür bir dildir?', 'Yorumlanan, genel amaçlı bir dil'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --help'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?', 'input()'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Python hangi tür bir dildir?', 'Yalnız web sayfaları için bir işaretleme dili'),
    ('can.demir@miniudemy.dev', 'Pandas temelleri sınavı', 'Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?', 'pd.read_csv()'),
    ('can.demir@miniudemy.dev', 'Pandas temelleri sınavı', 'Her sütundaki eksik değer sayısını hangi ifade verir?', 'df.isna().sum()'),
    ('can.demir@miniudemy.dev', 'Pandas temelleri sınavı', 'Tutarı 100''den büyük satırları süzen ifade hangisidir?', 'df[df["tutar"] > 100]'),
    ('can.demir@miniudemy.dev', 'Pandas temelleri sınavı', 'Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?', 'pd.parse()'),
    ('burak.celik@miniudemy.dev', 'Git temelleri sınavı', 'Bir değişikliği hazırlık alanına alan komut hangisidir?', 'git add'),
    ('burak.celik@miniudemy.dev', 'Git temelleri sınavı', 'Yeni bir branch açıp ona geçen komut hangisidir?', 'git merge yeni-branch'),
    ('burak.celik@miniudemy.dev', 'Git temelleri sınavı', 'Paylaşılmış bir commit''i geçmişi silmeden geri almanın yolu nedir?', 'git revert'),
    ('burak.celik@miniudemy.dev', 'Git temelleri sınavı', 'Çakışma çözüldükten sonra ne yapılır?', 'Depo silinip yeniden klonlanır')
) AS x (email, quiz_title, question, option_text)
JOIN users u ON u.email = x.email
JOIN quizzes qz ON qz.title = x.quiz_title
JOIN quiz_attempts at ON at.quiz_id = qz.id AND at.student_id = u.id
JOIN questions q ON q.text = x.question
JOIN options o ON o.question_id = q.id AND o.text = x.option_text;

-- ---------------------------------------------------------------------------
-- Sertifika: Ayşe Python'un zorunlu derslerinin ve zorunlu sınavlarının hepsini tamamladı
-- ---------------------------------------------------------------------------
INSERT INTO certificates (enrollment_id, verification_code, issued_at)
SELECT en.id, 'K7Q2-M9XA-4T', now() - make_interval(days => 11)
FROM enrollments en
JOIN users u ON u.id = en.student_id AND u.email = 'ayse.yilmaz@miniudemy.dev'
JOIN courses co ON co.id = en.course_id AND co.title = 'Sıfırdan Python';

-- ---------------------------------------------------------------------------
-- Değerlendirmeler (yalnız kayıtlı kursiyerler). Python'daki üçü demo senaryosu 2b'nin sayıları için değişmez.
-- ---------------------------------------------------------------------------
INSERT INTO reviews (user_id, course_id, rating, comment, created_at, updated_at)
SELECT u.id, co.id, r.rating, r.comment, now() - make_interval(days => r.days_ago), now() - make_interval(days => r.days_ago)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 5, 'Çok anlaşılır, örnekler tam yerinde.', 10),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 4, 'Güzel kurs, video dersler biraz daha uzun olabilirdi.', 8),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 4, NULL, 5),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 3, 'Kalıtım bölümü hızlı geçiyor.', 4),
    ('ayse.yilmaz@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 4,
        'Kütüphane örneği konuları birbirine bağlıyor; kapsülleme dersi çok açık.', 3),
    ('can.demir@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 5,
        'Kitabevi veri setiyle çalışmak çok öğretici, groupby sonunda oturdu.', 5),
    ('selin.koc@miniudemy.dev', 'Pandas ile Veri Analizine Giriş', 4, 'Temizleme bölümü harika; grafik dersi biraz kısa.', 6),
    ('elif.sahin@miniudemy.dev', 'Arayüz Tasarımının Temelleri', 5,
        'Kontrast dersinden sonra kendi projemdeki renkleri düzelttim.', 2),
    ('ayse.yilmaz@miniudemy.dev', 'Scrum ile Çevik Proje Yönetimi', 4,
        'Sprint planlama şablonunu staj yaptığım ekipte kullandık.', 4),
    ('burak.celik@miniudemy.dev', 'Git ile Sürüm Kontrolü', 5, 'Çakışma çözmeyi ilk kez gerçekten anladım.', 1)
) AS r (email, course_title, rating, comment, days_ago)
JOIN users u ON u.email = r.email
JOIN courses co ON co.title = r.course_title;

-- ---------------------------------------------------------------------------
-- Şikâyet: yöneticinin şikâyet ekranında incelenecek açık bir örnek (#144)
-- ---------------------------------------------------------------------------
INSERT INTO complaints (reporter_id, course_id, reason, status, created_at)
SELECT u.id, co.id,
       'Soyut Sınıflar ders notundaki Dergi örneği tek başına derlenmiyor; Yayin sınıfının tamamı verilmeli.',
       'OPEN', now() - make_interval(days => 2)
FROM users u
JOIN courses co ON co.title = 'Java ile Nesne Yönelimli Programlama'
WHERE u.email = 'selin.koc@miniudemy.dev';

-- ---------------------------------------------------------------------------
-- Bildirimler (#124): yukarıdaki kayıt, onay/red, sınav ve sertifika satırlarından üretilir. Metin ve bağlantılar
-- NotificationMessages ile aynıdır (DemoSeedIT karşılaştırır). 7 günden eski olanlar okunmuş, yeniler okunmamış.
-- ---------------------------------------------------------------------------
INSERT INTO notifications (user_id, type, title, body, link, read_at, created_at)
SELECT n.user_id, n.type, n.title, n.body, n.link,
       CASE WHEN n.created_at < now() - make_interval(days => 7) THEN n.created_at + make_interval(hours => 1) END,
       n.created_at
FROM (
    -- Kursa kayıt → eğitmene
    SELECT co.instructor_id AS user_id, 'ENROLLMENT_CREATED' AS type, 'Kursunuza yeni kayıt' AS title,
           st.full_name || ' "' || co.title || '" kursunuza kaydoldu.' AS body,
           '/instructor/courses/' || co.id || '/analytics' AS link, en.enrolled_at AS created_at
    FROM enrollments en
    JOIN courses co ON co.id = en.course_id
    JOIN users st ON st.id = en.student_id
    UNION ALL
    -- Kurs onayı → eğitmene (ilk kayıtlardan önce)
    SELECT co.instructor_id, 'COURSE_APPROVED', 'Kursunuz onaylandı',
           '"' || co.title || '" kursunuz onaylandı ve katalogda yayımlandı.',
           '/courses/' || co.id, now() - make_interval(days => 25)
    FROM courses co
    WHERE co.status = 'PUBLISHED'
    UNION ALL
    -- Kurs reddi → eğitmene, gerekçeyle
    SELECT co.instructor_id, 'COURSE_REJECTED', 'Kursunuz reddedildi',
           '"' || co.title || '" kursunuz reddedildi. Gerekçe: ' || co.rejection_reason,
           '/instructor', now() - make_interval(days => 3)
    FROM courses co
    WHERE co.rejection_reason IS NOT NULL
    UNION ALL
    -- Sınav sonucu → kursiyere
    SELECT at.student_id, 'QUIZ_COMPLETED',
           CASE WHEN at.passed THEN 'Sınavı geçtiniz' ELSE 'Sınavı geçemediniz' END,
           '"' || co.title || '" kursundaki sınavı ' || CASE WHEN at.passed THEN 'geçtiniz.' ELSE 'geçemediniz.' END,
           '/quizzes/' || at.quiz_id || '/attempts', at.submitted_at
    FROM quiz_attempts at
    JOIN quizzes qz ON qz.id = at.quiz_id
    JOIN sections se ON se.id = qz.section_id
    JOIN courses co ON co.id = se.course_id
    UNION ALL
    -- Sertifika → kursiyere
    SELECT en.student_id, 'CERTIFICATE_ISSUED', 'Sertifikanız hazır',
           '"' || co.title || '" kursunu tamamladınız; sertifikanız oluşturuldu.',
           '/certificates', ce.issued_at
    FROM certificates ce
    JOIN enrollments en ON en.id = ce.enrollment_id
    JOIN courses co ON co.id = en.course_id
) AS n;
