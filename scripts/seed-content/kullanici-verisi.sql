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
