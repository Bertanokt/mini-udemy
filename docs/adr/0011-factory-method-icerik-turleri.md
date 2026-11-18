# ADR-011: Factory Method ile ders içerik türleri

- **Durum:** Kabul edildi
- **Karar tarihi:** 18.11.2026 (#110 planı, Bertan'ın onayıyla)
- **Kayıt tarihi:** 18.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #110, #111 (depolama, `FilePolicy`), #112 (yükleme), #113 (yetki kontrollü sunum), [ADR-004](0004-yerel-dosya-depolama.md), [`docs/architecture.md`](../architecture.md) › Tasarım desenleri

## Bağlam

Bir dersin içeriği üç türden biridir (`lessons.content_type`, enum `LessonContentType`). Türler hem yüklemede hem sunumda farklı davranır (proje dokümanı FR-11, NFR-11, NFR-12; `docs/api/openapi.yaml` › `LessonContentType`):

| Tür | Kabul edilen gerçek türler | En büyük boyut | Sunuluş |
|---|---|---|---|
| VIDEO | mp4, webm | 200 MB | tarayıcıda oynatılır, ileri sarma (HTTP Range, 206) |
| PDF | pdf | 20 MB | tarayıcıda açılır |
| DOCUMENT | pdf, docx, pptx | 20 MB | indirilir |

Bu farklar iki ayrı yerde kullanılacak: yükleme ucu (#112) kabul kuralını, yetki kontrollü sunum ucu (#113) sunuluş şeklini soracak. Depolama (#111) türleri bilmez; kendisine verilen `FilePolicy`'yi uygular (ADR-004 eki). Kural yeri bu iki uç yazılmadan belirlenmeli.

## Karar

Ders içerik türleri **Factory Method** deseniyle yazılır.

- **Ürün:** `content/domain/LessonContent` (soyut, `sealed`), üç somut ürünüyle: `VideoContent`, `PdfContent`, `DocumentContent`.
  - Her ürün `filePolicy()` (kabul edilen türler, boyut sınırı, 413/415 mesajları), `deliveryMode()` (`INLINE` / `ATTACHMENT`) ve `supportsRangeRequests()` verir.
  - Ürünler Spring'e ve `course` modülüne bağımlı değildir.
- **Yaratıcı:** `content/service/LessonContentFactory` (soyut).
  - Factory method'u `createContent()`; çağıranların kullandığı tek giriş `create()`.
  - Her türün kendi alt sınıfı vardır ve bir Spring bean'idir: `VideoContentFactory`, `PdfContentFactory`, `DocumentContentFactory`. Her fabrika hangi `LessonContentType` için çalıştığını `type()` ile söyler.
- **Seçim tek yerde:** `content/service/LessonContentFactories`, fabrika bean'lerini kendi `type()` değerleriyle bir `EnumMap`'e koyar; `forType(type)` if/switch olmadan okur. Bir türün fabrikası yoksa ya da iki fabrika aynı türü iddia ederse kurucu `IllegalStateException` fırlatır, yani uygulama başlamaz.
- **Kullanım:**
  - Yükleme: `factories.forType(lesson.contentType()).create().filePolicy()` → `storageService.store(…, policy, …)` (#112).
  - Sunum: aynı ürünün `deliveryMode()` ve `supportsRangeRequests()`'i (#113).
  - Çağıranlarda tür için if/switch yoktur. Yeni bir tür (ör. ses) = yeni bir ürün + yeni bir fabrika + enum'a bir değer.
- **Mesajlar:**

  | Tür | 413 | 415 |
  |---|---|---|
  | Video | "Video en fazla 200 MB olabilir." | "Yalnız mp4 ve webm videolar yüklenebilir." |
  | PDF | "PDF en fazla 20 MB olabilir." | "Yalnız pdf dosyaları yüklenebilir." |
  | Doküman | "Doküman en fazla 20 MB olabilir." | "Yalnız pdf, docx ve pptx dokümanlar yüklenebilir." |

Sunuluşu dosyanın biçimi değil dersin türü belirler: doküman dersine yüklenen bir PDF indirilir, `Content-Type` ise dosyanın kendi gerçek türünden (`FileFormat.mimeType()`) gelir.

## Değerlendirilen alternatifler

### Tek sınıfta switch

Bir `ContentRules` sınıfı `switch (type)` ile her türün kuralını ve sunuluşunu döner.

- **Artıları:** tek dosya, kısa; üç tür için tablo gibi okunur.
- **Neden seçilmedi:**
  - Yükleme ve sunum ayrı switch'ler yazar ya da aynı switch'e bağımlı olur.
  - Yeni bir tür eklenince her switch'in elle güncellenmesi gerekir; `default` dalı varken derleyici eksik dalı haber vermez.
  - #110'un kabul kriteri ve `CLAUDE.md` servislerde tür için if/switch'i açıkça yasaklıyor.
  - Dersin değerlendirdiği tasarım deseni kullanımını göstermez.

### Abstract Factory

Her tür için birbiriyle ilişkili bir ürün ailesi üreten fabrika (ör. doğrulayıcı + sunucu + önizleyici).

- **Artıları:** bir türün birden fazla birlikte değişen nesnesi olsaydı aileyi tutarlı tutardı.
- **Neden seçilmedi:** bugün her tür için tek bir ürün var: kural ve sunuluş. Ürünü tek bir nesne verdiğinde aile kavramı yalnız ek arayüz ve sınıf getirir. İleride türe özgü birden fazla nesne gerekirse (ör. video için ayrı bir akış sunucusu) bu fabrika Abstract Factory'ye genişletilebilir.

## Sonuçlar

**Olumlu:**

- Türün bütün kuralı kendi ürün sınıfında: 413/415 mesajları, boyut ve sunuluş tek yerde test ediliyor.
- Yükleme ve sunum türleri bilmiyor; ikisi aynı ürünü kullandığı için kabul kuralıyla sunuluş birbirinden kopmuyor.
- Eksik ya da çift fabrika uygulamanın açılışında yakalanıyor; çalışma anında `null` fabrika dönmüyor.

**Olumsuz / ödünleşimler:**

- 3 tür için 9 sınıf (soyut ürün, 3 ürün, soyut yaratıcı, 3 fabrika, kayıt) ve bir enum (`DeliveryMode`). Tek sınıflık switch'e göre daha fazla dosya.
- Yeni bir tür üç yerde değişiklik ister: `LessonContentType` (veritabanı CHECK kısıtıyla birlikte), yeni ürün, yeni fabrika. Kayıt eksik fabrikayı açılışta yakaladığı için unutulamaz.
- `content` modülü `course`'un `LessonContentType` enum'una bağımlı. `docs/architecture.md` tablosunda `content → course` izinli; `ModuleArchitectureTest` başka modülden enum kullanımına izin veriyor (#86).

**Kararın kodda nasıl korunduğu:**

- `VideoContentFactoryTest`, `PdfContentFactoryTest`, `DocumentContentFactoryTest`: türün ürünü, kabul edilen türler, sınır, mesajlar, sunuluş ve Range.
- `LessonContentFactoriesTest`: her türün fabrikası çözülür; eksik ve çift fabrika açılışta hata verir. `LessonContentFactoriesWiringTest`: kayıt Spring bean'leriyle kurulur.
- Her kural bilerek bozulduğunda ilgili test kırıldı (#110 PR'ı).
- `CLAUDE.md` › Tasarım desenleri: "Türe özgü biçim, boyut ve sunuluş kuralları türün fabrikası/ürünündedir; yükleme servisi yalnız ortak arayüzü bilir."
