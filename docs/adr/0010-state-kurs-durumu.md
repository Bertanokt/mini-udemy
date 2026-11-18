# ADR-010: State deseni ile kurs yaşam döngüsü

- **Durum:** Kabul edildi
- **Karar tarihi:** 18.11.2026 (#85 planı; istisnanın yeri Bertan'ın onayıyla)
- **Kayıt tarihi:** 18.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #85, #23 (kurs düzenle/sil), #27 (onaya gönder), #59 (yönetici onay/red), #86 (ArchUnit), [ADR-001](0001-moduler-monolit.md), [`docs/architecture.md`](../architecture.md) › Kurs durumları

## Bağlam

Kurs dört durumdan birindedir: Taslak, İncelemede, Yayımlandı, Yayından kaldırıldı (`courses.status`, enum `CourseStatus`). Proje dokümanı hangi geçişin izinli olduğunu açıkça veriyor (FR-09), düzenleme ve silmeyi duruma bağlıyor (FR-10) ve yayından kaldırmayı tanımlıyor (FR-38). Bunların dışındaki her işlem 409 ile reddedilmeli.

Bu kurallar dört ayrı issue'nun uçlarında kullanılacak: kurs düzenleme ve silme (#23), onaya gönderme (#27), yönetici onayı ve reddi (#59), yayından kaldırma. Bölüm sınavlarının eklenmesi ve düzenlenmesi de "kurs düzenlenebilir mi" sorusunu soruyor: `QuizService`, #22'de yazılan `CourseOwnershipService.assertOwnerAndEditable`'ı çağırıyor. Bölüm ve ders uçları (#24, #25) da aynı soruyu soracak.

#85'ten önce kural `Course.isEditable()` içinde iki koşullu bir `if` olarak duruyordu; geçişlerin hiçbiri yazılmamıştı. Uçlar yazılmadan kuralın yeri belirlenirse dört issue aynı yerden beslenir.

## Karar

Kurs durumu **State** deseniyle yazılır; kodu `course/domain` paketindedir.

- **`CourseState`:** `sealed` ve paket içi bir arayüz; izin verilen uygulamaları yalnız `DraftState`, `InReviewState`, `PublishedState`, `UnpublishedState`. Altı işlemi var:
  - kontroller: `assertEditable()`, `assertDeletable()`;
  - geçişler: `submit()`, `approve()`, `reject()`, `unpublish()` (hedef `CourseStatus`'u döner);
  - sorgu: `isPublished()`.
- **Varsayılan red:** arayüzdeki her işlemin varsayılan gövdesi `CourseStateException` (409) fırlatır. Her durum sınıfı yalnız kendi izinli işlemlerini ezer. Yeni bir işlem eklenirse, açıkça izin vermeyen her durumda kendiliğinden reddedilir.
- **Durum nesneleri veri tutmaz:** her birinin tek bir örneği (`INSTANCE`) var. Değişen tek şey kursun `status` alanı.
- **Eşleme tek yerde:** her `CourseStatus` sabiti kendi durum nesnesiyle kurulur (`DRAFT(DraftState.INSTANCE)` …). Durum nesnesi olmayan yeni bir sabit derlenmez.
- **Tek giriş noktası `Course`:** `submit()`, `approve()`, `reject(gerekçe)`, `unpublish(gerekçe)`, `assertEditable()`, `assertDeletable()`, `isPublished()`. Her biri `status.state()`'e sorar. `status` alanının dışarıdan değiştirilmesi için setter yok. Servislerde durum için if/switch yazılmaz.
- **Gerekçeler:**
  - Red gerekçesi zorunludur ve `rejection_reason`'da saklanır. Boşsa `IllegalArgumentException` fırlar; istekte `@NotBlank` ile doğrulanması #59'un işidir.
  - Onaya gönderince önceki red gerekçesi temizlenir (#27'nin kriteri, ama geçişin parçası).
  - Yayından kaldırma gerekçesi isteğe bağlıdır: yönetici gerekçeyle, eğitmen gerekçesiz kaldırabilir (FR-38). `unpublish_reason`'da saklanır.

**Geçiş tablosu** (satır: şu anki durum, sütun: işlem; ✓ izinli kontrol, → hedef durum, 409 red):

| | Düzenle | Sil | Onaya gönder | Onayla | Reddet | Yayından kaldır |
|---|---|---|---|---|---|---|
| Taslak | ✓ | ✓ | → İncelemede | 409 | 409 | 409 |
| İncelemede | 409 | 409 | 409 | → Yayımlandı | → Taslak | 409 |
| Yayımlandı | 409 | 409 | 409 | 409 | 409 | → Yayından kaldırıldı |
| Yayından kaldırıldı | ✓ | 409 | → İncelemede | 409 | 409 | 409 |

Katalogda yalnız Yayımlandı görünür (`PublishedState.isPublished()`).

### İstisnanın yeri: domain'den `common.exception`'a tek bağımlılık

`CourseStateException`, `common.exception.ConflictException`'dan türer ve `course/domain`'de durur. Bu, bir domain paketinden `common`'a ilk bağımlılıktır. Gerekçe:

- **Hata gövdesi kendiliğinden standart:** istisna `ApiException` olduğu için global handler 409 ve standart hata gövdesiyle döner. Handler değişmez (`CLAUDE.md` › Standart hata formatı: yeni hata türü `ApiException`'dan türer, handler değişmez).
- **Alternatif dört yerde tekrar ederdi:** domain kendi `RuntimeException`'ını fırlatsaydı, onu 409'a çeviren `try/catch` #23, #27, #59 ve yayından kaldırma servislerinde ayrı ayrı yazılırdı. Unutulan yer 500 döndürürdü. Handler'a eklemek ise "handler'a yalnız Spring'in yakalanmamış istisnaları eklenir" kuralını bozardı.

**#86'nın kurallarıyla ilişkisi.** #86, `docs/architecture.md` §3–4'teki kuralları ArchUnit'le denetleyecek. Bu karar o kuralların hiçbirini gevşetmiyor:

| Kural (architecture.md) | Bu kararla durumu |
|---|---|
| §3: `domain` hiçbir katmana bağımlı değildir (controller, service, repository, dto, mapper) | Değişmedi. `common.exception` bu katmanlardan biri değil; domain hâlâ hiçbir modülün controller, service, repository, dto ya da mapper paketine erişmiyor. |
| §4: başka modülün `repository` ve entity paketlerine erişilmez; yalnız service arayüzü ve olaylar | Değişmedi. `common` iş modülü değil, iş kuralı içermeyen ortak altyapı (`CLAUDE.md` › Modüller). main'de service kodu olan yedi modülün hepsinin service'leri zaten `common.exception`'ı kullanıyor; progress, certificate ve notification'ın henüz service'i yok. |
| §4: `common` hiçbir modüle bağımlı değildir | Değişmedi. Bağımlılık yönü `course` → `common`. |
| #224: service, dinleyiciyi (`listener`) doğrudan çağırmaz (ADR-009) | İlgisiz. |

İzin dar tutulur: domain yalnız `common.exception`'ı kullanabilir; `common.security`, `common.config` ve `common.mapper` domain için yasak kalır. #86'daki domain kuralı bu izni adıyla yazmalıdır. Kuralı "domain yalnız şunlara bağımlı olabilir: kendi domain paketi, `java..`, `jakarta.persistence..`, `org.hibernate.annotations..`, `com.miniudemy.common.exception..`" biçiminde kurmak, bu kararı ve geri kalan yasağı birlikte denetler. `docs/architecture.md` §3'teki katman tablosu ve `CLAUDE.md` bu istisnayla güncellendi.

## Değerlendirilen alternatifler

### Enum + switch (servislerde ya da `Course`'ta)

Her işlem `switch (status)` ile izinli durumları sayar. 4 durum × 6 işlem için kısa bir kod.

- **Artıları:** sınıf sayısı az; tablo tek bakışta okunur.
- **Neden seçilmedi:**
  - Her işlem ayrı bir switch; kurallar dört issue'nun servislerine dağılır. #85'in kabul kriteri ve `CLAUDE.md` serviste durum için if/switch'i açıkça yasaklıyor.
  - Yeni bir durum eklenince her switch'in elle güncellenmesi gerekir. `default` dalı olan bir switch'te derleyici eksik dalı haber vermez.
  - Dersin değerlendirdiği tasarım deseni kullanımını göstermez.

### Enum sabitlerinde metot gövdeleri (her `CourseStatus` sabiti kendi kurallarını ezer)

Kurallar ayrı sınıf yerine enum'un içinde olur. Teknik olarak State'e en yakın alternatif ve daha kısa.

- **Neden seçilmedi:** veritabanı değeri ile iş kuralları aynı türde karışır; enum hem kalıcılık eşlemesi hem davranış taşır. Ayrı sınıflar her durumun kurallarını kendi dosyasında, sınıf diyagramında da ayrı kutular olarak gösterir. Enum yalnız eşlemeyi tutar.

### Spring Statemachine

Durumlar, olaylar ve geçişler bir yapılandırmayla tanımlanır; kütüphane geçişleri yürütür.

- **Artıları:** büyük ve iç içe durum makineleri, geçiş dinleyicileri, makinenin kalıcı saklanması hazır gelir.
- **Neden seçilmedi:**
  - Dört durum, altı işlem için yeni ve ağır bir bağımlılık (`CLAUDE.md`: yeni bağımlılık sorulmadan eklenmez).
  - Kurs durumu zaten `courses.status`'ta saklanıyor; makinenin kendi durumunu ayrıca saklamak ya da her istekte yeniden kurmak gerekirdi.
  - Olay temelli API, "bu işlem bu durumda izinli mi, değilse 409" sorusunu basit bir metot çağrısından daha dolaylı yapıyor.
  - Ekibin öğrenmesi gereken bir yapılandırma dili ekliyor.

  Projede denenmedi; değerlendirme kütüphanenin dokümantasyonuna dayanıyor.

## Sonuçlar

**Olumlu:**

- Durum kuralları tek yerde. #23, #27, #59 ve yayından kaldırma uçları yalnız `course.submit()`, `course.approve()` gibi çağrılar yapar.
- İzinsiz işlem her yerde aynı istisna ve aynı Türkçe mesajla 409 döner.
- Yeni bir durum, yeni bir sınıf demek: `sealed` listesine ve enum eşlemesine eklenmeden derlenmez.
- `isEditable()` kaldırıldı; `CourseOwnershipService` yalnız `course.assertEditable()` çağırıyor.

**Olumsuz / ödünleşimler:**

- 6 yeni sınıf (arayüz, dört durum, istisna); kural tablosu birkaç dosyaya bölünüyor. Tablonun tamamı `docs/architecture.md`'de ve testte tek yerde duruyor.
- Domain'den `common.exception`'a bir bağımlılık (yukarıda gerekçesi ve sınırı).
- **Katalog görünürlüğü iki yerde:** katalog sorgusu (`CourseRepository`) "yalnız Yayımlandı" filtresini veritabanında uyguluyor (`c.status = PUBLISHED`). Bu bir iş kuralı değil, sorgu filtresi; ama görünürlük kuralı değişirse hem `PublishedState.isPublished()` hem bu sorgu değişmeli.
- **Eski yayından kaldırma gerekçesi kalıyor:** yeniden onaylanan bir kursta önceki `unpublish_reason` silinmiyor. Proje dokümanı bunu tanımlamıyor; onay ucu yazılırken (#59) karar verilecek.

**Kararın kodda nasıl korunduğu:**

- `CourseStateTest`: 4 durum × 6 işlemin 24 birleşiminin tamamı (izinsizde 409 ve değişmeyen durum), gerekçelerin yazılıp temizlenmesi, gerekçesiz red ve her `CourseStatus`'un kendi durum nesnesine eşlendiği. Her durum sınıfında, eşlemede ve `Course`'ta kural bilerek bozulduğunda bu test kırıldı (#85 PR'ı).
- `CourseOwnershipServiceTest`: sahiplik servisinin düzenlenebilirliği durum nesnesine sorduğu.
- `LayerArchitectureTest.domainOnlyDependsOnPersistenceJavaAndCommonException` (#86): domain'in yalnız izin verilen paketlere bağımlı olduğu (yukarıdaki kural biçimi); domain'e `common.security` bağımlılığı bilerek eklendiğinde kırıldı.
- `CLAUDE.md` › Tasarım desenleri: "Kurs durumuna bağlı her izin ve geçiş durum nesnesine sorulur; serviste status için if/switch yok."
