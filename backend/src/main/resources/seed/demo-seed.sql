-- Demo ve manuel test verisi (#67). Yalnız dev profilinde, veritabanı boşken DemoSeedRunner çalıştırır.
-- Bütün hesapların parolası README'de; {{PASSWORD_HASH}} çalışma anında BCrypt özetiyle değiştirilir.
-- Kimlikler sabit yazılmaz; satırlar e-posta, başlık ve depolama anahtarıyla bulunur (sıralar ilerler).
-- Dosyaların kendisi seed/files/<storage_key> altında; DemoSeedRunner depolama dizinine kopyalar ve boyutlarını yazar.

-- ---------------------------------------------------------------------------
-- Kullanıcılar: 1 yönetici, 2 eğitmen, 6 kursiyer
-- ---------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, bio, role) VALUES
    ('admin@miniudemy.dev', '{{PASSWORD_HASH}}', 'Ali Yönetici', NULL, 'ADMIN'),
    ('mehmet.kaya@miniudemy.dev', '{{PASSWORD_HASH}}', 'Mehmet Kaya', 'Python ve veri tabanları eğitmeni.', 'INSTRUCTOR'),
    ('zeynep.ak@miniudemy.dev', '{{PASSWORD_HASH}}', 'Zeynep Ak', 'Java ve web geliştirme eğitmeni.', 'INSTRUCTOR'),
    ('ayse.yilmaz@miniudemy.dev', '{{PASSWORD_HASH}}', 'Ayşe Yılmaz', NULL, 'STUDENT'),
    ('can.demir@miniudemy.dev', '{{PASSWORD_HASH}}', 'Can Demir', NULL, 'STUDENT'),
    ('elif.sahin@miniudemy.dev', '{{PASSWORD_HASH}}', 'Elif Şahin', NULL, 'STUDENT'),
    ('burak.celik@miniudemy.dev', '{{PASSWORD_HASH}}', 'Burak Çelik', NULL, 'STUDENT'),
    ('deniz.arslan@miniudemy.dev', '{{PASSWORD_HASH}}', 'Deniz Arslan', NULL, 'STUDENT'),
    ('selin.koc@miniudemy.dev', '{{PASSWORD_HASH}}', 'Selin Koç', NULL, 'STUDENT');

