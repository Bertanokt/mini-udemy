# Demo senaryosu (#70)

Proje dokümanındaki beş senaryonun ([use-cases.md](use-cases.md) › 1) sunumda adım adım gösterimi. Her adımda **ne gösterilir** ve **hangi mimari nokta devreye girer** yazılıdır. Veriler seed verisinden gelir (#67, README › Demo hesapları); bütün hesapların parolası `Demo1234!`.

> **Ekranı henüz olmayan adımlar** (21.11.2026): katalogda puan filtresi (#305; API'de `minRating` var), kullanıcının şikâyet oluşturması (düğme yok; `POST /api/complaints`). Bu adımlar aşağıda **API ile** verildi. Uçtan uca testleri olduğu için davranışları doğrulanmış durumda (her senaryonun sonunda test adı).
>
> **Sıra önerisi:** Senaryo 5'in sayıları seed'e göredir. Senaryo 2'de yeni bir kursiyer kaydolunca "Sıfırdan Python"da 6 kayıt ve %16,7 tamamlanma görünür. Ya Senaryo 5 Senaryo 2'den önce gösterilir ya da sayılar "seed'e göre" diye söylenir.

## Hazırlık

```bash
docker compose down -v
```

```bash
docker compose up --build
```

- **İlk build uzun sürer:** temiz makinede `docker compose up --build` yaklaşık 22 dk sürdü (Maven ve npm bağımlılıkları iniyor); önbellekle 18 sn (#68, #70). Sunum günü image'lar önceden build edilmeli.
- Frontend http://localhost:3000, backend http://localhost:8080. Boş veritabanında seed verisi açılışta yüklenir (log: "Demo verisi yüklendi").
- Üç tarayıcı penceresi (ya da gizli pencere) hazır olsun: **eğitmen** (`mehmet.kaya@miniudemy.dev`), **kursiyer** (`deniz.arslan@miniudemy.dev` — kaydı var, hiç ilerlemesi yok), **yönetici** (`admin@miniudemy.dev`).
- API adımları için token (her rol için bir kez):

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"email":"mehmet.kaya@miniudemy.dev","password":"Demo1234!"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
```

- Senaryo 1'de yüklenecek küçük bir MP4 dosyası (seed'de video dosyası yok).

---

## Senaryo 1: Kurs yayımlama (eğitmen → yönetici)

| # | Adım (kim / nerede) | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | Eğitmen: `/instructor` → "Yeni kurs": başlık, açıklama, kategori, seviye, öğrenme hedefleri, kapak (jpg/png/webp, en fazla 2 MB; seçince önizlenir) → "Kursu oluştur" | Kurs **Taslak** oluşur, eğitmene ait; düzenleme sayfası açılır. Boş alanlar alan altında hata verir; İncelemede/Yayımlandı kursun "Bilgiler" sayfası salt okunur | DTO + Bean Validation (`CreateCourseRequest`), MapStruct mapper; sahiplik `CourseOwnershipService`; düzenleme izni **State** (`assertEditable`, aksi 409); kapak türü içerikten denetlenir (415) |
| 2 | Eğitmen: kurs satırında ya da kurs formunda "Bölüm ve dersler" (`/instructor/courses/{id}/sections`) → bölüm ekle; bölümde "Ders ekle": başlık, içerik türü (video/PDF/doküman), süre, zorunlu işareti; ↑/↓ ile sırala | Yeni bölüm/ders sona eklenir; taşımada sıralar boşluksuz; silmeden önce onay sorulur. İncelemedeki kurs salt okunur, yayımlanmış kursta yalnız ders eklenir | Sıra kuralı entity'de (`Course`, `Section`); her işlemde "kurs düzenlenebilir mi?" kararını **State** (`CourseState`) verir, izin yoksa 409 mesajı gösterilir |
| 3 | Eğitmen: aynı ekranda dersin "İçerik yükle" alanından MP4 seç → ilerleme çubuğu → "Önizle" (video sayfada oynar, PDF yeni sekmede açılır). Örnek: "Sıfırdan Python" › "İlk Program" (yayımlanmış kursta içeriği olmayan derse yükleme izinli). Uzantısı mp4 olan sahte dosya ya da 200 MB'tan büyük video da denenebilir | Tür/boyut kuralı seçerken denetlenir; sunucu türü içerikten yeniden anlar: yanlış tür 415, büyük dosya 413, mesaj dersin altında | **Factory Method**: ders türünün fabrikası dosya kuralını ve sunuluş şeklini verir (`LessonContentFactories`); önizleme kursiyer oynatıcısıyla aynı içerik token'ını kullanır (ADR-012) |
| 4 | Eğitmen: `/instructor/courses/{id}/questions` ve `/quizzes` | Soru bankası ve bölüm sınavı (eşik %70 varsayılan) | Sınav yalnız aynı kursun bankasındaki sorularla kurulur |
| 5 | Eğitmen: `/instructor` → "Onaya gönder" | Kurs **İncelemede**; artık düzenlenemez (409) | **State**: `DraftState.submit()` → `IN_REVIEW`; serviste durum için if/switch yok |
| 6 | Yönetici: `/admin` → "Kurs onayı" (`/admin/courses`, varsayılan filtre İncelemede) → "İncele" | Açıklama, hedefler, bölüm/ders listesi; içeriği olan ders "İçeriği aç" ile oynatıcıda yeni sekmede açılır, içeriği eksik dersler işaretli | Yayımlanmamış kurs yalnız yöneticiye ve sahibine görünür (`CourseVisibility`); içerik ucu yöneticiye açık, ilerleme yazılmaz (`ContentDeliveryService`) |
| 6b | Yönetici: "Onayla" → "Evet, onayla" | Kurs **Yayımlandı**, onay kuyruğundan düşer, katalogda görünür | **State** geçişi + **Observer**: `CourseApproved` olayı, commit'ten sonra `NotificationListener` eğitmene bildirim yazar |
| 6c | (alternatif) Yönetici: "Reddet" → gerekçe (5–1000 karakter) | Kurs Taslak'a döner; gerekçe eğitmen panelinde (`/instructor`). Gerekçesiz gönderimde alan hatası | `CourseRejected` olayı gerekçeyi taşır; kurs bu arada başka duruma geçtiyse 409 mesajı ve liste yenilenir |
| 6d | (isteğe bağlı) Yönetici: panel → "Şikâyetler" | Kursa ya da yoruma yapılmış şikâyetler, en yeni önce; "İncelendi" / "Reddet" ile durum değişir. Gerekirse kurs bu ekrandaki bağlantıdan yayından kaldırılır | Şikâyet `admin` modülünde; kurs ve yorumun varlığı `CourseLookupService` / `ReviewService` arayüzünden sorulur (modül sınırında id). Kullanıcı ekranında "Şikâyet et" düğmesi yok; seed'de de şikâyet yok: göstermeden önce kursiyer token'ıyla bir tane oluşturun (komut tablonun altında) |

Hazır örnek: seed'de "SQL ve Veritabanları" kursu İncelemede bekliyor; 6. adım doğrudan onunla gösterilebilir. Yayımlanmış bir kursu yönetici aynı ekranda "Yayımlandı" filtresinden gerekçeyle yayından kaldırır (FR-38). "Web Geliştirmeye Giriş" reddedilmiş haliyle (gerekçeli) Zeynep'in panelinde.

6d için örnek şikâyet (önce kursiyer token'ı, örn. `deniz.arslan@…`; kurs id'si katalogdaki adresten):

```bash
curl -s -X POST localhost:8080/api/complaints -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"courseId": <kurs id>, "reason": "Ders videolarında ses yok."}'
```

Test: `CoursePublishingScenarioIT`, `ContentUploadIT`, `ComplaintIT` (6d).

## Senaryo 2: Kursa kayıt ve öğrenme (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | Ziyaretçi: ana sayfa → katalogda "python" ara, kategori ve seviye filtresi | Yalnız yayımlanmış kurslar; "Python 2 (eski)" yok. Puan filtresi ekranda yok (#305); API ile: `GET /api/courses?minRating=4` | Tek JPQL sorgusu, sayfalı (N+1 yok); katalog kartı DTO'su |
| 2 | "Sıfırdan Python" detay sayfası | Hedefler, bölüm/ders listesi, süre, 3 yorum (ortalama 4,3) | Herkese açık uç; yayımlanmamış kurs başkasına 404 |
| 2b | (ek) Yönetici: `/admin` → "Yorum moderasyonu" (`/admin/reviews`) → Ayşe'nin 5 puanlı yorumunda "Gizle" → gerekçe → detay sayfasını yenile; sonra "Gizli" filtresinden "Geri aç" | Yorum listeden kalkar, ortalama 4,3 → 4,0 (2 yorum); gerekçe yalnız yönetici listesinde. Geri açınca 4,3'e döner, gerekçe silinir. Gerekçesiz gizleme alan hatası verir | Gizli yorum sorgu düzeyinde süzülür (liste, özet, katalog kartı, `minRating`); `admin` → `review`'un servis arayüzü (`ReviewModerationService`); yalnız ADMIN (`/api/admin/**`) |
| 3 | Yeni bir kursiyerle kayıt ol (`/register`), kurs sayfasında "Kursa kayıt ol" | Kayıt; düğme "Derse başla"ya döner. Mükerrer kayıt ekranda denenemez; API'de ikinci istek 409 "Bu kursa zaten kayıtlısınız." | **Observer**: `EnrollmentCreated` → eğitmene "yeni kayıt" bildirimi; tekillik veritabanı kısıtıyla |
| 4 | "Derse başla" → `/learn/...` | PDF dersini aç; videoyu oynat ve ileri sar (Senaryo 1'de yüklenen) | İçerik ucu her istekte kayıt/sahiplik sorar (ADR-004); video için Range (206); medya etiketi için 5 dk'lık içerik token'ı (ADR-012) |
| 5 | Kayıtsız hesapla aynı ders adresi | "Bu derse erişmek için kursa kaydolun" (403) | Bölüm 6 kritik durumu |
| 6 | Ders içeriğinin altında "Tamamlandı olarak işaretle" | Düğme "✓ Tamamlandı" olur; sol listede ders tiklenir, bölüm ve kurs ilerleme çubukları güncellenir | **Observer**: `LessonCompleted` → `ProgressListener` yüzdeyi commit'ten sonra günceller; ekran tamamlama yanıtındaki güncel ilerlemeyi gösterir (#46) |

Test: `EnrollmentScenarioIT`, `Bolum6CriticalCasesIT`, `ReviewModerationIT`.

## Senaryo 3: Kaldığı yerden devam (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `can.demir@…` ile giriş → `/my-courses` | Python "Son ders: Fonksiyon Tanımlama · %67 tamamlandı" (ekran tam sayıya yuvarlar; kayıtlı değer %66,67) | `course_progress` önbelleği; ders eklenip silinince `CourseLessonsChanged` ile yeniden hesaplanır |
| 2 | "Kaldığın yerden devam et" (panelde ya da kurs sayfasında) | Oynatıcı doğrudan o derse açılır | `GET /api/me/courses/{id}/resume`: son açılan ders, yoksa ilk tamamlanmamış ders |
| 3 | Başka bir dersi aç, çık, tekrar gir | Son ders o derse dönmüş | **Observer**: içerik açılınca `LessonOpened` → `ProgressListener` |

Test: `ResumeScenarioIT`.

## Senaryo 4: Sınav ve sertifika (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `elif.sahin@…` ile giriş → "Kayıtlı kurslarım" → "Kaldığın yerden devam et" → oynatıcının ders listesinde "Giriş" bölümünün altındaki "📝 Giriş sınavı" (durumu "Denendi, geçilmedi") | Sorular doğru cevap bilgisi olmadan | Öğrenci görünümü DTO'su (`correct` alanı yok); sınav listesi yalnız kayıtlı kursiyere (`GET /api/sections/{id}/quizzes`, #304) |
| 2 | Yanlış cevapla gönder → "Tekrar dene" | "Kaldın"; soru bazında sonuç; "Tekrar dene" formu boş cevaplarla yeniden açar, denemeler "Deneme geçmişim"de | **Strategy**: puanlama türüne göre `ScoringStrategyResolver` stratejiyi seçer (tek doğru / hep ya hiç / kısmi) |
| 3 | Doğru cevapla tekrar gönder → "Kursa dön" | Puan, yüzde, "Geçtin"; oynatıcıda sınav "✓ Geçildi" | `QuizCompleted` olayı (**Observer**): bildirim + sertifika dinleyicisi |
| 4 | `ayse.yilmaz@…` → "Kayıtlı kurslarım" → "Sertifikalarım" (`/certificates`) → sertifikayı aç (`/certificates/{id}`) → "Yazdır" | Kursiyer adı, kurs, tarih, doğrulama kodu (`K7Q2-M9XA-4T`) ve doğrulama bağlantısı; "resmî akreditasyon değildir" ibaresi. Zorunlu dersler + zorunlu sınavlar tamamsa otomatik, tek sertifika. Başkasının sertifikası "bulunamadı" | `CertificateListener` → `CertificateIssuingService` (UNIQUE + `ON CONFLICT`); `CertificateIssued` → bildirim. Sekans diyagramı: `docs/diagrams/seq-certificate.png` |
| 5 | Giriş yapmadan (gizli pencere) sertifikadaki bağlantı: `/verify/K7Q2-M9XA-4T`; sonra kodu bozarak dene | "✓ Geçerli sertifika": yalnız kursiyer adı, kurs ve tarih. Geçersiz kodda "bulunamadı" | Herkese açık uç (`GET /api/certificates/verify/{code}`); e-posta ya da id dönmez, kod büyük/küçük harften bağımsız (#130). Başkasının sertifikası 404, çünkü 403 varlığını belli ederdi |

Bildirimler (Senaryo 1, 2, 4'te yazılır): üst menüdeki zil (#136). Alıcı hesapla giriş yapınca zilde okunmamış sayısı görünür (sayfa geçişinde, sekmeye dönünce ve 60 sn'de bir yenilenir). Zile tıklayınca en yeni 10 bildirim açılır. Bildirime tıklamak onu okundu işaretler ve bağlantısına gider, örneğin "Kursunuz onaylandı" → kurs sayfası, "Kursunuz reddedildi" → `/instructor`, "Yeni kayıt" → kurs istatistikleri. "Hepsini okundu işaretle" sayıyı sıfırlar. API: `GET /api/me/notifications`, `GET /api/me/notifications/unread-count`, `POST /api/me/notifications/{id}/read`, `POST /api/me/notifications/read-all`.

Test: `QuizCertificateScenarioIT`, `CertificateScenarioIT`, `NotificationScenarioIT`, `NotificationApiIT`.

## Senaryo 5: Eğitmen analitiği (eğitmen)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `mehmet.kaya@…` → `/instructor` | Kurslar durumlarıyla; yayından kaldırılan "Python 2 (eski)"de gerekçe. Reddedilen kurs ("Web Geliştirmeye Giriş") ve red gerekçesi Zeynep'in panelinde (`zeynep.ak@…`) | Eğitmen yalnız kendi kurslarını görür |
| 2 | "Sıfırdan Python" → "Analitik" (`/instructor/courses/{id}/analytics`) | Seed'e göre 5 kayıt ve tamamlanma oranı %20 (Senaryo 2'de yeni kayıt olduysa 6 ve %16,7); ortalama ilerleme, sınav başarı oranları, puan dağılımı | Veriler modüllerin servis arayüzlerinden (`CourseRoster`, `CourseProgressStats`, `CourseQuizStats`); modüller arası doğrudan repository erişimi yok (ArchUnit) |
| 3 | Kursiyer listesi | Her kursiyerin ilerlemesi, son erişimi, sınav sonucu | Sayfalı; sorgu sayısı kursiyer sayısından bağımsız |
| 4 | Yönetici: `/admin` → "Platform raporu" (`/admin/reports`) | Rol/durum sayıları, tamamlanma, sertifika, en çok kayıt alan kurslar | Sayılar modüllerin istatistik servislerinden (`admin` başka modülün tablosuna erişmez) |

Test: `InstructorAnalyticsIT`, `InstructorAnalyticsScenarioIT`, `PlatformReportIT`.

---

## Mimari noktaların özeti (sunumda tek slayt)

| Desen / ilke | Nerede görülür |
|---|---|
| Modüler monolit, katmanlar | Her modül yalnız başka modülün `service`/`dto`'sunu kullanır; ArchUnit testleri (#86) CI'da |
| **State** | Kurs durumları: onaya gönder, onay, red, yayından kaldır, düzenle/sil izinleri (Senaryo 1) |
| **Strategy** | Sınav puanlama (Senaryo 4) |
| **Factory Method** | Ders içerik türleri: dosya kuralı, sunuluş, Range (Senaryo 1–2) |
| **Observer** | 10 domain olayı; AFTER_COMMIT dinleyiciler: ilerleme, sertifika, bildirim, dosya temizliği (Senaryo 2–4) |
| DTO + Mapper | Bütün uçlar; entity dışarı çıkmaz |
| Güvenlik | JWT, rol × uç erişim matrisi testi (`AccessMatrixIT`), içerik token'ı (ADR-012) |

## Kontrol

- [x] Temiz bir `docker compose up --build` (`docker compose down -v` ile boş volume) ile beş senaryo ekranda baştan sona yüründü: Bertan, 21.11.2026, `main` @ `d445b44`; adım adım sonuç #70'teki yorumda. O gün eksik çıkan ekranlar (#46, #58, #120, #130, #131, #304, #296) aynı gün merge edildi; bu doküman onlara göre güncellendi.
- [ ] Sunumdan önce son hal bir kez daha yürünecek: demo provası (#106).
