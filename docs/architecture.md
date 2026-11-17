# Mimari kurallar

Bu doküman Mini Udemy backend'inin mimari kurallarını tanımlar: sistemin hangi modüllere ayrıldığını, katmanların ve modüllerin birbirine nasıl bağımlı olabileceğini ve tasarım desenlerinin nerede kullanıldığını. Kod bu kurallara uyar; kural değişecekse önce bu doküman (ve gerekiyorsa bir ADR) değişir.

Kapsamın kaynağı proje tanım dokümanıdır (modüller A–L, senaryo 1–5). Önemli kararların gerekçeleri `docs/adr/` altındaki mimari karar kayıtlarındadır (ADR).

## 1. Mimari stil

**Katmanlı, modüler monolit.** Backend tek bir Spring Boot uygulaması olarak dağıtılır ve tek bir PostgreSQL veritabanı kullanır. Kod içeride iş alanlarına göre modüllere, her modül de katmanlara ayrılır.

Kısaca neden: proje dokümanı bu yapıyı önerir; dört kişilik ekip ve beş haftalık süre için mikroservislerin dağıtım, ağ ve veri tutarlılığı maliyeti gereksizdir. Modül sınırları ise kodun büyürken dağılmasını önler ve ileride bir modülün ayrılmasını mümkün kılar. Ayrıntılı karar ve değerlendirilen alternatifler: [**ADR-001**](adr/0001-moduler-monolit.md).

## 2. Modüller

Paket yapısı: `com.miniudemy.<modül>.<katman>`

| Modül | Sorumluluk | Ana kavramlar | Proje dokümanı |
|---|---|---|---|
| `auth` | Kayıt, giriş, JWT, parola yenileme, profil; kursiyer/eğitmen/yönetici rolleri | User, Role | A |
| `course` | Kategori, kurs, bölüm ve sıralı ders hiyerarşisi; kurs yayın durumları; katalog ve arama; eğitmen paneli istatistikleri | Category, Course, Section, Lesson, CourseState | B, D, J |
| `content` | Ders içerikleri ve materyaller: video/PDF/doküman yükleme, içerik türleri, yerel depolama, yetki kontrollü servis | LessonContent, StoredFile | C |
| `enrollment` | Yayımlanmış kurslara ücretsiz kayıt, kayıtlı kurslar paneli, mükerrer kayıt engeli | Enrollment | E |
| `progress` | Ders tamamlama, son izlenen ders, bölüm/kurs ilerleme yüzdesi, kaldığı yerden devam | LessonProgress, CourseProgress | F |
| `quiz` | Soru bankası, bölüm sonu sınavı, otomatik puanlama, başarı eşiği, deneme geçmişi | Question, Quiz, QuizAttempt | G |
| `certificate` | Koşulları tamamlayan kursiyere doğrulama kodlu platform içi sertifika | Certificate | H |
| `review` | 1–5 puan ve yorum, mükerrer değerlendirme engeli | Review | I |
| `notification` | Uygulama içi bildirimler; domain olaylarını dinler | Notification | L |
| `admin` | Kurs onay/red/yayından kaldırma, kullanıcı ve kategori yönetimi, şikâyetler, platform raporları | Complaint | K |
| `common` | Config, güvenlik altyapısı (`common/security`: JWT doğrulama, herkese açık uçlar, 401/403 gövdeleri), global hata yönetimi, MapStruct ortak ayarı | – | – |

`common` iş kuralı içermez ve hiçbir modüle bağımlı değildir.

## 3. Katmanlar

Her modül aynı altı pakete ayrılır:

| Katman | İçerik | Kullanabileceği |
|---|---|---|
| `controller` | REST uçları, istek doğrulamasının tetiklenmesi, yetki anotasyonları | `service`, `dto` |
| `service` | İş kuralları ve `@Transactional` sınırı | `repository`, `domain`, `mapper`, `dto` |
| `repository` | Spring Data JPA arayüzleri | `domain` |
| `dto` | API'nin istek/yanıt tipleri (Java `record`) | – |
| `mapper` | `domain` ↔ `dto` dönüşümleri | `domain`, `dto` |
| `domain` | JPA entity'leri, enum'lar, domain olayları | – |

Bağımlılık yönü tek yönlüdür: **controller → service → repository**. Hepsi `domain`'i kullanabilir; `domain` hiçbir katmana bağımlı değildir.