-- ---------------------------------------------------------------------------
-- Dosyalar (size_bytes'ı DemoSeedRunner dosyayı kopyalarken gerçek boyutla günceller)
-- ---------------------------------------------------------------------------
INSERT INTO stored_files (uploader_id, storage_key, original_name, mime_type, size_bytes)
SELECT u.id, f.storage_key, f.original_name, f.mime_type, 0
FROM (VALUES
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000001', 'python-kurulum.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000002', 'python-veri-tipleri.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000003', 'python-fonksiyonlar.pdf', 'application/pdf'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000004', 'python-alistirmalar.docx',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000005', 'kapak-python.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000006', 'kapak-java.png', 'image/png'),
    ('zeynep.ak@miniudemy.dev', '5eed0000-0000-4000-8000-000000000007', 'kapak-web.png', 'image/png'),
    ('mehmet.kaya@miniudemy.dev', '5eed0000-0000-4000-8000-000000000008', 'kapak-sql.png', 'image/png')
) AS f (email, storage_key, original_name, mime_type)
JOIN users u ON u.email = f.email;

-- ---------------------------------------------------------------------------
-- Kurslar: her durumdan en az bir tane
-- ---------------------------------------------------------------------------
INSERT INTO courses (instructor_id, category_id, cover_file_id, title, description, level, learning_outcomes, status,
                     rejection_reason, unpublish_reason)
SELECT u.id, c.id, sf.id, x.title, x.description, x.level, x.outcomes, x.status, x.rejection_reason, x.unpublish_reason
FROM (VALUES
    ('mehmet.kaya@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000005', 'Sıfırdan Python',
        'Hiç programlama bilmeyenler için Python: kurulum, veri tipleri ve fonksiyonlar.', 'BEGINNER',
        ARRAY['Python kurup ilk programını çalıştırır', 'Temel veri tiplerini kullanır', 'Fonksiyon yazar'],
        'PUBLISHED', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000006',
        'Java ile Nesne Yönelimli Programlama',
        'Sınıflar, nesneler, kalıtım ve arayüzlerle Java''da nesne yönelimli tasarım.', 'INTERMEDIATE',
        ARRAY['Sınıf ve nesne tasarlar', 'Kalıtım ve arayüz kullanır'], 'PUBLISHED', NULL, NULL),
    ('zeynep.ak@miniudemy.dev', 'Yazılım', '5eed0000-0000-4000-8000-000000000007', 'Web Geliştirmeye Giriş',
        'HTML, CSS ve biraz JavaScript ile ilk web sayfanı yap.', 'BEGINNER',
        ARRAY['HTML sayfası hazırlar', 'CSS ile biçimlendirir'], 'DRAFT',
        'Bölümlerde örnek proje eksik. Her bölüme kısa bir uygulama ekleyin.', NULL),
    ('mehmet.kaya@miniudemy.dev', 'Veri Bilimi', '5eed0000-0000-4000-8000-000000000008', 'SQL ve Veritabanları',
        'SELECT''ten JOIN''e ilişkisel veritabanlarıyla çalışma.', 'BEGINNER',
        ARRAY['SQL sorgusu yazar', 'Tabloları birleştirir'], 'IN_REVIEW', NULL, NULL),
    ('mehmet.kaya@miniudemy.dev', 'Yazılım', NULL, 'Python 2 ile Programlama (eski)',
        'Python 2 için hazırlanmış eski kurs.', 'BEGINNER', ARRAY['Python 2 sözdizimi'], 'UNPUBLISHED', NULL,
        'Python 2 artık desteklenmiyor. Yerine Sıfırdan Python kursunu önerin.')
) AS x (email, category, cover_key, title, description, level, outcomes, status, rejection_reason, unpublish_reason)
JOIN users u ON u.email = x.email
JOIN categories c ON c.name = x.category
LEFT JOIN stored_files sf ON sf.storage_key = x.cover_key;

-- ---------------------------------------------------------------------------
-- Bölümler
-- ---------------------------------------------------------------------------
INSERT INTO sections (course_id, title, position)
SELECT co.id, s.title, s.position
FROM (VALUES
    ('Sıfırdan Python', 'Giriş', 0),
    ('Sıfırdan Python', 'Veri Tipleri', 1),
    ('Sıfırdan Python', 'Fonksiyonlar', 2),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 0),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 0),
    ('SQL ve Veritabanları', 'İlk Sorgular', 0),
    ('Python 2 ile Programlama (eski)', 'Giriş', 0)
) AS s (course_title, title, position)
JOIN courses co ON co.title = s.course_title;

-- ---------------------------------------------------------------------------
-- Dersler. Video derslerinin dosyası yok (demo videosu yükleme ucuyla eklenir, README); PDF ve doküman var.
-- ---------------------------------------------------------------------------
INSERT INTO lessons (section_id, content_file_id, title, content_type, mandatory, duration_seconds, position)
SELECT se.id, sf.id, l.title, l.content_type, l.mandatory, l.duration, l.position
FROM (VALUES
    ('Sıfırdan Python', 'Giriş', 'Kurulum', 'PDF', TRUE, 300, 0, '5eed0000-0000-4000-8000-000000000001'),
    ('Sıfırdan Python', 'Giriş', 'İlk Program', 'VIDEO', TRUE, 420, 1, NULL),
    ('Sıfırdan Python', 'Veri Tipleri', 'Sayılar ve Metinler', 'PDF', TRUE, 360, 0,
        '5eed0000-0000-4000-8000-000000000002'),
    ('Sıfırdan Python', 'Veri Tipleri', 'Listeler ve Sözlükler', 'VIDEO', TRUE, 540, 1, NULL),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Fonksiyon Tanımlama', 'PDF', TRUE, 480, 0,
        '5eed0000-0000-4000-8000-000000000003'),
    ('Sıfırdan Python', 'Fonksiyonlar', 'Alıştırmalar', 'DOCUMENT', FALSE, 600, 1,
        '5eed0000-0000-4000-8000-000000000004'),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 'Sınıf Nedir?', 'VIDEO', TRUE, 600, 0, NULL),
    ('Java ile Nesne Yönelimli Programlama', 'Sınıflar ve Nesneler', 'Kalıtım', 'VIDEO', TRUE, 720, 1, NULL),
    ('Web Geliştirmeye Giriş', 'HTML Temelleri', 'Etiketler', 'VIDEO', TRUE, 300, 0, NULL),
    ('SQL ve Veritabanları', 'İlk Sorgular', 'SELECT', 'VIDEO', TRUE, 480, 0, NULL),
    ('Python 2 ile Programlama (eski)', 'Giriş', 'print ifadesi', 'VIDEO', TRUE, 240, 0, NULL)
) AS l (course_title, section_title, title, content_type, mandatory, duration, position, file_key)
JOIN courses co ON co.title = l.course_title
JOIN sections se ON se.course_id = co.id AND se.title = l.section_title
LEFT JOIN stored_files sf ON sf.storage_key = l.file_key;

