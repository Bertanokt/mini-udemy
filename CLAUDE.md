# Mini Udemy — Claude Code kuralları

Bu repo bir Yazılım Tasarımı ve Mimarisi dersi projesi. Notun %70'i tasarım kalitesinden geliyor: kodun mimariye, diyagramlara ve OpenAPI sözleşmesine **uyması** en az çalışması kadar önemli. Ekip çalışma kuralları: `CONTRIBUTING.md`. Her issue için izlenecek akış: `docs/claude-code-workflow.md`.

Kapsamın tek kaynağı hocanın proje tanım dokümanıdır (modüller A–L, senaryo 1–5, bölüm 5–7). Issue'lar bu dokümana göre yazıldı; issue'nun kabul kriterleri dışına çıkma, çıkman gerekiyorsa dur ve sor.

## Komutlar

```bash
docker compose up -d postgres              # yalnız veritabanı
cd backend && ./mvnw spring-boot:run       # backend (dev profili)
cd backend && ./mvnw verify                # backend build + tüm testler (CI ile aynı)
cd frontend && npm run dev                 # frontend
cd frontend && npm run lint && npm run build   # CI ile aynı
docker compose up --build                  # her şey
```

PR açmadan önce ilgili iki kontrolün çıktısı yeşil olmalı; çıktıyı kullanıcıya göster.

## Modüller ve katmanlar

Backend `com.miniudemy.<modül>.<katman>` olarak paketlenir. Modüller ve kapsamları:

| Modül | Kapsam |
|---|---|
| `auth` | kayıt, giriş, JWT, parola yenileme, profil, roller |
| `course` | kategori, kurs, bölüm, ders, kurs durumları (State), katalog/arama, eğitmen istatistikleri |
| `content` | ders içerikleri ve dosyalar: yükleme, içerik türleri (Factory Method), depolama, yetki kontrollü servis |
| `enrollment` | kursa kayıt, kayıtlı kurslar, mükerrer kayıt engeli |
| `progress` | ders tamamlama, son izlenen ders, bölüm/kurs ilerleme yüzdesi |
| `quiz` | soru bankası, bölüm sınavları, puanlama (Strategy), eşik, deneme geçmişi |
| `certificate` | doğrulama kodlu sertifika |
| `review` | puan ve yorum |
| `notification` | uygulama içi bildirimler (olay dinleyicileri) |
| `admin` | onay/red/yayından kaldırma, kullanıcı/kategori yönetimi, şikâyet, raporlar |
| `common` | config, güvenlik, hata yönetimi, ortak tipler; iş kuralı içermez |

Katmanlar ve bağımlılık yönü: **controller → service → repository**, hepsi `domain`'i kullanabilir; `dto` ve `mapper` controller–service sınırındadır.

- Controller yalnız DTO alır/döner, iş kuralı içermez, repository'ye erişmez.
- İş kuralı ve `@Transactional` service katmanındadır.
- Repository yalnız Spring Data arayüzüdür.
- Domain (entity, enum, olay) diğer katmanlara bağımlı değildir.
- **Modüller arası:** bir modül başka modülün yalnız `service` paketindeki public arayüzünü ya da domain olaylarını kullanır. Başka modülün `repository`'sine ve entity'sine erişme; modül sınırında id ya da DTO taşı.
- Bu kurallar ArchUnit testleriyle CI'da kontrol edilir; testi geçmek için kuralı gevşetme, kodu düzelt.

## Tasarım desenleri — nerede, neden

Desenler bilinçli kullanılıyor; her birinin ADR'si `docs/adr/` altında. Deseni atlatan bir kısayol (servise `if/switch` eklemek gibi) yazma.

