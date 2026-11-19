# SOLID ve nesne yönelimli tasarım (koddan örneklerle)

Bu doküman, SOLID ilkelerinin ve temel OOP kavramlarının Mini Udemy backend'inde **nerede ve nasıl** uygulandığını gösterir (proje şablonu 4.6, #95). Her örnek gerçek koddan alınmıştır; satır numaraları `main` @ `df9dd72` içindir, kod değişirse kayabilir.

Kısaltma: `…/` = `backend/src/main/java/com/miniudemy/`.

İlgili belgeler:

- Mimari ve modüller arası iletişim: [`architecture.md`](architecture.md) › 4. Modüller arası iletişim
- Desen kararları: [ADR-006 DTO + Mapper](adr/0006-dto-mapper-mapstruct.md), [ADR-008 Strategy](adr/0008-strategy-puanlama.md), [ADR-009 Observer](adr/0009-observer-spring-events.md), [ADR-010 State](adr/0010-state-kurs-durumu.md), [ADR-011 Factory Method](adr/0011-factory-method-icerik-turleri.md), [ADR-004 Yerel dosya depolama](adr/0004-yerel-dosya-depolama.md)
- Sınıf diyagramları (#51): [genel bakış](diagrams/class.puml), [State](diagrams/class-state.puml), [Observer](diagrams/class-observer.puml), [Strategy](diagrams/class-strategy.puml), [Factory Method](diagrams/class-factory.puml), [DTO + Mapper](diagrams/class-dto-mapper.puml)

## Özet

| İlke / kavram | Ana örnek | Yer |
|---|---|---|
| SRP | Mapper yalnız dönüştürür; dinleyici yalnız iletir; depolama tür bilmez | `CourseMapper`, `NotificationListener`, `StorageService` |
| OCP | Yeni puanlama kuralı / içerik türü = yeni sınıf; seçimde if/switch yok | `ScoringStrategyResolver`, `LessonContentFactories` |
| LSP | Dört kurs durumu aynı sözleşmeyle birbirinin yerine geçer | `CourseState` + 4 durum sınıfı, `LessonContent` + 3 ürün |
| ISP | Tek bir "istatistik servisi" yerine beş dar port | `CourseRoster`, `EnrollmentCounts`, `CourseProgressStats`, `CourseQuizStats`, `CourseRatings` |
| DIP | Arayüz kullanan modülde, uygulama sağlayan modülde | course portları, `EnrollmentProgress`, `AccountLookup`, `StorageService` |
| Kapsülleme | Kurs durumu yalnız iş metotlarıyla değişir | `Course`, `CourseStatus`, paket içi `CourseState` |
| Çok biçimlilik | Aynı çağrı, nesneye göre farklı davranış | `state().submit()`, `strategy.score(...)`, `content.filePolicy()` |
| Kalıtım / soyutlama | Şablon metot + sealed hiyerarşiler | `LessonContentFactory.create()` |

---

## S — Tek Sorumluluk İlkesi (SRP)

> Bir sınıfın değişmek için tek bir nedeni olmalı.

Katman kuralı (`CLAUDE.md` › Modüller ve katmanlar) sorumlulukları zaten ayırıyor: controller yalnız HTTP, service iş kuralı ve işlem sınırı, repository yalnız erişim, mapper yalnız dönüşüm. Bunu `LayerArchitectureTest` CI'da denetliyor. Aşağıda sınıf düzeyinde üç örnek var.

### 1. Mapper yalnız dönüştürür — `CourseMapper`

[`…/course/mapper/CourseMapper.java:21-40`](../backend/src/main/java/com/miniudemy/course/mapper/CourseMapper.java)

```java
/**
 * Kurs, bölüm ve ders entity'lerinden katalog DTO'larına dönüşüm (ADR-006). Eğitmen adı ve sayılar başka modüllerden
 * geldiği için hazır parametre olarak verilir; mapper veritabanına ya da başka servise gitmez.
 */
@Mapper(config = MapStructConfig.class, uses = CategoryMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CourseMapper {
    ...
    CourseSummaryResponse toSummary(Course course, UserSummary instructor, CourseStats stats);
```

Eğitmen adı (`UserSummary`) ve sayılar (`CourseStats`) mapper'a hazır olarak verilir. Mapper'ın değişme nedeni yalnız DTO'nun biçiminin değişmesidir. Verinin nereden geldiği servisin sorumluluğundadır. Entity'nin API'ye çıkmaması ve dönüşümün yalnız mapper'da yapılması ADR-006'nın kuralıdır. MapStruct'ın `unmappedTargetPolicy = ERROR` ayarı, eşlenmemiş bir alanı derleme hatasına çevirir.

### 2. Dinleyici yalnız iletir — `NotificationListener`

[`…/notification/listener/NotificationListener.java:17-35`](../backend/src/main/java/com/miniudemy/notification/listener/NotificationListener.java)

```java
@Component
public class NotificationListener {

    private final NotificationService notifications;
    ...
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEnrollmentCreated(EnrollmentCreated event) {
        notifications.notifyEnrollmentCreated(event.courseId(), event.studentId());
    }
```

Sorumluluklar üç sınıfa bölünmüştür:

- **`NotificationListener`:** olayın ne zaman işleneceğini belirler (commit'ten sonra, ayrı bir işlemde).
- **`NotificationService`:** bildirimin kime yazılacağını belirler.
- **`NotificationMessages`:** bildirim metnini üretir. Metinler yalnız [`…/notification/service/NotificationMessages.java:10`](../backend/src/main/java/com/miniudemy/notification/service/NotificationMessages.java)'de üretilir.

Yayıncı (örneğin `EnrollmentService`) bildirimi hiç bilmez (Observer, ADR-009).

### 3. Depolama tür bilmez — `StorageService`

[`…/content/service/StorageService.java:10-17`](../backend/src/main/java/com/miniudemy/content/service/StorageService.java)

```java
/**
 * ...
 * Tür ve boyut kuralını çağıran verir ({@link FilePolicy}; ders içerikleri için #110'daki fabrikalar, kapak için
 * #112). Depolama türleri bilmez.
 */
public interface StorageService {
    StoredFileInfo store(String originalName, InputStream content, FilePolicy policy, Long uploaderId);
```

"Video en fazla 200 MB" kuralı bir içerik kuralıdır ve `VideoContent`'te durur. "Akışı geçici dosyaya yaz, boyutu say, türü içerikten tespit et" ise bir depolama kuralıdır ve `LocalDiskStorageService`'te durur. Biri değişince diğeri değişmez. İkisi arasındaki tek bağlantı `FilePolicy` değeridir.

---

## O — Açık/Kapalı İlkesi (OCP)

> Bir modül genişlemeye açık, değişikliğe kapalı olmalı.

### 1. Puanlama kuralları — Strategy (ADR-008)

[`…/quiz/service/ScoringStrategy.java:9-16`](../backend/src/main/java/com/miniudemy/quiz/service/ScoringStrategy.java)

```java
public interface ScoringStrategy {
    ScoringType type();
    QuestionScore score(ScoringInput input);
}
```

Üç uygulaması var: `SingleAnswerScoring`, `MultiAnswerAllOrNothing`, `MultiAnswerPartialScoring`. Seçimi [`…/quiz/service/ScoringStrategyResolver.java:19-37`](../backend/src/main/java/com/miniudemy/quiz/service/ScoringStrategyResolver.java) yapar:

```java
private final Map<ScoringType, ScoringStrategy> strategies = new EnumMap<>(ScoringType.class);

public ScoringStrategyResolver(List<ScoringStrategy> candidates) {
    for (ScoringStrategy strategy : candidates) {
        ScoringStrategy previous = strategies.put(strategy.type(), strategy);
        ...
}

public ScoringStrategy resolve(ScoringType type) {
    return strategies.get(type);
}
```

Spring bütün `ScoringStrategy` bean'lerini listeye koyar. Eşleme, her bean'in kendi `type()` değerinden kurulur. Sınavı puanlayan servis ([`…/quiz/service/QuizAttemptService.java:91`](../backend/src/main/java/com/miniudemy/quiz/service/QuizAttemptService.java)) hangi kuralın çalıştığını bilmez:

```java
ScoringStrategy strategy = scoringResolver.resolve(quiz.getScoringType());
```

Yeni bir puanlama kuralı eklemek için yeni bir `@Component` sınıfı yazmak yeter. `QuizAttemptService` ve resolver değişmez. Bir tipin stratejisi eksikse ya da bir tipi iki strateji iddia ediyorsa resolver uygulama başlarken hata verir. Bunu `ScoringStrategyResolverTest` doğrular. (Ödünleşim için aşağıdaki 3. nota bakın.)

### 2. Ders içerik türleri — Factory Method (ADR-011)

[`…/content/service/LessonContentFactories.java:21-44`](../backend/src/main/java/com/miniudemy/content/service/LessonContentFactories.java)

```java
private final Map<LessonContentType, LessonContentFactory> byType = new EnumMap<>(LessonContentType.class);
...
public LessonContentFactory forType(LessonContentType type) {
    ...
    return byType.get(type);
}
```

Fabrikayı kullananlar türü sormaz, yalnız ortak arayüzü çağırır:

- Yükleme: [`…/content/service/ContentUploadService.java:42-43`](../backend/src/main/java/com/miniudemy/content/service/ContentUploadService.java)

  ```java
  StoredFileInfo file = storage.store(originalName, content,
          factories.forType(lesson.contentType()).create().filePolicy(), instructorId);
  ```

- Sunum: [`…/content/service/ContentDeliveryService.java:93`](../backend/src/main/java/com/miniudemy/content/service/ContentDeliveryService.java)

  ```java
  LessonContent content = factories.forType(ref.contentType()).create();
  ```

Yeni bir içerik türü (örneğin ses) eklemek için yeni bir fabrika (`AudioContentFactory`) ve yeni bir ürün (`AudioContent`) yazılır. Yükleme ve sunum servisleri değişmez. `LessonContentFactoriesTest` eksik ya da çift fabrikanın uygulamayı başlatmadığını, `LessonContentFactoriesWiringTest` de Spring'in bulduğu fabrikaların her türü doldurduğunu doğrular.

---

## L — Liskov Yerine Geçme İlkesi (LSP)

> Bir alt tür, üst türünün beklendiği her yerde programın doğruluğunu bozmadan kullanılabilmeli.

### 1. Kurs durumları — `CourseState` (State, ADR-010)

[`…/course/domain/CourseState.java:13-80`](../backend/src/main/java/com/miniudemy/course/domain/CourseState.java)

```java
sealed interface CourseState permits DraftState, InReviewState, PublishedState, UnpublishedState {

    CourseStatus status();

    default void assertEditable() {
        throw new CourseStateException(
                "Kurs bu durumdayken düzenlenemez; yalnız taslak ve yayından kaldırılmış kurslar düzenlenebilir.");
    }
    ...
    default CourseStatus submit() {
        throw new CourseStateException("Kurs bu durumdayken onaya gönderilemez; ...");
    }
```

Sözleşme her işlem için aynıdır: izin varsa kontrol sessizce geçer ya da geçiş hedef `CourseStatus`'u döner, izin yoksa `CourseStateException` (409) fırlar. Dört durum sınıfı bu sözleşmenin dışına çıkmaz. Hiçbiri daha güçlü bir ön koşul koymaz, başka bir istisna türü fırlatmaz ya da `null` dönmez. Örneğin [`…/course/domain/DraftState.java:16-31`](../backend/src/main/java/com/miniudemy/course/domain/DraftState.java):

```java
@Override
public void assertEditable() {
}
...
@Override
public CourseStatus submit() {
    return CourseStatus.IN_REVIEW;
}
```

Bu yüzden `Course`, hangi durumda olduğunu bilmeden çağrıyı yapabilir ([`…/course/domain/Course.java:240-289`](../backend/src/main/java/com/miniudemy/course/domain/Course.java)):

```java
public void submit() {
    CourseStatus target = state().submit();
    ...
    status = target;
}
...
private CourseState state() {
    return status.state();
}
```

`CourseStateTest`, 4 durum × 7 işlem = 28 birleşimin her birinde bu sözleşmeyi doğrular (izinsiz işlemde 409 ve değişmeyen durum). Dört durum sınıfı aynı testten geçtiği için birbirinin yerine konabilir.

### 2. İçerik ürünleri — `LessonContent`

[`…/content/domain/LessonContent.java:10-20`](../backend/src/main/java/com/miniudemy/content/domain/LessonContent.java)

```java
public abstract sealed class LessonContent permits VideoContent, PdfContent, DocumentContent {
    public abstract FilePolicy filePolicy();
    public abstract DeliveryMode deliveryMode();
    public abstract boolean supportsRangeRequests();
}
```

`ContentUploadService` ve `ContentDeliveryService` yalnız bu üç metodu çağırır. Hangi ürünün geldiği sonucu değiştirir ama sözleşmeyi değiştirmez: her ürün boş olmayan bir `FilePolicy` ve bir `DeliveryMode` döner. `LessonContentFactory.create()` de `null` ürünü engeller (aşağıda, Kalıtım ve soyutlama).

---

## I — Arayüz Ayrımı İlkesi (ISP)

> İstemciler kullanmadıkları metotlara bağımlı olmaya zorlanmamalı.

`course` modülü, katalog ve eğitmen istatistikleri için başka modüllerden veri ister. Bunun için tek bir büyük "istatistik servisi" yerine, her biri tek bir modülün tek bir sorusuna karşılık gelen **beş dar port** tanımlar:

| Port (`…/course/service/`) | Metot | Uygulayan modül |
|---|---|---|
| [`CourseRoster`](../backend/src/main/java/com/miniudemy/course/service/CourseRoster.java) | `page`, `isEnrolled` | enrollment |
| [`EnrollmentCounts`](../backend/src/main/java/com/miniudemy/course/service/EnrollmentCounts.java) | `countByCourse`, `countByCourses` | enrollment |
| [`CourseProgressStats`](../backend/src/main/java/com/miniudemy/course/service/CourseProgressStats.java) | `totals` | progress |
| [`CourseQuizStats`](../backend/src/main/java/com/miniudemy/course/service/CourseQuizStats.java) | `attemptStats`, `bestResults` | quiz |
| [`CourseRatings`](../backend/src/main/java/com/miniudemy/course/service/CourseRatings.java) | `ratingOf`, `ratingsByCourses`, `distributionOf`, `courseIdsWithAverageAtLeast` | review |

Her istemci yalnız ihtiyaç duyduğu portu alır:

- **`CourseCatalogService`** ([`…/course/service/CourseCatalogService.java:54-55`](../backend/src/main/java/com/miniudemy/course/service/CourseCatalogService.java)) katalog kartları için yalnız sayı ve puan kullanır: `EnrollmentCounts` ve `CourseRatings`.
- **`CourseVisibility`** ([`…/course/service/CourseVisibility.java:21`](../backend/src/main/java/com/miniudemy/course/service/CourseVisibility.java)) yalnız `CourseRoster`'ı kullanır, onun da yalnız `isEnrolled` metodunu.
- **`CourseAnalyticsService`** ([`…/course/service/CourseAnalyticsService.java:43-47`](../backend/src/main/java/com/miniudemy/course/service/CourseAnalyticsService.java)) eğitmen istatistikleri ekranı için beşini birden alır, çünkü o ekran hepsini gösterir.

Uygulayan tarafta da aynı ayrım var. Örneğin `progress` modülü yalnız `totals` metodunu uygular. Sınav ya da puan metotlarına yer tutucu (boş) gövde yazmak zorunda kalmaz.

---

## D — Bağımlılığı Tersine Çevirme İlkesi (DIP)

> Üst düzey modüller alt düzey modüllere değil, soyutlamalara bağımlı olmalı; soyutlamayı kullanan taraf tanımlar.

### 1. course portları: arayüz kullananda, uygulama sağlayanda

Yukarıdaki beş port `course/service`'te tanımlıdır, uygulamaları diğer modüllerdedir:

| Port | Uygulama |
|---|---|
| `CourseRoster` | [`…/enrollment/service/EnrollmentRosterService.java:24`](../backend/src/main/java/com/miniudemy/enrollment/service/EnrollmentRosterService.java) |
| `EnrollmentCounts` | [`…/enrollment/service/EnrollmentStatsService.java:18`](../backend/src/main/java/com/miniudemy/enrollment/service/EnrollmentStatsService.java) |
| `CourseProgressStats` | [`…/progress/service/CourseProgressStatsService.java:21`](../backend/src/main/java/com/miniudemy/progress/service/CourseProgressStatsService.java) |
| `CourseQuizStats` | [`…/quiz/service/QuizStatsService.java:31`](../backend/src/main/java/com/miniudemy/quiz/service/QuizStatsService.java) |
| `CourseRatings` | [`…/review/service/ReviewStatsService.java:24`](../backend/src/main/java/com/miniudemy/review/service/ReviewStatsService.java) |

```java
// …/course/service/EnrollmentCounts.java
public interface EnrollmentCounts {
    long countByCourse(Long courseId);
    Map<Long, Long> countByCourses(Collection<Long> courseIds);
}

// …/enrollment/service/EnrollmentStatsService.java
public class EnrollmentStatsService implements EnrollmentCounts {
    ...
    @Override
    public long countByCourse(Long courseId) {
        return repository.countByCourseId(courseId);
    }
```

Bu kalıp olmasaydı `course` modülü `enrollment`, `progress`, `quiz` ve `review` modüllerine bağımlı olurdu. Oysa bu dört modül zaten `course`'a bağımlı (kursun var olup olmadığını, sahibini, yayımlanıp yayımlanmadığını sorarlar). Sonuç bir döngü olurdu. Port sayesinde derleme zamanındaki ok her zaman `enrollment/progress/quiz/review → course` yönündedir. ArchUnit'teki modül tablosunda `course` yalnız `auth`'a bağımlı olabilir ([`backend/src/test/java/com/miniudemy/common/architecture/ModuleArchitectureTest.java:41`](../backend/src/test/java/com/miniudemy/common/architecture/ModuleArchitectureTest.java)). `modulesAreFreeOfCycles` kuralı (aynı dosya, satır 66) da döngüyü CI'da yakalar. Mimari şemada bu ilişki "istatistik arayüzü" noktalı çizgisiyle gösterilir (`architecture.md` › 4).

### 2. Aynı kalıbın tekrarları

- **`EnrollmentProgress`:** Arayüz enrollment'ta ([`…/enrollment/service/EnrollmentProgress.java:13`](../backend/src/main/java/com/miniudemy/enrollment/service/EnrollmentProgress.java)), uygulaması progress'te ([`…/progress/service/EnrollmentProgressService.java:19`](../backend/src/main/java/com/miniudemy/progress/service/EnrollmentProgressService.java)). "Kayıtlı kurslarım" listesi ilerleme yüzdesini gösterir, ama `enrollment` `progress`'e bağımlı olmaz.
- **`AccountLookup`:** Arayüz `common/security`'de ([`…/common/security/AccountLookup.java:15`](../backend/src/main/java/com/miniudemy/common/security/AccountLookup.java)), uygulaması auth'ta ([`…/auth/service/AccountLookupService.java:17`](../backend/src/main/java/com/miniudemy/auth/service/AccountLookupService.java)). Güvenlik altyapısı (`JwtAccountAuthenticationConverter:27`) her istekte kullanıcının güncel rolünü ve aktifliğini sorar, ama `auth` modülünü bilmez. `common` hiçbir modüle bağımlı olamaz (`ModuleArchitectureTest.commonDependsOnNoModule`).

### 3. Altyapıya değil soyutlamaya bağımlılık — `StorageService` (ADR-004)

```java
// …/content/service/ContentUploadService.java:29
private final StorageService storage;

// …/content/service/LocalDiskStorageService.java:37
public class LocalDiskStorageService implements StorageService {
```

Yükleme, sunum ve temizlik (`ContentUploadService`, `ContentDeliveryService`, `StoredFileCleanupListener`) diske nasıl yazıldığını bilmez. Hepsi `StorageService` arayüzüne bağımlıdır. Depolama yeri değişirse yalnız uygulama sınıfı değişir. ADR-004 gereği şu an tek uygulama yerel disktir (Docker volume). Testlerde arayüz kolayca taklit edilir (mock).

> Not: #95'te DIP örneği olarak anılan `PasswordResetNotifier` henüz kodda yok; parola yenileme #115'te açık. O yazıldığında aynı kalıbın (arayüz `auth`'ta, uygulama dev'de log'a yazan sınıf) bir örneği daha olacak.

---

## Nesne yönelimli kavramlar

### Kapsülleme — kurs durumu dışarıdan değiştirilemez

[`…/course/domain/Course.java:83`](../backend/src/main/java/com/miniudemy/course/domain/Course.java), [`…/course/domain/CourseStatus.java:21-30`](../backend/src/main/java/com/miniudemy/course/domain/CourseStatus.java)

```java
// Course
private CourseStatus status = CourseStatus.DRAFT;     // setter yok

// CourseStatus
DRAFT(DraftState.INSTANCE), ...
/** Bu değerin durum nesnesi; yalnız {@link Course} kullanır. */
CourseState state() {                                 // paket içi
    return state;
}
```

- `status` alanı `private` ve setter'ı yok. Durum yalnız `submit()`, `approve()`, `reject(gerekçe)` ve `unpublish(gerekçe)` ile değişir. Her biri önce durum nesnesine sorar, izin varsa alanı yazar.
- `CourseState` arayüzü, dört durum sınıfı ve `CourseStatus.state()` metodu **paket içidir** (`public` değil). `course/domain` dışındaki hiçbir kod durum nesnesine ulaşamaz. Servisler yalnız `course.submit()` gibi iş metotlarını görür.
- İş kuralları da veriyle birlikte durur: "onaya göndermek için içerikli en az bir ders gerekli" kontrolü servis yerine `Course.submit()` içinde, özel (`private`) `hasLessonWithContent()` metodundadır.
- Modül sınırında da aynı ilke geçerli: `Course`, eğitmeni ve kapak dosyasını entity olarak değil yalnız id olarak tutar (`instructorId`, `coverFileId`). Böylece başka modülün entity'si `course`'a sızmaz.

### Çok biçimlilik — aynı çağrı, farklı davranış

| Çağrı | Çalışan gövde neye göre seçilir | Yer |
|---|---|---|
| `state().submit()` | Kursun durumu (Taslak → İncelemede, İncelemede → 409, …) | `Course.java:241` |
| `strategy.score(input)` | Sınavın `scoringType`'ı | `QuizAttemptService.java:98` |
| `content.filePolicy()` | Dersin içerik türü | `ContentUploadService.java:43` |
| `notifications.notify…` ← olay | Olayın türü (Spring olay yayını) | `NotificationListener.java` |

`CourseState`'te çok biçimlilik varsayılan metotlarla kurulur. Arayüz her işlemi reddeder, her durum sınıfı yalnız kendi izinli işlemlerini ezer (`@Override`). Bu yüzden serviste `if (status == DRAFT)` ya da `switch (status)` yoktur. `CLAUDE.md` bunu kural olarak yazar, ADR-010 de "Enum + switch" alternatifini gerekçesiyle reddeder.

### Kalıtım ve soyutlama — şablon metot ve sealed hiyerarşiler

[`…/content/service/LessonContentFactory.java:15-26`](../backend/src/main/java/com/miniudemy/content/service/LessonContentFactory.java)

```java
public abstract class LessonContentFactory {

    public abstract LessonContentType type();

    /** Factory method: türün ürünü. */
    protected abstract LessonContent createContent();

    /** Türün ürünü; çağıranların kullandığı tek giriş. */
    public final LessonContent create() {
        return Objects.requireNonNull(createContent(), () -> getClass().getSimpleName() + " ürün üretmedi");
    }
}
```

- Alt sınıflar (`VideoContentFactory` vb.) yalnız `createContent()` metodunu ezer. Dışarıya açık `create()` metodu `final` olduğu için ortak kontrol (boş ürün yok) atlanamaz.
- `CourseState` ve `LessonContent` **sealed** tiplerdir: izinli alt türler `permits` ile sabitlenir. Hiyerarşi kapalıdır; yeni bir durum ya da ürün ancak bilinçli olarak listeye eklenerek gelir. Durum tarafında yeni bir `CourseStatus` sabiti, durum nesnesi verilmeden derlenmez.

---

## Bilinçli ödünleşimler

1. **State deseni ile JPA kalıcılığı (ADR-010).** Veritabanında durum nesnesi değil `CourseStatus` enum'u (`courses.status`) saklanır. Durum sınıfları veri taşımaz ve tekil (`INSTANCE`) nesnelerdir. Eşleme her enum sabitinin kurucusundadır. Bedeli şu: kurallar birkaç dosyaya bölünür (arayüz, dört durum, istisna). Ayrıca katalog sorgusu "yalnız Yayımlandı" filtresini SQL'de ayrıca uygular, yani görünürlük kuralı iki yerde durur. Bir de domain paketinden `common.exception`'a tek bir bağımlılık vardır: `CourseStateException extends ConflictException`. Bu izin ArchUnit'te adıyla yazılmıştır (`LayerArchitectureTest.domainOnlyDependsOnPersistenceJavaAndCommonException`). Kazanç: 409 her yerde aynı gövdeyle ve servislerde `try/catch` tekrarı olmadan döner.
2. **Port verisinin biçimini course belirler (DIP).** `CourseProgressStats.ProgressTotals`, `CourseRoster.RosterPage` ve `RosterEntry` gibi tipler `course` modülündedir. Yani `course`, başka modüllerin verisini hangi biçimde istediğini kendisi belirler, uygulayıcılar bu biçime uyar. İstatistik ekranı değiştikçe birden fazla modül değişebilir. Karşılığında bağımlılık yönü tek ve döngüsüz kalır.
3. **OCP tam kapalı değil (Strategy).** Yeni bir puanlama kuralı yeni bir strateji sınıfıdır, ama tek başına yetmez. `ScoringType` enum'una bir değer ([`…/quiz/domain/ScoringType.java:8`](../backend/src/main/java/com/miniudemy/quiz/domain/ScoringType.java)), `ck_quizzes_scoring_type` CHECK kısıtını genişleten yeni bir Flyway migration'ı ve OpenAPI'deki `ScoringType` güncellemesi de gerekir. Bunlar puanlama mantığı değil, saklanan değerin sözleşmesidir. Enum, veritabanı ve API'nin aynı değerleri kullanmasının bedeli budur. Eksik strateji ise üretimde değil, uygulama başlarken yakalanır (resolver kurucusu).
4. **Bir dinleyicide altı olay (SRP sınırı).** `NotificationListener` altı farklı olayı tek sınıfta dinler. Her olay için ayrı bir sınıf da yazılabilirdi. Tek sınıf seçildi, çünkü altı metodun hepsi aynı işi yapar (olayı `NotificationService`'e iletmek) ve aynı işlem kuralına uyar (AFTER_COMMIT + REQUIRES_NEW). Sınıfın değişme nedeni tektir: "hangi olay bildirim üretir". Bildirim mantığı serviste, metinler `NotificationMessages`'ta kalır.
