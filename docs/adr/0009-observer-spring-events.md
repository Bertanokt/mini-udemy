# ADR-009: Observer: Spring domain olayları, AFTER_COMMIT ve senkron teslim

- **Durum:** Kabul edildi
- **Karar tarihi:** 17.11.2026 (#42'nin uygulanması sırasında; yaklaşım seçimi #42'nin kabul kriteriydi)
- **Kayıt tarihi:** 17.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #42, #30 (ilk yayıncı), #124, [ADR-001](0001-moduler-monolit.md), [`docs/architecture.md`](../architecture.md) › Domain olayları

## Bağlam

Kayıt, ders tamamlama, kurs onayı/reddi, sınav sonucu ve sertifika gibi olaylara birden fazla modül tepki verir (ilerleme hesabı, sertifika uygunluğu, bildirim). Yayıncı modül bunları doğrudan çağırırsa modüller birbirine sıkı bağlanır, modül bağımlılık tablosunda döngüler oluşur ve her yeni tepki yayıncı kodunu değiştirir. Üç şey kararlaştırılmalıydı: olaylar nasıl taşınacak, hangi anda teslim edilecek, teslim senkron mu asenkron mu.

## Karar

Modüller arası tepkiler Spring'in uygulama içi olaylarıyla kurulur.

- **Olay tipleri** yayınlayan modülün `domain` paketinde Java `record`'tur: `EnrollmentCreated` (`enrollment`), `LessonPublished`, `CourseApproved`, `CourseRejected` (`course`), `LessonCompleted` (`progress`), `QuizCompleted` (`quiz`), `CertificateIssued` (`certificate`). Olay, işi yapan modüle aittir; dinleyen modül olay tipini içe aktarır, tersi olmaz. Olaylar entity taşımaz, yalnız id ve basit değerler taşır.
- **Yayınlama** service içinde `ApplicationEventPublisher.publishEvent` ile ve `@Transactional` yöntemin içinde yapılır.
- **Dinleyiciler** dinleyen modülün `listener` paketindedir ve `@TransactionalEventListener(phase = AFTER_COMMIT)` kullanır.
- **Senkron teslim varsayılandır:** dinleyici, yayıncının thread'inde commit'ten hemen sonra çalışır. `@Async` yalnız ölçülmüş bir ihtiyaç doğarsa ve ayrı bir ADR ile eklenir.
- Dinleyici AFTER_COMMIT'te çalıştığı için işlem artık bitmiştir; veritabanına yazacaksa `@Transactional(propagation = REQUIRES_NEW)` ile kendi işlemini açar. Yayıncının işlemi commit edilmiş olduğundan dinleyici hatası onu geri almaz.
- **Kural:** hiçbir `service` sınıfı bir `listener` paketine bağımlı olamaz. Bu `ObserverArchitectureTest` (ArchUnit) ile denetlenir.

AFTER_COMMIT dışında bir faz gerekiyorsa (örneğin yayıncıyla aynı işlemde atomik olması gereken bir tepki) o dinleyici gerekçesiyle birlikte ayrıca yazılır.

## Değerlendirilen alternatifler

### Doğrudan servis çağrısı

Yayıncı service, tepki verecek modüllerin service'lerini doğrudan çağırır. Basit ve izlenmesi kolaydır, ama yayıncı her dinleyiciyi tanımak zorunda kalır: `progress`, `certificate` ve `notification` birbirine ve yayıncıya bağlanır, bağımlılık tablosunda döngü riski doğar ve yeni bir tepki eklemek yayıncının kodunu değiştirir (Open/Closed ihlali). Seçilmedi.

### Mesaj kuyruğu (RabbitMQ, Kafka vb.)

Olaylar dış bir aracıya yazılır. Dinleyicilerin ayrı süreçte çalışmasını ve yeniden denemeyi sağlar, ama tek süreçli bir modüler monolit için ek altyapı, dağıtım ve tutarlılık maliyeti getirir (outbox gibi desenler gerekir). Dört kişilik ekip ve beş haftalık süre için orantısızdır; ADR-001 ile uyumsuzdur. Seçilmedi; modüller ayrılırsa yeniden değerlendirilir.

### Düz `@EventListener` (işlem fazı olmadan)

Olay `publishEvent` çağrıldığı anda, işlem henüz bitmeden teslim edilir. Yayıncı sonradan rollback olursa dinleyici hiç olmamış bir kayıt için bildirim üretmiş ya da ilerleme yazmış olur. Seçilmedi; AFTER_COMMIT bu durumu engeller.

### `@Async` dinleyiciler

Yayıncıyı dinleyici süresinden ayırır, ama hata yönetimi, thread havuzu ve testte belirsizlik getirir; şu an ölçülmüş bir gecikme sorunu yoktur. Varsayılan olarak seçilmedi.

## Sonuçlar

**Olumlu:**

- Yayıncı dinleyicileri tanımaz; yeni tepki yeni bir `listener` sınıfıdır, yayıncıya dokunulmaz.
- Rollback olan işlem için hiçbir yan etki oluşmaz.
- Ek altyapı yoktur; testler Docker gerektirmez.

**Olumsuz / ödünleşimler:**

- Dinleyici senkron çalıştığı için yavaş bir dinleyici isteği geciktirir.
- AFTER_COMMIT'te yayıncı ile dinleyici arasında atomiklik yoktur: dinleyici çökerse olay kaybolur (yeniden deneme yok). Bildirim ve ilerleme gibi telafisi olan tepkiler için kabul edilmiştir; kayıp olamayacak bir tepki için outbox ya da ayrı karar gerekir.
- Olay akışı kodda doğrudan çağrı kadar kolay izlenmez; bu yüzden `docs/architecture.md` › Domain olayları tablosu güncel tutulur.

**Kararın kodda nasıl korunduğu:**

- `ObserverArchitectureTest`: `..service..` paketindeki sınıflar `..listener..` paketlerine bağımlı olamaz (yalnız bu kural yazılıdır; genel katman ve modül sınırı kuralları #86'dadır).
- `EnrollmentCreatedDeliveryTest`: gerçek `EnrollmentService` ile olayın commit öncesi teslim edilmediği, commit sonrası edildiği, rollback ve hata durumunda hiç edilmediği gösterilir.
- `NotificationScenarioIT` (#124): gerçek HTTP ve PostgreSQL ile altı bildirim olayı commit'ten sonra doğru alıcıya tek bildirim yazar; yayıncının işi başarısız olunca (mükerrer kayıt 409) bildirim yazılmaz.
- Yeni bir olay ya da dinleyici eklenince `docs/architecture.md` tablosu aynı PR'da güncellenir.