| Desen | Yer | Kural |
|---|---|---|
| State | `course` — `CourseState` ve durum sınıfları (Taslak / İncelemede / Yayımlandı / Yayından kaldırıldı) | Kurs durumuna bağlı her izin ve geçiş durum nesnesine sorulur; serviste `status` için if/switch yok. Red: İncelemede → Taslak, gerekçe kursta saklanır. |
| Observer | olaylar ilgili modülün `domain` paketinde; dinleyiciler `progress`, `certificate`, `notification` | Bir modülün işi başka modülü tetikleyecekse olay yayınla (`EnrollmentCreated`, `LessonPublished`, `LessonCompleted`, `CourseApproved`, `CourseRejected`, `QuizCompleted`, `CertificateIssued`); dinleyiciyi doğrudan çağırma. |
| Strategy | `quiz` — `ScoringStrategy` ve uygulamaları | Puanlama kuralı sınavın `scoringType`'ına göre seçilen stratejidedir; yeni kural = yeni strateji sınıfı. |
| Factory Method | `content` — `LessonContentFactory` ve tür fabrikaları (VIDEO, PDF, DOCUMENT) | Türe özgü biçim, boyut ve sunuluş kuralları türün fabrikası/ürünündedir; yükleme servisi yalnız ortak arayüzü bilir. |
| DTO + Mapper | her modülün `dto` ve `mapper` paketi | Entity API'ye çıkmaz; dönüşüm yalnız mapper'da. |

Desen sınıflarına dokunan bir değişiklikte sınıf diyagramını ve ilgili ADR'yi de güncelle.

## İsimlendirme

- Sınıflar: `CourseController`, `CourseService`, `CourseRepository`, `CourseMapper`; entity tekil (`Course`), enum `CourseStatus`.
- DTO'lar Java `record`: istek `CreateCourseRequest`, `UpdateCourseRequest`; yanıt `CourseResponse`, `CourseSummaryResponse`. Doğrulama anotasyonları istek DTO'sunda.
- Olaylar geçmiş zaman: `LessonCompleted`, `CertificateIssued`.
- REST: `/api/` altında, kaynak adları çoğul ve kebab-case (`/api/courses/{id}/sections`); kullanıcının kendi kaynakları `/api/me/...`, yönetici uçları `/api/admin/...`, eğitmen uçları `/api/instructor/...`.
- JSON alanları camelCase; tablo ve kolonlar snake_case, tablolar çoğul.
- Testler: unit `*Test`, entegrasyon (Testcontainers) `*IT`; test metodu davranışı anlatır: `enrollTwice_returnsConflict`.
- Frontend: bileşenler PascalCase; API çağrıları yalnız `src/lib/api/<modül>.ts` üzerinden (`apiFetch`), sayfalar `fetch`'i doğrudan çağırmaz.

## Standart hata formatı

Tüm hatalar `common/exception`'daki global handler'dan aynı gövdeyle döner:

```json
{ "status": 409, "message": "Bu kursa zaten kayıtlısınız.", "errors": { "alan": "mesaj" } }
```