**Yasaklar:**

- Controller repository'ye erişmez ve iş kuralı içermez.
- **Entity API'ye çıkmaz.** Controller yalnız DTO alır ve döner; dönüşüm yalnız mapper'da yapılır.
- İş kuralı yalnız service katmanındadır; repository'de sorgu dışında mantık olmaz.
- Hata yanıtı controller'da elle kurulmaz; service tanımlı bir istisna fırlatır, `common` içindeki global handler standart hata gövdesine çevirir (`CLAUDE.md` › Standart hata formatı).
- Şema yalnız Flyway migration'ı ile değişir (`backend/src/main/resources/db/migration/README.md`).

### Yetki kontrolleri nasıl yazılır

Kimlik doğrulama ve rol kontrolü `common/security/SecurityConfig`'te kurulu (#10, [ADR-002](adr/0002-jwt-stateless-kimlik-dogrulama.md)). Yeni bir uç yazarken üç soru sorulur: kim çağırabilir, hangi rol, kimin kaynağı?

1. **Kimlik:** sözleşmede `security: []` olmayan her uç token ister. Token yoksa, geçersizse ya da süresi dolmuşsa filtre 401 döner; uçta ayrıca bir şey yazılmaz. Yeni bir **herkese açık** uç, sözleşmede `security: []` ile ve `SecurityConfig.PUBLIC_GET` / `PUBLIC_POST` listesinde aynı PR'da eklenir.
2. **Rol:**
   - `/api/admin/**` yalnız ADMIN'e, `/api/instructor/**` yalnız INSTRUCTOR'a açıktır. Bu adres kuralı `SecurityConfig`'te; bu öneklerin altındaki uçlara ayrıca anotasyon yazılmaz.
   - Diğer uçlarda rol kuralı **controller metodunda** `@PreAuthorize` ile yazılır (`@EnableMethodSecurity` açık).
   - Rol token'daki claim'den değil, her istekte veritabanından okunur (`AccountLookup`). Rolü değişen kullanıcının eski token'ı eski rolle çalışmaz.
   - Ret 403 ve standart gövde döner ("Bu işlem için yetkiniz yok."), 500 değil.
3. **Giriş yapan kullanıcı ve sahiplik:**
   - Controller kullanıcının id'sini `CurrentUser.id(authentication)` ile alır ve service'e **id olarak** verir. Service `Authentication`'a ya da `SecurityContextHolder`'a erişmez.
   - "Bu kurs bu eğitmenin mi?" gibi sahiplik kontrolleri rol değil iş kuralıdır. Service'te yapılır ve `ForbiddenException` fırlatır (NFR-05; proje dokümanı §6 "yetkisiz kurs düzenleme" testi).

```java
@PostMapping("/api/courses")
@PreAuthorize("hasRole('INSTRUCTOR')")                       // 2. rol: uçta
public CourseResponse create(@Valid @RequestBody CreateCourseRequest request, Authentication authentication) {
    return courseService.create(CurrentUser.id(authentication), request);   // 3. kullanıcı id olarak
}

// CourseService — 3. sahiplik: service'te
if (!course.getInstructorId().equals(userId)) {
    throw new ForbiddenException("Bu kursu düzenleme yetkiniz yok.");
}
```

Örnekler ve testleri:
- `SecurityConfigTest`: `hasRole('ADMIN')` ve `hasRole('INSTRUCTOR')` (öğrenci 403, doğru rol 200); adres kuralı; rolün veritabanından geldiği.
- `MeControllerTest`: `GET /api/me` için token'sız 401, kendi profili 200, pasif hesap 403.

## 4. Modüller arası iletişim

Bir modül başka bir modülle **yalnız iki yoldan** konuşur:

1. **Diğer modülün `service` paketindeki public arayüzü** (senkron çağrı; örn. kayıt sırasında kursun yayımlanmış olup olmadığını sormak).
2. **Domain olayları** (Observer, bölüm 5): bir modülün işi başka modüllerde tepki doğuracaksa olay yayınlanır, dinleyen modül tepkisini kendisi verir.

**Yasaklar:**

- Başka bir modülün `repository` paketine erişilmez.
- Başka bir modülün entity'si alan, parametre ya da dönüş tipi olarak kullanılmaz. Modül sınırında yalnız id ya da DTO taşınır. Veritabanında modüller arası yabancı anahtar olabilir, ama JPA ilişkisi (`@ManyToOne` vb.) yalnız aynı modülün entity'leri arasında kurulur.
- Modüller arası döngüsel bağımlılık olmaz.

