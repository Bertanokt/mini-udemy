# Demo senaryosu (#70)

Proje dokümanındaki beş senaryonun ([use-cases.md](use-cases.md) › 1) sunumda adım adım gösterimi. Her adımda **ne gösterilir** ve **hangi mimari nokta devreye girer** yazılıdır. Veriler seed verisinden gelir (#67, README › Demo hesapları); bütün hesapların parolası `Demo1234!`.

> **Ekranı henüz olmayan adımlar** (19.11.2026): bölüm/ders düzenleme ve içerik yükleme ekranı, sertifika sayfası ve doğrulama (#130), bildirim listesi (#125). Bu adımlar aşağıda **API ile** (curl) verildi; ekranlar gelince ekran adımıyla değiştirilecek. Uçtan uca testleri olduğu için davranışları doğrulanmış durumda (her senaryonun sonunda test adı).

## Hazırlık

```bash
docker compose down -v
```

```bash
docker compose up --build
```

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
| 2 | Eğitmen: bölüm ve ders ekle, sırasını değiştir (API) | Yeni bölüm/ders sona eklenir; taşımada sıralar boşluksuz | Sıra kuralı entity'de (`Course`, `Section`); her işlemde "kurs düzenlenebilir mi?" kararını **State** (`CourseState`) verir |
| 3 | Eğitmen: derse MP4 yükle (`POST /api/lessons/{id}/content`, API). Örnek: "Sıfırdan Python" › "İlk Program" (yayımlanmış kursta içeriği olmayan derse yükleme izinli) | Dosya türü içerikten anlaşılır; yanlış tür 415, büyük dosya 413 | **Factory Method**: ders türünün fabrikası dosya kuralını ve sunuluş şeklini verir (`LessonContentFactories`) |
| 4 | Eğitmen: `/instructor/courses/{id}/questions` ve `/quizzes` | Soru bankası ve bölüm sınavı (eşik %70 varsayılan) | Sınav yalnız aynı kursun bankasındaki sorularla kurulur |
| 5 | Eğitmen: `/instructor` → "Onaya gönder" | Kurs **İncelemede**; artık düzenlenemez (409) | **State**: `DraftState.submit()` → `IN_REVIEW`; serviste durum için if/switch yok |
| 6 | Yönetici: `/admin` → "Kurs onayı" (`/admin/courses`, varsayılan filtre İncelemede) → "İncele" | Açıklama, hedefler, bölüm/ders listesi; içeriği olan ders "İçeriği aç" ile oynatıcıda yeni sekmede açılır, içeriği eksik dersler işaretli | Yayımlanmamış kurs yalnız yöneticiye ve sahibine görünür (`CourseVisibility`); içerik ucu yöneticiye açık, ilerleme yazılmaz (`ContentDeliveryService`) |
| 6b | Yönetici: "Onayla" → "Evet, onayla" | Kurs **Yayımlandı**, onay kuyruğundan düşer, katalogda görünür | **State** geçişi + **Observer**: `CourseApproved` olayı, commit'ten sonra `NotificationListener` eğitmene bildirim yazar |
| 6c | (alternatif) Yönetici: "Reddet" → gerekçe (5–1000 karakter) | Kurs Taslak'a döner; gerekçe eğitmen panelinde (`/instructor`). Gerekçesiz gönderimde alan hatası | `CourseRejected` olayı gerekçeyi taşır; kurs bu arada başka duruma geçtiyse 409 mesajı ve liste yenilenir |
| 6d | (isteğe bağlı) Yönetici: panel → "Şikâyetler" | Kursa ya da yoruma yapılmış şikâyetler, en yeni önce; "İncelendi" / "Reddet" ile durum değişir. Gerekirse kurs bu ekrandaki bağlantıdan yayından kaldırılır | Şikâyet `admin` modülünde; kurs ve yorumun varlığı `CourseLookupService` / `ReviewService` arayüzünden sorulur (modül sınırında id). Kullanıcı ekranında "Şikâyet et" düğmesi yok: şikâyet `POST /api/complaints` ile önceden oluşturulur |

Hazır örnek: seed'de "SQL ve Veritabanları" kursu İncelemede bekliyor; 6. adım doğrudan onunla gösterilebilir. Yayımlanmış bir kursu yönetici aynı ekranda "Yayımlandı" filtresinden gerekçeyle yayından kaldırır (FR-38). "Web Geliştirmeye Giriş" reddedilmiş haliyle (gerekçeli) Zeynep'in panelinde.

Test: `CoursePublishingScenarioIT`, `ContentUploadIT`, `ComplaintIT` (6d).

## Senaryo 2: Kursa kayıt ve öğrenme (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | Ziyaretçi: ana sayfa → katalogda "python" ara, kategori/seviye/puan filtresi | Yalnız yayımlanmış kurslar; "Python 2 (eski)" yok | Tek JPQL sorgusu, sayfalı (N+1 yok); katalog kartı DTO'su |
| 2 | "Sıfırdan Python" detay sayfası | Hedefler, bölüm/ders listesi, süre, 3 yorum (ortalama 4,3) | Herkese açık uç; yayımlanmamış kurs başkasına 404 |
| 3 | Yeni bir kursiyerle kayıt ol (`/register`) ve "Kaydol" | Kayıt; ikinci tıklamada 409 | **Observer**: `EnrollmentCreated` → eğitmene "yeni kayıt" bildirimi; tekillik veritabanı kısıtıyla |
| 4 | "Derse devam et" → `/learn/...` | PDF dersini aç; videoyu oynat ve ileri sar (Senaryo 1'de yüklenen) | İçerik ucu her istekte kayıt/sahiplik sorar (ADR-004); video için Range (206); medya etiketi için 5 dk'lık içerik token'ı (ADR-012) |
| 5 | Kayıtsız hesapla aynı ders adresi | "Bu derse erişmek için kursa kaydolun" (403) | Bölüm 6 kritik durumu |
| 6 | Dersi "Tamamlandı" işaretle | İlerleme çubuğu güncellenir | **Observer**: `LessonCompleted` → `ProgressListener` yüzdeyi commit'ten sonra günceller |

Test: `EnrollmentScenarioIT`, `Bolum6CriticalCasesIT`.

## Senaryo 3: Kaldığı yerden devam (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `can.demir@…` ile giriş → `/my-courses` | Python %66,67, son izlenen ders "Fonksiyon Tanımlama" | `course_progress` önbelleği; ders eklenip silinince `CourseLessonsChanged` ile yeniden hesaplanır |
| 2 | "Kaldığın yerden devam" | Oynatıcı doğrudan o derse açılır | `GET /api/me/courses/{id}/resume`: son açılan ders, yoksa ilk tamamlanmamış ders |
| 3 | Başka bir dersi aç, çık, tekrar gir | Son ders o derse dönmüş | **Observer**: içerik açılınca `LessonOpened` → `ProgressListener` |

Test: `ResumeScenarioIT`.

## Senaryo 4: Sınav ve sertifika (kursiyer)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `elif.sahin@…` ile giriş → Python "Giriş sınavı" (`/quizzes/{id}`) | Sorular doğru cevap bilgisi olmadan | Öğrenci görünümü DTO'su (`correct` alanı yok) |
| 2 | Yanlış cevapla gönder | "Kaldın", önceki denemeler listesinde | **Strategy**: puanlama türüne göre `ScoringStrategyResolver` stratejiyi seçer (tek doğru / hep ya hiç / kısmi) |
| 3 | Doğru cevapla tekrar gönder | Puan, yüzde, "Geçtin" | `QuizCompleted` olayı (**Observer**): bildirim + sertifika dinleyicisi |
| 4 | Sertifika (Ayşe'de var: `K7Q2-M9XA-4T`) | Zorunlu dersler + zorunlu sınavlar tamamsa otomatik, tek sertifika | `CertificateListener` → `CertificateIssuingService` (UNIQUE + `ON CONFLICT`); `CertificateIssued` → bildirim. Sekans diyagramı: `docs/diagrams/seq-certificate.png` |
| 5 | Herkese açık doğrulama `/api/certificates/verify/{code}` | — | **#130 gelince**; şimdilik sertifika veritabanında gösterilir |

Test: `QuizCertificateScenarioIT`, `CertificateScenarioIT`.

## Senaryo 5: Eğitmen analitiği (eğitmen)

| # | Adım | Gösterilen | Mimari nokta |
|---|---|---|---|
| 1 | `mehmet.kaya@…` → `/instructor` | Kurslar durumlarıyla; reddedilen kursta gerekçe | Eğitmen yalnız kendi kurslarını görür |
| 2 | "Sıfırdan Python" → "Analitik" (`/instructor/courses/{id}/analytics`) | 5 kayıt, ortalama ilerleme, tamamlanma oranı (%20), sınav başarı oranları, puan dağılımı | Veriler modüllerin servis arayüzlerinden (`CourseRoster`, `CourseProgressStats`, `CourseQuizStats`); modüller arası doğrudan repository erişimi yok (ArchUnit) |
| 3 | Kursiyer listesi | Her kursiyerin ilerlemesi, son erişimi, sınav sonucu | Sayfalı; sorgu sayısı kursiyer sayısından bağımsız |
| 4 | Yönetici: platform raporu (`GET /api/admin/reports`) | Rol/durum sayıları, tamamlanma, sertifika, en çok kayıt alan kurslar | Ekranı #147 |

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

- [ ] Temiz bir `docker compose up --build` ile baştan sona bir kez denendi (süre: … dk) — Docker'lı bir üye yapacak; sonuç #70'e yazılacak.