- Controller'da elle hata yanıtı kurma; service'ten `common/exception`'daki istisnalardan birini fırlat: `BadRequestException` → 400 (alan doğrulamasıyla yakalanamayan iş kuralı), `ForbiddenException` → 403, `NotFoundException` → 404, `ConflictException` → 409, `PayloadTooLargeException` → 413 (dosya boyutu), `UnsupportedMediaTypeException` → 415 (dosya türü). `@Valid` ihlalleri kendiliğinden 400 + alan bazlı `errors` olur.
- Hata belirli bir alanla ilgiliyse alan hatasını da ver ki frontend mesajı o alanın altında göstersin: `throw new ConflictException("Bu e-posta adresi zaten kayıtlı.", Map.of("email", "Bu e-posta adresi zaten kayıtlı."))`.
- Bozuk JSON, yanlış tipte/eksik parametre (400), olmayan adres (404), yanlış HTTP yöntemi (405) ve beklenmeyen hatalar (500) handler'da zaten aynı gövdeye çevrilir; ayrıca bir şey yazma.
- 401 (token yok/geçersiz) ve güvenlik katmanının erişim reddi istek controller'a ulaşmadan oluşur; bunlar Spring Security yapılandırmasında aynı `ErrorResponse` gövdesiyle döner (#10).
- `message` kullanıcıya gösterilebilir Türkçe bir cümle; stack trace ya da iç ayrıntı içermez.
- Yeni bir hata türü gerekiyorsa `ApiException`'dan türeyen yeni bir sınıf yaz; handler değişmez. O durum kodu için `docs/api/openapi.yaml` › `components.responses`'a da bir yanıt ekle. Handler'a yalnız Spring'in yakalanmamış bir istisnası (şu an 500'e düşen) için yeni metot eklenir.

## Veritabanı ve Flyway

- Şema yalnız Flyway ile değişir (`ddl-auto=validate`).
- Dosya adı **zaman damgalı**: `V<yyyyMMddHHmm>__<aciklama>.sql`, örn. `V202610231415__create_courses.sql`. Zaman damgasını üret: `date +%Y%m%d%H%M`. Sıra numarası (V1, V2…) kullanma.
- Merge edilmiş migration'ı **asla değiştirme**; düzeltme yeni migration ile.
- Ayrıntı: `backend/src/main/resources/db/migration/README.md`.

## Test beklentisi

- Her service'in public metotları için unit test: başarılı yol + en az bir hata yolu (yetki, bulunamadı, çakışma).
- Issue'daki her kabul kriteri bir teste ya da açıkça yazılmış bir manuel kontrole karşılık gelir; PR'da kriter → test tablosu ver.
- Bir hatayı düzeltirken önce hatayı gösteren kırmızı testi yaz, sonra düzelt.
- Yeni testin **doğru sebeple** kırmızıya döndüğünü bir kez gör (kodu geçici bozarak); sadece yeşil görmek yetmez.
- Proje dokümanı Bölüm 6'nın dört durumu (kayıtsız erişim, mükerrer kayıt, yetkisiz düzenleme, yanlış ilerleme hesabı) ve dosya türü/boyut denetimleri her zaman testli kalmalı; bu testleri silme ya da `@Disabled` yapma.
- Entegrasyon testleri Testcontainers ile gerçek PostgreSQL'e karşı çalışır. Playwright ya da başka bir UI test aracı ekleme.

## Kod değişirse doküman da değişir

Aynı PR'da güncelle:

| Değişiklik | Güncellenecek |
|---|---|
| Uç eklendi/değişti (yol, parametre, şema, hata kodu) | `docs/api/openapi.yaml` |
| Tablo/kolon değişti | ER diyagramı (`docs/diagrams/er.*`) |
| Sınıf, desen sınıfı, ilişki değişti | sınıf diyagramı |
| Kayıt, sınav çözme, sertifika akışı değişti | ilgili sekans diyagramı |
| Modüller arası bağımlılık değişti | bileşen diyagramı + ArchUnit kuralı |
| Mimari bir karar alındı ya da değişti | yeni ya da güncellenmiş ADR |
| Yeni kütüphane eklendi | PR açıklamasında belirt (rapordaki kütüphane listesi için) |

Diyagramlar metin tabanlıdır (PlantUML/Mermaid) ve yanında render edilmiş görsel bulunur; ikisini birlikte güncelle.

## Git ve PR

- Bir issue (ya da aynı modülde birbirine bağlı küçük issue'lar) = bir branch = bir PR. Branch: `feature/#<no>-kisa-ad` ya da `fix/#<no>-kisa-ad`. zsh'ta adı tırnakla yaz: `git switch -c 'feature/#12-auth-register'`.
- Commit ve PR açıklamasında `Closes #<no>` (birden fazla için `Closes #a #b #c`).
- Commit'ler geliştiricinin kendi git kimliğiyle atılır; `git config user.email`'i değiştirme.
- **PR açıklamasındaki "Ne / Neden" bölümünü yazma.** Şablonu doldururken bu bölümü boş bırak ve kullanıcıdan kendi cümleleriyle yazmasını iste; kullanıcı yazmadan PR'ı açma. Diğer bölümleri (değişen dosyalar, test çıktısı, kriter → test tablosu, DoD) sen hazırlayabilirsin.
- `main`'e doğrudan push yok. Push ve `gh pr create` öncesi kullanıcıdan onay al.

## Yapma

- Issue kapsamı dışında başka modüllerin koduna dokunma; gerekiyorsa yeni issue öner.
- Gizli bilgi (parola, token, JWT secret) commit'leme; ortam değişkeni kullan.
- Test, ArchUnit kuralı ya da lint kuralını kapatarak/gevşeterek "yeşil" yapma.
- Yeni bağımlılığı sormadan ekleme.
- Dosya depolama için MinIO/S3 ya da dış servis ekleme (ADR-004: Docker volume + yetki kontrollü servis).
