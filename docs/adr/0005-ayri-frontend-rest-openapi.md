# ADR-005: Ayrı frontend, REST API ve önce OpenAPI sözleşmesi

- **Durum:** Kabul edildi
- **Karar tarihi:** 15.10.2026 (repo iskeleti: ayrı Next.js uygulaması ve REST, `479fc03`); sözleşmenin koddan önce yazılması 18.10.2026 (backlog planlaması, `5eada73`)
- **Kayıt tarihi:** 13.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #84, #3 · #4 · #5 · #108 (OpenAPI sözleşmesi), #16 (frontend API katmanı), [ADR-001](0001-moduler-monolit.md)

## Bağlam

Mini Udemy'nin arayüzü etkileşimli: video ve PDF oynatıcısı, sınav çözme, ilerleme göstergeleri, bildirim zili, rol bazlı paneller; mobilde de kullanılabilmeli (NFR-07). Kısıtlar:

- **Ekip:** 4 kişi aynı haftalarda hem backend hem frontend işlerine dağılmış durumda (`CONTRIBUTING.md` › Roller: herkes en az bir modülü backend ve frontend tarafıyla geliştirir). Bir ekranın frontend'i, backend ucu bitmeden başlayabilmeli.
- **Roller:** API sözleşmesinin sahibi Veri ve Entegrasyon sorumlusu (Eray); backend ve frontend işleri farklı kişilerde.
- **Değerlendirme:** API tasarımının belgelenmesi ve kodla tutarlı olması isteniyor; standart bir hata biçimi tüm uçlarda aynı olmalı.
- **Teknoloji:** backend Spring Boot (ADR-001).

İki soruya karar vermek gerekiyordu: arayüz nerede üretilecek ve backend ile nasıl konuşacak; uçların biçimi önce kodda mı, önce dokümanda mı belirlenecek.

## Karar

Frontend **ayrı bir Next.js uygulamasıdır** ve backend ile **REST + JSON** üzerinden konuşur. Bir uç kodlanmadan önce **`docs/api/openapi.yaml` sözleşmesine yazılır**; backend ve frontend bu sözleşmeye göre geliştirilir.

