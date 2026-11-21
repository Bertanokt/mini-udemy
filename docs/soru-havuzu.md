# Sunum soru havuzu

Hoca sunumda her üyeye bireysel teknik soru soracak ve katkı katsayısını buna göre belirleyecek (#107). Bu dosya prova için soru havuzudur: her modül, her desen, her ADR ve SOLID için en az iki soru, kısa bir cevap ve "kodu göster" denirse açılacak yer.

**Nasıl kullanılır:** biri soruyu sorar, diğeri dosyaya bakmadan cevaplar, sonra **Kodu göster** satırındaki dosyayı açıp gösterir. Herkes kendi yazmadığı modüllerden başlasın. ★ işaretli sorular zor ya da "neden başka türlü yapmadınız?" türündendir.

**Yollar:** `Kodu göster` satırındaki Java yolları `backend/src/main/java/com/miniudemy/` altına, test yolları `backend/src/test/java/com/miniudemy/` altına göredir. Diğerleri repo köküne göre.

**Kaynaklar:** [`architecture.md`](architecture.md), [`solid.md`](solid.md), [`adr/`](adr/README.md), [`test-plan.md`](test-plan.md), `CLAUDE.md`. Cevap bu dokümanlarla çelişirse doğru olan kod ve o dokümanlardır; bu dosya düzeltilir.

Veri tarihi: 21.11.2026, `main` @ `2c0996e`.

---

## 1. Genel mimari ve katmanlar

**G1. Sistemin mimari stili ne?**

Katmanlı, modüler monolit: tek Spring Boot uygulaması, tek PostgreSQL. İçeride 10 iş modülü ve `common`; her modül `controller → service → repository` katmanlarına ayrılır, hepsi `domain`'i kullanabilir, `dto` ve `mapper` controller–service sınırındadır.

Kodu göster: paket yapısı (`course/controller`, `course/service` …), `docs/architecture.md` › 1–3.

**G2. Katman kurallarına uyulduğunu nasıl garanti ediyorsunuz?**

ArchUnit testleri CI'da çalışır; ihlal build'i kırar. `LayerArchitectureTest` katman yönünü (controller repository'ye erişemez, entity döndüremez), `ModuleArchitectureTest` modüller arası izinli bağımlılıkları ve döngü yokluğunu, `ObserverArchitectureTest` service'in dinleyiciyi çağırmadığını denetler. Testi geçmek için kural gevşetilmez, kod düzeltilir.

Kodu göster: `common/architecture/ModuleArchitectureTest.java` (test) › `ALLOWED`.

**G3. ★ Bir modül başka bir modülü nasıl kullanır? Neyi kullanamaz?**

Yalnız iki yol: diğer modülün `service` paketindeki public sınıflar ya da domain olayları. Başka modülün `repository`'si ve entity'si kullanılamaz; sınırda yalnız id ya da DTO taşınır. Başka modülün `domain`'inden yalnız olay record'ları ve enum'lar alınabilir (örn. `admin`, `CourseStatus`'u filtre olarak alır). Veritabanında modüller arası yabancı anahtar olabilir, ama `@ManyToOne` gibi JPA ilişkisi yalnız aynı modül içinde.

Kodu göster: `docs/architecture.md` › 4, bağımlılık tablosu.

**G4. ★ Neden mikroservis değil?**

Dört kişilik ekip ve beş haftalık sürede dağıtım, servisler arası iletişim ve dağıtık veri tutarlılığının (örn. kayıt ile ilerleme arasında) maliyeti fayda getirmiyor; ayrı ölçekleme ihtiyacı yok, proje dokümanı da monoliti öneriyor. Modül sınırlarını net ve testli tuttuğumuz için ileride bir modül ayrılabilir. (ADR-001)

**G5. Bir isteğin yolculuğunu anlat: kursa kayıt.**

`EnrollmentController` isteği alır, rolü `@PreAuthorize` ile kontrol edilir, kullanıcı id'sini `CurrentUser.id(...)` ile alıp `EnrollmentService.enroll`'a verir. Service kursun yayımlanmış olduğunu `course` modülünün `CourseLookupService.assertPublished`'ına sorar (değilse 404), mükerrer kaydı kontrol eder (409), kaydı yazar ve `EnrollmentCreated` yayınlar. Commit'ten sonra `NotificationListener` eğitmene bildirim yazar. Yanıt mapper ile DTO'ya çevrilir.

Kodu göster: `enrollment/service/EnrollmentService.java`, sekans diyagramı `docs/diagrams/seq-enroll.*`.

---

## 2. Modüller

### auth

**M-auth-1. Parolalar nasıl saklanıyor?**

BCrypt özeti olarak; düz parola hiçbir yerde saklanmaz ya da loglanmaz (NFR-04). Parola 8–72 karakter; üst sınır BCrypt'in 72 bayt sınırından gelir.

Kodu göster: `auth/domain/User.java` (`passwordHash`), `auth/service/AuthService.java`.

**M-auth-2. ★ Parola yenileme nasıl çalışıyor, güvenli mi?**

İstek geldiğinde e-posta kayıtlı ve hesap aktifse kullanıcının eski kullanılmamış token'ları geçersiz kılınır, 32 rastgele bayttan yeni token üretilir. Veritabanına token'ın kendisi değil SHA-256 özeti yazılır; süre varsayılan 30 dakika, token tek kullanımlıktır (koşullu güncelleme ile, iki eşzamanlı istekten yalnız biri geçer). Kayıtlı olmayan e-postaya da aynı 202 döner, böylece hangi e-postanın kayıtlı olduğu sızmaz. E-posta gönderimi kapsam dışı: dev profilinde bağlantı log'a yazılır (`LoggingPasswordResetNotifier`), diğer profillerde hiçbir yere yazılmaz.

Kodu göster: `auth/service/PasswordResetService.java`, `auth/service/PasswordResetNotifier.java`.

**M-auth-3. Rol nereden okunuyor, token'dan mı?**

Hayır, her istekte veritabanından. Token'ın imzası ve süresi geçerliyse `JwtAccountAuthenticationConverter`, `AccountLookup` üzerinden kullanıcının güncel durumunu sorar: silinmişse 401, pasifse 403, rol de veritabanındaki rol. Böylece rolü düşürülen ya da pasifleştirilen kullanıcının eski token'ı işe yaramaz.

Kodu göster: `common/security/JwtAccountAuthenticationConverter.java`, `auth/service/AccountLookupService.java`.

### course

**M-course-1. Kursun durumları ve geçişleri neler?**

Taslak → (onaya gönder) İncelemede → (onay) Yayımlandı → (yayından kaldır) Yayından kaldırıldı → (onaya gönder) İncelemede. Red İncelemede'den Taslak'a döndürür ve gerekçe kursta saklanır. İzinsiz geçiş 409. Katalogda yalnız Yayımlandı görünür.

Kodu göster: `course/domain/CourseState.java`, geçiş tablosu `docs/architecture.md` › Kurs durumları.

**M-course-2. ★ Eğitmen istatistikleri başka modüllerin verisini istiyor; `course` onlara bağımlı olunca döngü olmuyor mu?**

Olurdu; bu yüzden bağımlılık tersine çevrildi. `course`, ihtiyaç duyduğu dar sorgu arayüzlerini kendi `service` paketinde tanımlar (`EnrollmentCounts`, `CourseRoster`, `CourseProgressStats`, `CourseQuizStats`, `CourseRatings`); `enrollment`, `progress`, `quiz` ve `review` bunları uygular. Yön hep "diğer modül → `course`" kalır (DIP).

Kodu göster: `course/service/CourseAnalyticsService.java`, `progress/service/CourseProgressStatsService.java`.

**M-course-3. Yayından kaldırılan kursa kayıtlı kursiyer ne yaşar?**

Kurs katalogda görünmez ve yeni kayıt almaz (404), ama önceden kaydolan kursiyer detayı, bölümleri ve içeriği görmeye devam eder; ilerlemesi, sınavları ve sertifikası etkilenmez. Görünürlük kuralı tek yerde: `CourseVisibility` (sahip, yönetici ve kayıtlı kursiyer görür, diğerlerine 404).

Kodu göster: `course/service/CourseVisibility.java`, test `integration/CourseUnpublishIT.java`.

### content

**M-content-1. Yüklenen dosyanın türünü nasıl anlıyorsunuz?**

Dosyanın içeriğinden, ilk baytlarına (magic bytes) bakarak; uzantıya ve istemcinin gönderdiği `Content-Type`'a bakılmaz. Örneğin PDF `%PDF-` ile başlar, DOCX/PPTX bir zip ve içinde `word/document.xml` / `ppt/presentation.xml` olmalı. Uzantısı `.mp4` yapılmış bir çalıştırılabilir dosya reddedilir (415).

Kodu göster: `content/service/FileFormatDetector.java`.

**M-content-2. Kayıtsız biri ders videosunun adresini bulursa izleyebilir mi?**

Hayır. Dosyalar diskte UUID adla durur ve dışarıya doğrudan açılmaz; içerik yalnız `GET /api/lessons/{id}/content` ucundan, her istekte yetki kontrolüyle verilir: kayıtlı kursiyer, kursun eğitmeni ya da yönetici, diğerleri 403. Bu Bölüm 6'nın "kayıtsız erişim" testidir.

Kodu göster: `content/service/ContentDeliveryService.java`, test `integration/Bolum6CriticalCasesIT.java`.

**M-content-3. Video oynatıcı uzun videoyu nasıl oynatıyor?**

HTTP `Range` istekleriyle (206 Partial Content); dosya belleğe alınmadan akışla gönderilir (NFR-02). `<video>` etiketi `Authorization` başlığı gönderemediği için adres 5 dakikalık, derse özel bir içerik token'ı taşır (ADR-012); süre dolarsa oynatıcı yeni token alıp kaldığı saniyeden devam eder.

### enrollment

**M-enrollment-1. Mükerrer kaydı nasıl engelliyorsunuz?**

İki katmanlı: service önce kaydın var olup olmadığına bakar (409). İki istek aynı anda gelirse veritabanındaki `UNIQUE (student_id, course_id)` kısıtı ikincisini reddeder ve o da 409'a çevrilir. Bu Bölüm 6'nın "mükerrer kayıt" testidir.

Kodu göster: `enrollment/service/EnrollmentService.java` › `enroll`.

**M-enrollment-2. Başka modüller "bu kursiyer kayıtlı mı?" sorusunu nereden soruyor?**

Yalnız `EnrollmentService`'ten (`requireEnrollmentId`, kayıtlı değilse 403 "Bu kursa kayıtlı değilsiniz."). Kayıt kuralı tek yerde durur; `progress`, `quiz`, `content`, `review` aynı metodu kullanır.

### progress

**M-progress-1. İlerleme yüzdesi nasıl hesaplanıyor?**

Tamamlanan ders sayısı / kurstaki toplam ders × 100, iki ondalığa yarım yukarı yuvarlanır (20 dersin 15'i 75.00; 3'ün 1'i 33.33). Dersi olmayan kurs ya da bölümde %0, sıfıra bölme yok; yalnız hâlâ var olan dersler sayılır. Bu Bölüm 6'nın "yanlış ilerleme hesabı" testidir.

Kodu göster: `progress/service/ProgressService.java`, `docs/architecture.md` › İlerleme yüzdesi.

**M-progress-2. ★ Kurs yüzdesi saklanıyor mu, hesaplanıyor mu?**

İkisi de: ayrıntı ekranında her istekte güncel ders yapısından hesaplanır; `course_progress.progress_percent` yalnız liste ekranları için önbellektir. Önbellek `LessonCompleted` ile, kursun ders sayısı değişince de `CourseLessonsChanged` olayıyla (`recalculateCourse`) güncellenir.

**M-progress-3. "Kaldığın yerden devam et" hangi dersi açar?**

Son açılan dersi; o ders yoksa (hiç açılmamış ya da silinmiş) ilk tamamlanmamış dersi; hepsi tamamlanmışsa kursun ilk dersini. Hiç ders açılmamışsa düğmenin metni "Derse başla" olur. Son açılan ders, içerik açılınca yayınlanan `LessonOpened` olayıyla yazılır.

Kodu göster: `ProgressService.getMyResumePoint`.

### quiz

**M-quiz-1. Sınav nasıl puanlanıyor ve geçme nasıl belirleniyor?**

Her soru, sınavın `scoringType`'ına göre seçilen stratejiyle puanlanır (Strategy). Yüzde, sınavın başarı eşiğine (`passThreshold`) eşit ya da büyükse geçti; yüzde yuvarlanmadan karşılaştırılır. "Geçti" bilgisi denemenin kendisinde saklanır, yani eşik sonradan değişse de eski deneme değişmez.

Kodu göster: `quiz/service/QuizAttemptService.java` › `submit`.

**M-quiz-2. Kalan kursiyer tekrar girebilir mi? Geçmişini görebilir mi?**

Evet, deneme sayısı sınırsız; her deneme ve cevapları kaydedilir, kursiyer kendi denemelerini en yeni önce görür. Kursa kayıtlı olmayan 403.

### certificate

**M-certificate-1. Sertifika ne zaman ve nasıl veriliyor?**

Kursun bütün zorunlu dersleri tamamlanmış ve zorunlu sınavların her birinin en az bir geçen denemesi varsa. Kontrolü `CertificateListener`, `LessonCompleted` ve geçen `QuizCompleted` olaylarında commit'ten sonra yapar; kursiyer bir şeye tıklamaz.

Kodu göster: `certificate/service/CertificateIssuingService.java`, sekans diyagramı `docs/diagrams/seq-certificate.*`.

**M-certificate-2. ★ Son ders ve son sınav aynı anda biterse iki sertifika oluşur mu?**

Hayır. `certificates.enrollment_id` tekil ve ekleme `ON CONFLICT DO NOTHING` ile tek SQL komutu: biri yazar, diğeri hiçbir şey yapmaz; `CertificateIssued` de yalnız yazan tarafta bir kez yayınlanır.

**M-certificate-3. Doğrulama kodu tahmin edilebilir mi?**

`XXXX-XXXX-XX` biçiminde, `SecureRandom` ile 31 karakterlik okunaklı alfabeden (0/O, 1/I/L yok) 10 karakter, yaklaşık 49 bit. Doğrulama ucu herkese açık ama yalnız kursiyer adı, kurs adı, tarih ve geçerliliği döner; e-posta yok.

Kodu göster: `certificate/service/VerificationCodeGenerator.java`.

### review

**M-review-1. Kim değerlendirme yapabilir, kaç kez?**

Yalnız kursa kayıtlı kursiyer (kayıtsız 403), kurs başına bir kez (ikincisi 409; eşzamanlı istekte `UNIQUE (user_id, course_id)` kısıtı). Puan 1–5; dışı 400. Kendi değerlendirmesini düzenleyebilir, başkasınınkini 403.

Kodu göster: `review/service/ReviewService.java`.

**M-review-2. Yönetici bir yorumu gizlerse ne olur?**

Yorum herkese açık listeden ve ortalama puandan çıkar; gizleme gerekçesi zorunlu. Gizli yorum şikâyet de edilemez (404), çünkü kullanıcı onu göremez.

Kodu göster: `review/service/ReviewModerationService.java`.

### notification

**M-notification-1. Hangi olaylar bildirim üretiyor?**

Altı olay: kursa kayıt → eğitmene; yayımlanmış kursa yeni ders → kayıtlı kursiyerlere; kurs onayı ve reddi (gerekçeyle) → eğitmene; sınav sonucu → kursiyere; sertifika → kursiyere. Hepsini `NotificationListener` dinler; metinler tek yerde, `NotificationMessages`'ta.

Kodu göster: `notification/listener/NotificationListener.java`.

**M-notification-2. ★ Bildirim yazılırken hata olursa kursa kayıt geri alınır mı?**

Hayır. Dinleyici `AFTER_COMMIT` ile kayıt işlemi commit edildikten sonra ve `REQUIRES_NEW` ile kendi işleminde çalışır; hatası kaydı geri almaz. Tersi de geçerli: kayıt başarısız olursa (örn. 409) olay hiç teslim edilmez, bildirim de olmaz.

### admin

**M-admin-1. `admin` modülü kullanıcıları nasıl yönetiyor? `users` tablosuna erişiyor mu?**

Hayır; `users`'a erişim `auth`'un `UserAccountService`'indedir, `admin` yalnız yönetici kuralını uygular (örn. yönetici kendi hesabını pasifleştiremez). Platform raporu da verileri yalnız modüllerin service sınıflarından alır; `admin` hiçbir modülün repository'sine dokunmaz.

Kodu göster: `admin/service/AdminUserService.java`, `admin/service/PlatformReportService.java`.

**M-admin-2. Şikâyet akışı nasıl?**

Kullanıcı bir kursu ya da görünür bir yorumu şikâyet eder (hedef tam biri olmalı, yoksa 400). Yönetici listeyi görür ve şikâyeti "incelendi" ya da "reddedildi" yapar; yorumu gizlemek ya da kursu yayından kaldırmak ayrı uçlardır, şikâyet servisinin işi değildir.

Kodu göster: `admin/service/ComplaintService.java`, `admin/domain/Complaint.java` › `resolve`.

### common

**M-common-1. `common`'da ne var, ne yok?**

Güvenlik altyapısı (JWT doğrulama, herkese açık uç listesi, 401/403 gövdeleri), global hata yönetimi, MapStruct ortak ayarı ve config. İş kuralı yok ve hiçbir modüle bağımlı değil; bu ArchUnit ile denetlenir.

**M-common-2. ★ Güvenlik filtresi kullanıcının aktif olup olmadığını `auth`'a bağımlı olmadan nasıl biliyor?**

`common`, `AccountLookup` arayüzünü tanımlar; `auth/service/AccountLookupService` uygular. Bağımlılık yönü `auth → common` kalır (DIP).

Kodu göster: `common/security/AccountLookup.java`.

---

## 3. Tasarım desenleri

**D-State-1. State'i nerede ve neden kullandınız?**

Kurs durumunda. Dört durumun her biri düzenleme, silme, onaya gönderme, onay, red ve yayından kaldırma için farklı kural koyuyor. Her durum kendi sınıfında (`DraftState`, `InReviewState`, `PublishedState`, `UnpublishedState`); servis `course.submit()` gibi çağırır, durum için if/switch yazmaz.

Kodu göster: `course/domain/CourseState.java`, `course/domain/Course.java`.

**D-State-2. ★ Yeni bir işlem eklenince bir durumda izin vermeyi unutursak ne olur?**

Güvenli tarafta kalır: `CourseState` arayüzündeki her işlemin varsayılan gövdesi 409 (`CourseStateException`) fırlatır, her durum yalnız izin verdiği işlemleri ezer. Ayrıca `CourseState` `sealed`; dört durumdan başka uygulama yazılamaz. Her `CourseStatus` sabiti kendi durum nesnesiyle kurulur, durum nesnesi olmayan sabit derlenmez.

**D-Observer-1. Observer'ı nerede kullandınız, neden?**

Bir modülün işi başka modüllerde tepki doğurduğunda: kayıt, ders tamamlama, sınav, sertifika, kurs onayı… Yayıncı dinleyicileri tanımaz; yeni tepki yeni bir dinleyici sınıfıdır. Spring `ApplicationEventPublisher` ve `@TransactionalEventListener(AFTER_COMMIT)` ile; 10 olay var.

Kodu göster: `docs/architecture.md` › Domain olayları, `progress/listener/ProgressListener.java`.

**D-Observer-2. ★ Neden düz `@EventListener` değil de `AFTER_COMMIT`?**

Düz dinleyici olay yayınlandığı anda, işlem bitmeden çalışır; yayıncı sonra rollback olursa hiç olmamış bir kayıt için bildirim ya da ilerleme yazılmış olur. `AFTER_COMMIT` yalnız commit'ten sonra teslim eder. Dinleyici veritabanına yazacaksa `REQUIRES_NEW` ile kendi işlemini açar.

**D-Strategy-1. Strategy nerede?**

Sınav puanlamasında. `ScoringStrategy` arayüzü ve üç uygulaması: `SingleAnswerScoring`, `MultiAnswerAllOrNothing`, `MultiAnswerPartialScoring`. `ScoringStrategyResolver` sınavın `ScoringType`'ına göre stratejiyi bir `EnumMap`'ten, if/switch olmadan seçer.

Kodu göster: `quiz/service/ScoringStrategyResolver.java`.

**D-Strategy-2. Kısmi puanlama nasıl hesaplanıyor?**

P × max(0, doğru seçilen − yanlış seçilen) / doğru şık sayısı; iki ondalığa yarım yukarı. Her yanlış seçim bir doğrunun kazandırdığını geri alır, puan 0'ın altına inmez. Puanlar `BigDecimal`; 10 puanlık soruda 3 doğrunun 1'i 3,33 olur.

Kodu göster: `quiz/service/MultiAnswerPartialScoring.java`.

**D-Factory-1. Factory Method nerede?**

Ders içerik türlerinde. Soyut yaratıcı `LessonContentFactory`, factory method'u `createContent()`; alt sınıflar `VideoContentFactory`, `PdfContentFactory`, `DocumentContentFactory` kendi ürününü (`VideoContent`, `PdfContent`, `DocumentContent`) üretir. Ürün türe özgü kuralları verir: kabul edilen biçimler ve boyut sınırı (`filePolicy()`), sunuluş (`deliveryMode()`), `Range` desteği.

Kodu göster: `content/service/LessonContentFactory.java`, `content/domain/LessonContent.java`.

**D-Factory-2. ★ Ses dersi eklemek istesek neyi değiştiririz?**

Yeni bir ürün (`AudioContent`), yeni bir fabrika (`AudioContentFactory`) ve `LessonContentType` enum'una bir değer. Yükleme ve sunum servisleri değişmez. Fabrika eksik ya da iki fabrika aynı türü iddia ederse `LessonContentFactories` açılışta hata verir, uygulama başlamaz. Ama tamamen kapalı değil: `lessons.content_type`'taki `ck_lessons_content_type` CHECK kısıtını genişleten bir migration ve sözleşmedeki enum da güncellenir.

**D-Mapper-1. DTO + Mapper neden?**

Entity API'ye çıkmasın: parola özeti gibi alanlar sızmasın, API sözleşmesi veritabanı şemasından bağımsız değişebilsin. DTO'lar Java `record`, mapper'lar MapStruct arayüzleri.

Kodu göster: `auth/mapper/UserMapper.java`, `auth/dto/UserResponse.java`.

**D-Mapper-2. ★ DTO'ya alan ekleyip mapper'da unutursak ne olur?**

Derleme durur: ortak ayar `MapStructConfig`'te `unmappedTargetPolicy = ERROR`. Bir alan bilerek boş bırakılacaksa `@Mapping(target = "…", ignore = true)` açıkça yazılır.

Kodu göster: `common/mapper/MapStructConfig.java`.

---

## 4. ADR'ler

**A-001-1. ADR-001 neyi karara bağlıyor?** Katmanlı modüler monolit; önce modüle sonra katmana paketleme (`com.miniudemy.<modül>.<katman>`), tek PostgreSQL.

**A-001-2. ★ Klasik katmanlı monolitten (`controller/`, `service/` en üstte) farkı ne, neden seçilmedi?** Klasikte bir modülün kodu tüm projeye dağılıyor; modül sınırları ve modüller arası bağımlılıklar paket yapısından okunamıyor, testle korunamıyor.

**A-002-1. Kimlik doğrulama nasıl?** Sunucuda oturum yok; HS256 imzalı, 1 saat geçerli JWT, `Authorization: Bearer` başlığında. Refresh token yok, süre dolunca yeniden giriş. Anahtar `JWT_SECRET` ortam değişkeninden; tanımsız ya da 32 bayttan kısaysa uygulama açılmaz.

**A-002-2. ★ Token `localStorage`'da; XSS riski yok mu?** Var, bilerek kabul edildi. Önlemler: React içeriği kaçışlayarak basar, `dangerouslySetInnerHTML` ESLint ile yasak (`react/no-danger`), CSP dış betiği engeller, token 1 saat geçerli, çıkışta ve 401'de silinir. httpOnly çerez CSRF korumasını, CORS'ta `allowCredentials`'ı ve sözleşme değişikliğini gerektirdiği için seçilmedi.

**A-002-3. Neden CSRF koruması kapalı?** Token çerezde değil başlıkta; tarayıcı onu başka siteden gelen isteğe kendiliğinden eklemiyor.

**A-003-1. Şema nasıl değişiyor?** Yalnız Flyway SQL migration'larıyla; Hibernate yalnız doğrular (`ddl-auto=validate`). Merge edilmiş migration değişmez, düzeltme yeni migration ile.

**A-003-2. ★ Neden migration adında V1, V2 değil zaman damgası?** Dört kişi paralel çalışırken iki PR aynı numarayı alıp çakışıyordu; zaman damgası (`V<yyyyMMddHHmm>__…`) bunu pratikte ortadan kaldırıyor. Bedeli: dev profilinde `out-of-order: true`.

**A-003-3. `ddl-auto=update` neden olmaz?** Kolon silmez, ad değişikliğini bilmez, kısıt ve indeksleri eksik üretir; şemanın hangi değişiklikle geldiği kaydı olmaz.

**A-004-1. Dosyalar nerede?** Backend konteynerine bağlı Docker volume'da, yerel diskte; diskteki adı sunucu üretir (UUID), meta veri `stored_files`'ta. Yalnız backend üzerinden, her istekte yetki kontrolüyle sunulur.

**A-004-2. ★ Neden S3/MinIO ya da nginx değil?** S3 hesap ve ücret ister, "temiz makinede tek komut" kuralına uymaz; MinIO Compose'a servis ve anahtar ekler. İkisinin imzalı adresi süresi dolana kadar paylaşılabilir; "her istekte kayıt kontrolü" (NFR-03) zayıflar. nginx dosyayı adresi bilen herkese verir (FR-06 ihlali).

**A-005-1. Frontend backend'le nasıl konuşuyor?** Ayrı bir Next.js uygulaması, REST + JSON. Tarayıcı backend'e `NEXT_PUBLIC_API_URL` ile gider, backend kökeni CORS ile izinler. Sayfalar `fetch`'i doğrudan çağırmaz; istekler `src/lib/api/client.ts`'teki `apiFetch` üzerinden.

**A-005-2. ★ Neden sözleşme önce (`openapi.yaml`), springdoc ile koddan üretmek değil?** Sözleşme kod yazılmadan önce var olunca frontend ve backend aynı anda başlayabiliyor; uç tasarımı (yol, alanlar, hata kodları) review'da kod ayrıntısına karışmıyor. Bedeli: sözleşme ile kodu aynı PR'da elle eşit tutmak (NFR-15).

**A-005-3. Neden GraphQL değil?** Hata modelimiz ve gereksinimlerimiz HTTP durum kodlarına dayanıyor (401, 403, 404, 409, 413, 415); GraphQL çoğunlukla 200 döner, dosya yükleme ve `Range` ile video akışı da onun doğal alanı değil.

**A-006-1. Mapper için hangi kütüphane, neden?** MapStruct: dönüşüm kodunu derleme sırasında üretir, eksik eşleme derleme hatasıdır.

**A-006-2. ★ ModelMapper ya da elle mapper neden olmaz?** ModelMapper çalışma anında yansımayla eşler; hata ancak kod çalışınca görülür. Elle mapper'da yeni alana kolayca `null` geçilip derlenir; tekrar eden kod ve testi de birikir.

**A-007-1. CSP politikası ne?** `default-src 'self'`; `script-src 'self' 'unsafe-inline'`; `connect-src`, `img-src`, `media-src` yalnız site ve API; `object-src 'none'`; `frame-ancestors 'none'` (clickjacking).

**A-007-2. ★ `unsafe-inline` var; CSP o zaman ne işe yarıyor?** Asıl XSS koruması React'in kaçışlaması ve `react/no-danger`. CSP ikinci katman: dış kaynaktan betiği ve başka siteye veri gönderilmesini (`connect-src`) engelliyor; bu 17.11'de denendi. Nonce'suz `script-src 'self'` Next.js'in satır içi betiklerini engelleyip uygulamayı çalışmaz yaptı; nonce ise statik optimizasyonu kapatıyordu.

**A-008-1. ADR-008 neyi karara bağlıyor?** Puanlamanın Strategy ile yazılmasını; stratejiler entity değil `ScoringInput`/`QuestionScore` değer tipleriyle çalışır, JPA'ya bağımlı değildir.

**A-008-2. ★ Üç kural için serviste bir switch yetmez miydi?** Kısa olurdu, ama her yeni kural servisi değiştirir (OCP), üç kuralın testi bir sınıfta birikir ve unutulan `case` ancak çalışma anında görülür. Resolver'da eksik strateji uygulamanın başlamasını engeller.

**A-009-1. ADR-009 neyi karara bağlıyor?** Modüller arası tepkilerin Spring uygulama içi olaylarıyla, `AFTER_COMMIT` dinleyicileriyle ve senkron kurulmasını.

**A-009-2. ★ Neden RabbitMQ/Kafka değil, neden `@Async` değil?** Mesaj kuyruğu tek süreçli monolit için ek altyapı ve tutarlılık maliyeti (outbox gibi) getirir. `@Async` hata yönetimi, thread havuzu ve testte belirsizlik getirir; ölçülmüş bir gecikme sorunu yok. Gerekirse ayrı ADR ile eklenir.

**A-010-1. ADR-010 neyi karara bağlıyor?** Kurs durumunun State ile yazılmasını; `CourseState` `sealed`, durum nesneleri veri tutmaz (`INSTANCE`), tek giriş noktası `Course`.

**A-010-2. ★ Domain katmanı `common.exception`'a bağımlı; bu katman kuralını bozmuyor mu?** Bilinçli bir istisna: izinsiz durum geçişi 409 olmalı ve domain bunu `CourseStateException` (bir `ConflictException`) ile söylüyor. Domain'in dışarıya tek bağımlılığı bu; `common.security`, `common.config` ve `common.mapper` domain için yasak ve ArchUnit bunu denetliyor.

**A-011-1. ADR-011 neyi karara bağlıyor?** İçerik türlerinin Factory Method ile yazılmasını; türe özgü biçim, boyut ve sunuluş kuralları türün ürünündedir, yükleme servisi yalnız ortak arayüzü bilir.

**A-011-2. Sınırlar neler?** Video 200 MB (mp4, webm); PDF 20 MB; doküman 20 MB (pdf, docx, pptx). Aşan 413, izin verilmeyen tür 415; mesajlar ürünün `FilePolicy`'sinde.

**A-012-1. İçerik token'ı nedir, neden gerekti?** `<video>` ve bağlantılar `Authorization` başlığı gönderemiyor. Bu yüzden `POST /api/lessons/{id}/content-token` 5 dakika geçerli, kullanıcıya ve derse özel bir token verir; `GET …/content?token=` güvenlik filtresinden kimliksiz geçer, token'ı ve yetkiyi içerik servisi her istekte doğrular.

**A-012-2. ★ Ana JWT'yi adreste taşısanız olmaz mıydı?** Bütün API'ye 1 saat yetki veren token adreste, tarayıcı geçmişinde ve sunucu loglarında görünürdü. İçerik token'ı yalnız bir ders ve 5 dakika için; imza anahtarı da ana anahtardan türetilmiş ayrı bir anahtar, yani içerik token'ıyla API, API token'ıyla içerik açılmaz.

---

## 5. SOLID ve nesne yönelimli kavramlar

**SRP-1. SRP örneği ver.**

`CourseMapper` yalnız dönüştürür; `NotificationListener` yalnız olayı `NotificationService`'e iletir; `StorageService` dosyanın türünü bilmez, yalnız saklar. Her birinin değişme nedeni tek.

Kodu göster: `docs/solid.md` › S.

**SRP-2. ★ `NotificationListener` altı olayı dinliyor; bu SRP'ye aykırı değil mi?**

Bilinçli bir ödünleşim: altı metot da aynı işi yapar (olayı bildirim servisine iletmek) ve aynı işlem kuralına uyar. Sınıfın değişme nedeni tek: "hangi olay bildirim üretir". Altı ayrı sınıf tekrar getirirdi.

**OCP-1. OCP örneği ver.**

Strategy: yeni puanlama kuralı yeni bir strateji sınıfıdır, servis değişmez. Factory Method: yeni içerik türü yeni bir ürün ve fabrikadır.

**OCP-2. ★ OCP tam sağlanıyor mu?**

Tam değil: yeni puanlama kuralı için `ScoringType` enum'una değer, CHECK kısıtını genişleten bir migration ve OpenAPI'deki enum da değişir. Servis kodu ise değişmez. Bu ödünleşim `solid.md`'de yazılı.

**LSP-1. LSP örneği ver.**

Dört kurs durumu `CourseState` yerine geçebilir: `Course` hangi durumda olursa olsun aynı çağrıyı yapar, her durum ya izin verir ya da sözleşmedeki 409'u fırlatır; hiçbiri beklenmeyen bir istisna ya da sessiz başarı üretmez. İçerik ürünleri (`LessonContent`) de aynı şekilde.

**LSP-2. ★ Bir durum sınıfı bir işlemde 409 fırlatıyor; bu LSP'yi bozmuyor mu?**

Hayır, 409 arayüzün kendi sözleşmesi (varsayılan gövde); alt sınıf sözleşmeyi daraltmıyor, yalnız izin verdiği işlemleri ezerek genişletiyor.

**ISP-1. ISP örneği ver.**

`course`, istatistik için tek büyük servis yerine beş dar port tanımlar: `CourseRoster`, `EnrollmentCounts`, `CourseProgressStats`, `CourseQuizStats`, `CourseRatings`. Katalog yalnız sayı ve puan portunu, `CourseVisibility` yalnız `isEnrolled`'ı kullanır; `progress` yalnız `totals`'ı uygular, boş metot yazmaz.

Kodu göster: `docs/solid.md` › I.

**ISP-2. `CourseAnalyticsService` beş portu birden alıyor; bu ISP'ye aykırı mı?**

Hayır; istatistik ekranı beşinin verisini de gösteriyor, yani hepsini gerçekten kullanıyor.

**DIP-1. DIP örneği ver.**

`course`'un istatistik portları (arayüz kullananda, uygulama sağlayanda), `common`'un `AccountLookup`'ı ve `content`'in `StorageService`'i (servisler yerel diske değil soyutlamaya bağımlı).

**DIP-2. ★ Port tiplerini `course` tanımlıyor; bunun bedeli ne?**

Veri biçimini `course` belirliyor (örn. `ProgressTotals`, `RosterPage`); istatistik ekranı değişince birden fazla modül değişebilir. Karşılığında bağımlılık yönü tek ve döngüsüz.

**OOP-1. Kapsülleme örneği?**

`Course`'un `status` alanının setter'ı yok; durum yalnız `submit()`, `approve()` gibi metotlarla, durum nesnesine sorularak değişir.

**OOP-2. Çok biçimlilik ve kalıtım örneği?**

Aynı `course.submit()` çağrısı duruma göre farklı davranır; aynı `factory.create()` türe göre farklı ürün üretir. `LessonContentFactory`'nin `create()`'i bir şablon metottur, alt sınıflar `createContent()`'i doldurur; `CourseState` ve `LessonContent` `sealed` hiyerarşilerdir.

---

## 6. Ortak konular

**O-hata-1. Hatalar istemciye nasıl dönüyor?**

Hepsi tek gövdeyle: `{ status, message, errors }`. Service `common/exception`'daki istisnalardan birini fırlatır (`NotFoundException` 404, `ConflictException` 409 …), `GlobalExceptionHandler` gövdeye çevirir. `@Valid` ihlalleri 400 ve alan bazlı `errors`; frontend mesajı ilgili alanın altında gösterir. `message` her zaman kullanıcıya gösterilebilir Türkçe bir cümle.

Kodu göster: `common/exception/GlobalExceptionHandler.java`.

**O-hata-2. ★ Filtrede oluşan 401 de aynı gövdeyle mi dönüyor?**

Evet; filtredeki 401/403'ü `JsonSecurityErrorHandler`, controller'daki `@PreAuthorize` reddini `GlobalExceptionHandler` yazar, mesajlar `SecurityMessages`'ta.

**O-yetki-1. Yetki kuralları nerede yazılıyor?**

Üç soru: kimlik (token yoksa filtre 401), rol (`/api/admin/**` ve `/api/instructor/**` adres kuralıyla `SecurityConfig`'te, diğerleri uçta `@PreAuthorize`), sahiplik ("bu kurs bu eğitmenin mi?" iş kuralı, service'te `ForbiddenException`). Bu Bölüm 6'nın "yetkisiz düzenleme" testidir.

Kodu göster: `common/security/SecurityConfig.java`, `course/service/CourseService.java`.

**O-test-1. Test stratejiniz ne?**

Service'ler için JUnit + Mockito unit testleri (`*Test`), Testcontainers ile gerçek PostgreSQL'e karşı entegrasyon testleri (`*IT`), ArchUnit mimari testleri. Hepsi her PR'da CI'da (`./mvnw verify`); frontend için lint ve build.

**O-test-2. Bölüm 6'nın dört durumu nerede test ediliyor?**

Kayıtsız erişim (403), mükerrer kayıt (409), yetkisiz kurs düzenleme (403) ve yanlış ilerleme hesabı; uçtan uca `integration/Bolum6CriticalCasesIT.java`'da, ayrıca ilgili service testlerinde. Dosya türü/boyut denetimleri `integration/ContentUploadIT.java`'da. Bu testler silinmez, `@Disabled` yapılmaz.

**O-sayfa-1. Listeler sayfalı mı?**

Evet: `page`/`size` parametreleri, varsayılan 20, en fazla 100; daha büyüğü 400 (NFR-01).

---

## 7. Prova takibi

#107'nin diğer iki kabul kriteri için. Her üye kendi yazmadığı en az iki modülü ve tüm desenleri kodu göstererek anlatır.

| Üye | Anlattığı modüller (kendi yazmadığı) | Desenler (5/5) | Tarih | Not |
|---|---|---|---|---|
| Bertan | | | | |
| Eray | | | | |
| Sedat | | | | |
| Veronika | | | | |

**Cevaplanamayan sorular ve yapılan kısa eğitim:**

| Soru | Kim | Not / kim anlattı |
|---|---|---|
| | | |