-- ---------------------------------------------------------------------------
-- Soru bankası ve bölüm sınavları (Sıfırdan Python)
-- ---------------------------------------------------------------------------
INSERT INTO questions (course_id, text, points)
SELECT co.id, q.text, 10
FROM (VALUES
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?'),
    ('Python sürümünü terminalde hangi komut gösterir?'),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?'),
    ('return ifadesi ne yapar?')
) AS q (text)
JOIN courses co ON co.title = 'Sıfırdan Python';

INSERT INTO options (question_id, text, is_correct)
SELECT q.id, o.text, o.correct
FROM (VALUES
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()', TRUE),
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'echo()', FALSE),
    ('Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'write()', FALSE),
    ('Python sürümünü terminalde hangi komut gösterir?', 'python --version', TRUE),
    ('Python sürümünü terminalde hangi komut gösterir?', 'python --help', FALSE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'def', TRUE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'function', FALSE),
    ('Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'func', FALSE),
    ('return ifadesi ne yapar?', 'Fonksiyondan değer döndürür', TRUE),
    ('return ifadesi ne yapar?', 'Programı kapatır', FALSE)
) AS o (question, text, correct)
JOIN questions q ON q.text = o.question;

INSERT INTO quizzes (section_id, title, scoring_type, pass_threshold, required)
SELECT se.id, z.title, 'SINGLE_ANSWER', z.threshold, TRUE
FROM (VALUES
    ('Giriş', 'Giriş sınavı', 70),
    ('Fonksiyonlar', 'Fonksiyonlar sınavı', 60)
) AS z (section_title, title, threshold)
JOIN courses co ON co.title = 'Sıfırdan Python'
JOIN sections se ON se.course_id = co.id AND se.title = z.section_title;

INSERT INTO quiz_questions (quiz_id, question_id, position)
SELECT qz.id, q.id, x.position
FROM (VALUES
    ('Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 0),
    ('Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 1),
    ('Fonksiyonlar sınavı', 'Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 0),
    ('Fonksiyonlar sınavı', 'return ifadesi ne yapar?', 1)
) AS x (quiz_title, question, position)
JOIN quizzes qz ON qz.title = x.quiz_title
JOIN questions q ON q.text = x.question;

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
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 8)
) AS e (email, course_title, days_ago)
JOIN users u ON u.email = e.email
JOIN courses co ON co.title = e.course_title;

-- Tamamlanan dersler: Ayşe 6/6, Can 4/6, Elif 3/6, Burak 1/6, Deniz 0/6; Selin Java'da 1/2
INSERT INTO lesson_progress (enrollment_id, lesson_id, completed_at)
SELECT en.id, le.id, now() - make_interval(days => p.days_ago)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'Kurulum', 19),
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'İlk Program', 18),
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'Sayılar ve Metinler', 16),
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'Listeler ve Sözlükler', 14),
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'Fonksiyon Tanımlama', 12),
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 'Alıştırmalar', 11),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 'Kurulum', 14),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 'İlk Program', 13),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 'Sayılar ve Metinler', 9),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 'Listeler ve Sözlükler', 4),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 'Kurulum', 9),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 'İlk Program', 7),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 'Sayılar ve Metinler', 3),
    ('burak.celik@miniudemy.dev', 'Sıfırdan Python', 'Kurulum', 5),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 'Sınıf Nedir?', 6)
) AS p (email, course_title, lesson_title, days_ago)
JOIN users u ON u.email = p.email
JOIN courses co ON co.title = p.course_title
JOIN enrollments en ON en.student_id = u.id AND en.course_id = co.id
JOIN sections se ON se.course_id = co.id
JOIN lessons le ON le.section_id = se.id AND le.title = p.lesson_title;