- **REST:** kaynaklar `/api/` altında, çoğul ve kebab-case (`/api/courses/{id}/sections`); kullanıcının kendi kaynakları `/api/me/...`; anlam HTTP yöntemi ve durum koduyla taşınır (`CLAUDE.md` › İsimlendirme).
- **Sözleşme önce:** yeni ya da değişen bir uç önce `openapi.yaml`'da tanımlanır; uç kodda değişirse sözleşme aynı PR'da güncellenir (`CLAUDE.md` › Kod değişirse doküman da değişir, NFR-15). Tüm hatalar sözleşmedeki tek `ErrorResponse` gövdesiyle döner (#11).
- **Frontend tarafı:** sayfalar `fetch`'i doğrudan çağırmaz; tüm istekler `frontend/src/lib/api/client.ts`'teki `apiFetch` üzerinden ve modül başına `src/lib/api/<modül>.ts` dosyalarıyla yapılır. Tipler sözleşmedeki şemalardan yazılır.
- **Bağlantı:** tarayıcı backend'e `NEXT_PUBLIC_API_URL` ile ulaşır (`docker-compose.yml` › `frontend.build.args`); backend izin verilen kökeni `CORS_ALLOWED_ORIGINS` ile alır (`common/config/WebConfig`).

## Değerlendirilen alternatifler

### Sunucu tarafı şablonlar (Spring MVC + Thymeleaf)

Sayfalar backend'de HTML olarak üretilir; ayrı bir frontend uygulaması yoktur.

- **Artıları:** tek uygulama ve tek dağıtım; CORS yok; oturum çerezle basit; sayfa ile veri aynı yerde.
- **Neden seçilmedi:** oynatıcı, sınav ve bildirim gibi etkileşimli ekranlar için yine yoğun JavaScript gerekiyor ve bu kod şablonlara dağılıyor. Bir ekranın arayüzü ve ucu aynı kişiye ve aynı sıraya bağlanıyor; frontend işi backend bitmeden başlayamıyor. API'nin kendisi belgelenmiş bir sözleşme olarak ortaya çıkmıyor.

### GraphQL

Tek bir uç, istemcinin istediği alanları sorguladığı bir şema.

- **Artıları:** istemci yalnız ihtiyacı olan veriyi ister; tek şema hem dokümantasyon hem tip kaynağıdır.
- **Neden seçilmedi:** hata modelimiz HTTP durum kodlarına dayanıyor (401, 403, 404, 409, 413, 415); gereksinimlerimiz bu kodları tanımlıyor (ör. FR-06, FR-10, FR-11, NFR-09) ve testlerimiz onları bekliyor. GraphQL yanıtları çoğunlukla 200 ile döner. Dosya yükleme (multipart) ve video akışı (`Range`) GraphQL'in doğal alanı değil, yine REST uçları gerekiyor. Ekip için ek öğrenme yükü getiriyor; ekranlarımızın veri ihtiyacı REST'le rahatça karşılanıyor.

### Kod önce sözleşme (springdoc ile koddan üretilen OpenAPI)

Sözleşme ayrı yazılmaz; backend kodundaki anotasyonlardan otomatik üretilir.

- **Artıları:** sözleşme ile kod arasında sapma olamaz; elle bakım yok.
- **Neden seçilmedi:** sözleşme ancak backend ucu yazıldıktan sonra var oluyor; frontend o ana kadar bekliyor. Uç tasarımı (yol, alanlar, hata kodları) review'da kod ayrıntısına karışıyor. Sözleşmenin sahibinin (Veri ve Entegrasyon) tasarımı kod yazılmadan önce ortaya koyabilmesi bu rol dağılımının temel nedeni.

## Sonuçlar

**Olumlu:**

- **Paralel çalışma:** sözleşme yazıldığında frontend ve backend aynı anda başlayabiliyor; frontend tipleri ve hata işleme sözleşmeden yazılıyor.
- **Tek referans:** review'da "bu uç ne döner?" sorusunun cevabı tek bir dosya; ekip içi tartışmalar kodda değil sözleşmede bitiyor.
- **Ortak biçimler tek yerde:** `ErrorResponse`, sayfalama ve kimlik doğrulama şeması bir kez tanımlanıp her uçta tekrar kullanılıyor (#11'deki global hata yönetimi bu tanıma göre yazıldı).
- **Bağımsız arayüz:** Next.js tarafı mobil uyumlu ve etkileşimli ekranlar için uygun; ileride başka bir istemci (mobil uygulama) aynı API'yi kullanabilir.

**Olumsuz / ödünleşimler:**

- **İki uygulama:** iki ayrı derleme, iki Docker imajı, iki CI işi; yerelde iki süreç çalıştırmak gerekiyor.
- **CORS ve adres ayarı:** frontend'in kökeni backend'de izinli olmalı; `NEXT_PUBLIC_API_URL` derleme sırasında koda gömüldüğü için adres değişince frontend yeniden derlenmeli.
- **Sapma riski:** sözleşme ve kod elle senkron tutuluyor. Şu an CI'da sözleşmeyi kodla karşılaştıran otomatik bir kontrol yok; uyum PR review'unda ve "aynı PR'da güncelle" kuralıyla korunuyor.
- **Kimlik doğrulama tarayıcıda:** oturum çerez yerine istemcinin taşıdığı bir token ile yürüyor (ayrıntı ve güvenlik kararları ADR-002'de, #81).

**Kararın kodda nasıl korunduğu:**

- `CLAUDE.md` › Kod değişirse doküman da değişir: uç eklendi ya da değişti → `docs/api/openapi.yaml` aynı PR'da.
- PR şablonu › "Güncellenen dokümanlar" bölümü her PR'da sözleşmenin güncellenip güncellenmediğini sorar.
- `CLAUDE.md` › İsimlendirme: frontend'de API çağrıları yalnız `src/lib/api/<modül>.ts` üzerinden (`apiFetch`); sayfalar `fetch`'i doğrudan çağırmaz.
- Hata gövdesi `GlobalExceptionHandlerTest` ile sözleşmedeki örneklere karşı test ediliyor (ör. 409 `errors.email` örneği).