### İzin verilen modül bağımlılıkları

| Modül | Service'ini kullanabileceği modüller | Dinlediği olaylar |
|---|---|---|
| `auth` | – | – |
| `course` | `auth` | – |
| `enrollment` | `course` | – |
| `content` | `course`, `enrollment` | – |
| `progress` | `course`, `enrollment` | `LessonCompleted` |
| `quiz` | `course`, `enrollment` | – |
| `certificate` | `course`, `progress`, `quiz` | `LessonCompleted`, `QuizCompleted` |
| `review` | `course`, `enrollment` | – |
| `notification` | `auth` | Tüm bildirim olayları (bölüm 5) |
| `admin` | `auth`, `course`, `enrollment`, `progress`, `certificate`, `review` | – |

Tabloda olmayan bir bağımlılık gerekiyorsa önce bu doküman güncellenir.

**Eğitmen istatistikleri (J) döngü oluşturmasın diye bağımlılık tersine çevrilir.** Eğitmen paneli `course` modülündedir ama kayıt, ilerleme, sınav ve puan verisi başka modüllerdedir; `course` bu modülleri doğrudan çağırsaydı onlar da `course`'a bağlı olduğu için döngü oluşurdu. Bunun yerine `course`, istatistik için ihtiyaç duyduğu sorgu arayüzlerini kendi `service` paketinde tanımlar; `enrollment`, `progress`, `quiz` ve `review` bu arayüzleri uygular. Böylece bağımlılık yönü hep "diğer modül → `course`" kalır (Dependency Inversion).