-- İlerleme önbelleği ve son izlenen ders (yüzde = tamamlanan ders / toplam ders, iki ondalık)
INSERT INTO course_progress (enrollment_id, progress_percent, last_lesson_id, last_accessed_at)
SELECT en.id, c.percent, le.id, CASE WHEN c.lesson_title IS NULL THEN NULL ELSE now() - make_interval(days => c.days_ago) END
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 100.00, 'Alıştırmalar', 11),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 66.67, 'Fonksiyon Tanımlama', 1),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 50.00, 'Listeler ve Sözlükler', 2),
    ('burak.celik@miniudemy.dev', 'Sıfırdan Python', 16.67, 'İlk Program', 5),
    ('deniz.arslan@miniudemy.dev', 'Sıfırdan Python', 0.00, NULL, 0),
    ('ayse.yilmaz@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 0.00, NULL, 0),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 50.00, 'Kalıtım', 3)
) AS c (email, course_title, percent, lesson_title, days_ago)
JOIN users u ON u.email = c.email
JOIN courses co ON co.title = c.course_title
JOIN enrollments en ON en.student_id = u.id AND en.course_id = co.id
LEFT JOIN sections se ON se.course_id = co.id AND c.lesson_title IS NOT NULL
    AND EXISTS (SELECT 1 FROM lessons l2 WHERE l2.section_id = se.id AND l2.title = c.lesson_title)
LEFT JOIN lessons le ON le.section_id = se.id AND le.title = c.lesson_title;

-- ---------------------------------------------------------------------------
-- Sınav denemeleri: Ayşe ikisini de geçti; Can girişi geçti; Elif girişten kaldı
-- ---------------------------------------------------------------------------
INSERT INTO quiz_attempts (quiz_id, student_id, score, max_score, passed, submitted_at)
SELECT qz.id, u.id, a.score, 20, a.passed, now() - make_interval(days => a.days_ago)
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 20, TRUE, 17),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 20, TRUE, 11),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 20, TRUE, 12),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 10, FALSE, 6)
) AS a (email, quiz_title, score, passed, days_ago)
JOIN users u ON u.email = a.email
JOIN quizzes qz ON qz.title = a.quiz_title;

-- Cevaplar: geçenler doğru şıkları, Elif ilk soruda doğru ikinci soruda yanlış şıkkı seçti
INSERT INTO attempt_answers (attempt_id, question_id, option_id)
SELECT at.id, q.id, o.id
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('ayse.yilmaz@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --version'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?', 'def'),
    ('ayse.yilmaz@miniudemy.dev', 'Fonksiyonlar sınavı', 'return ifadesi ne yapar?', 'Fonksiyondan değer döndürür'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('can.demir@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --version'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Python''da ekrana yazı yazdıran fonksiyon hangisidir?', 'print()'),
    ('elif.sahin@miniudemy.dev', 'Giriş sınavı', 'Python sürümünü terminalde hangi komut gösterir?', 'python --help')
) AS x (email, quiz_title, question, option_text)
JOIN users u ON u.email = x.email
JOIN quizzes qz ON qz.title = x.quiz_title
JOIN quiz_attempts at ON at.quiz_id = qz.id AND at.student_id = u.id
JOIN questions q ON q.text = x.question
JOIN options o ON o.question_id = q.id AND o.text = x.option_text;

-- ---------------------------------------------------------------------------
-- Sertifika: Ayşe zorunlu derslerin ve zorunlu sınavların hepsini tamamladı
-- ---------------------------------------------------------------------------
INSERT INTO certificates (enrollment_id, verification_code, issued_at)
SELECT en.id, 'K7Q2-M9XA-4T', now() - make_interval(days => 11)
FROM enrollments en
JOIN users u ON u.id = en.student_id AND u.email = 'ayse.yilmaz@miniudemy.dev'
JOIN courses co ON co.id = en.course_id AND co.title = 'Sıfırdan Python';

-- ---------------------------------------------------------------------------
-- Değerlendirmeler
-- ---------------------------------------------------------------------------
INSERT INTO reviews (user_id, course_id, rating, comment)
SELECT u.id, co.id, r.rating, r.comment
FROM (VALUES
    ('ayse.yilmaz@miniudemy.dev', 'Sıfırdan Python', 5, 'Çok anlaşılır, örnekler tam yerinde.'),
    ('can.demir@miniudemy.dev', 'Sıfırdan Python', 4, 'Güzel kurs, video dersler biraz daha uzun olabilirdi.'),
    ('elif.sahin@miniudemy.dev', 'Sıfırdan Python', 4, NULL),
    ('selin.koc@miniudemy.dev', 'Java ile Nesne Yönelimli Programlama', 3, 'Kalıtım bölümü hızlı geçiyor.')
) AS r (email, course_title, rating, comment)
JOIN users u ON u.email = r.email
JOIN courses co ON co.title = r.course_title;

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
           '"' || co.title || '" kursunuz reddedildi. Neden: ' || co.rejection_reason,
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
           '/my-courses', ce.issued_at
    FROM certificates ce
    JOIN enrollments en ON en.id = ce.enrollment_id
    JOIN courses co ON co.id = en.course_id
) AS n;