**Güvenlik altyapısı da aynı yolla `auth`'a bağımlı olmaz.** `common/security` her kimlikli istekte kullanıcının hâlâ var olup olmadığını, aktifliğini ve güncel rolünü bilmek zorunda (pasif kullanıcının ya da rolü düşürülmüş kullanıcının eski token'ı çalışmamalı). Bu bilgi `auth`'taki `users` tablosunda; `common` doğrudan `auth`'u çağırsaydı "`common` hiçbir modüle bağımlı değildir" kuralı bozulurdu. Bunun yerine `common` `AccountLookup` arayüzünü tanımlar, `auth/service/AccountLookupService` uygular (#10).

## 5. Tasarım desenleri

Desenler gerçek bir ihtiyaç için ve bilinçli olarak kullanılır. Her birinin gerekçesi kendi issue'sunda, kararı `docs/adr/` altında bir ADR'dedir.

| Desen | Modül | Çözdüğü sorun | Issue |
|---|---|---|---|
| **State** | `course` | Kursun durumu (Taslak / İncelemede / Yayımlandı / Yayından kaldırıldı) düzenleme, silme, onaya gönderme, onay, red ve yayından kaldırma kurallarını belirler. Her durumun kuralları kendi sınıfında toplanır; serviste durum için if/switch yazılmaz. | #85 |
| **Observer** | yayınlayan: `enrollment`, `course`, `progress`, `quiz`, `certificate`; dinleyen: `progress`, `certificate`, `notification` | Bir olaya birden fazla modül tepki verir; yayınlayan modül dinleyenleri bilmez. Spring `ApplicationEventPublisher` ve `@TransactionalEventListener` ile uygulanır. | #42, #124 |
| **Strategy** | `quiz` | Puanlama kuralı sınavın türüne göre değişir; her kural ayrı bir `ScoringStrategy` sınıfıdır, yeni kural yeni sınıftır. | #52 |
| **Factory Method** | `content` | Ders içerik türleri (VIDEO, PDF, DOCUMENT) farklı biçim, boyut ve sunuluş kurallarına sahiptir; içeriği oluşturan kod türün fabrikasındadır, yükleme servisi yalnız ortak arayüzü bilir. Türün kabul kuralı (izin verilen biçimler, en büyük boyut, 413/415 mesajları) bir `FilePolicy` olarak depolamaya (`StorageService.store`, #111) verilir; depolama türleri bilmez. | #110, #111 |
| **DTO + Mapper** | tüm modüller | Entity'nin API'ye sızmasını önler; API sözleşmesini veritabanı şemasından ayırır. DTO'lar `record`, mapper'lar **MapStruct** arayüzleri: `@Mapper(config = MapStructConfig.class)` (`common/mapper`), `unmappedTargetPolicy = ERROR` ile sözleşmeye eklenip eşlenmeyen alan derlemeyi durdurur. Elle yazılmış mapper yerine MapStruct'ın seçilme gerekçesi: [ADR-006](adr/0006-dto-mapper-mapstruct.md). | #9 |

### Domain olayları

| Olay | Yayınlayan | Dinleyenler |
|---|---|---|
| `EnrollmentCreated` | `enrollment` | `notification` (eğitmene) |
| `LessonPublished` | `course` | `notification` (kayıtlı kursiyerlere) |
| `CourseApproved` | `course` | `notification` (eğitmene) |
| `CourseRejected` (gerekçeli) | `course` | `notification` (eğitmene) |
| `LessonCompleted` | `progress` | `progress` (ilerleme yüzdesi), `certificate` (uygunluk) |
| `QuizCompleted` | `quiz` | `certificate` (uygunluk), `notification` (kursiyere) |
| `CertificateIssued` | `certificate` | `notification` (kursiyere) |

Olay tipleri yayınlayan modülün `domain` paketinde Java `record` olarak tanımlanır.

## 6. Diyagramlar

### Modül ve katman yapısı

```mermaid
flowchart TB
  subgraph MOD["Her modül (ör. course)"]
    direction TB
    C[controller] --> S[service]
    S --> R[repository]
    C -. DTO .-> D[dto]
    S --> M[mapper]
    M --> D
    M --> E[domain]
    S --> E
    R --> E
  end
  subgraph COMMON["common"]
    SEC[security: JWT filtresi, oturumdaki kullanıcı]
    EX[exception: global handler]
    CFG[config]
  end
  C -. kullanır .-> SEC
  S -. istisna fırlatır .-> EX
```

### Modül bağımlılıkları

Düz ok: service çağrısı. Kesikli ok: domain olayı (yayınlayan → dinleyen). Noktalı çizgi: `course`'un tanımladığı istatistik arayüzünü uygulama.

```mermaid
flowchart LR
  auth
  course --> auth
  enrollment --> course
  content --> course
  content --> enrollment
  progress --> course
  progress --> enrollment
  quiz --> course
  quiz --> enrollment
  certificate --> course
  certificate --> progress
  certificate --> quiz
  review --> course
  review --> enrollment
  notification --> auth
  admin --> auth
  admin --> course
  admin --> enrollment
  admin --> progress
  admin --> certificate
  admin --> review

  enrollment -. EnrollmentCreated .-> notification
  course -. LessonPublished / CourseApproved / CourseRejected .-> notification
  progress -. LessonCompleted .-> certificate
  quiz -. QuizCompleted .-> certificate
  quiz -. QuizCompleted .-> notification
  certificate -. CertificateIssued .-> notification

  enrollment -.-|istatistik arayüzü| course
  progress -.-|istatistik arayüzü| course
  quiz -.-|istatistik arayüzü| course
  review -.-|istatistik arayüzü| course
```

## 7. Mimari kararlar ve kuralların denetimi

- **Mimari karar kayıtları:** önemli her karar `docs/adr/` altında bir ADR olarak yazılır: bağlam, karar, değerlendirilen alternatifler, sonuçlar. Kurallar, şablon ve kayıt listesi: [`docs/adr/README.md`](adr/README.md). Planlanan ADR'ler: ADR-001 modüler monolit (#80), ADR-002 JWT ve ADR-003 Flyway (#81), ADR-004 yerel dosya depolama ve ADR-005 ayrı frontend (#84). Desen kararlarının ADR'leri ilgili desen issue'larında yazılır.
- **Otomatik denetim:** bölüm 3 ve 4'teki kurallar ArchUnit testleriyle CI'da kontrol edilir (#86). Kural ihlali build'i kırar; testi geçmek için kural gevşetilmez, kod düzeltilir.
- **Review:** PR'larda bu dokümana uyum kontrol edilir; modüller arası yeni bir bağımlılık bu dokümanın güncellenmesini gerektirir.
- **Tutarlılık:** bu doküman, `CLAUDE.md` ve bileşen diyagramı (#91) aynı modül listesini ve bağımlılık kurallarını anlatır; biri değişirse diğerleri aynı PR'da güncellenir.
