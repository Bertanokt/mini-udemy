# ADR-004: Yerel dosya depolama (Docker volume) ve yetki kontrollü servis

- **Durum:** Kabul edildi
- **Karar tarihi:** 22.10.2026 (backlog'un proje tanım dokümanına göre yeniden planlanması, `f483d28`)
- **Kayıt tarihi:** 13.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #84, #111 (depolama), #112 (yükleme), #113 (yetki kontrollü servis), [ADR-001](0001-moduler-monolit.md)

## Bağlam

Eğitmenler derslere video, PDF ve yardımcı doküman, kurslara kapak görseli yükler; kayıtlı kursiyerler bu içerikleri kurs oynatıcısında açar (proje dokümanı modül C; FR-11, FR-12). Kısıtlar:

- **Erişim kuralı:** ders içeriği yalnız kursa kayıtlı kursiyere, kursun eğitmenine ve yöneticiye açıktır; dosyaya doğrudan (yetkisiz) URL ile ulaşılamaz (FR-06, NFR-03). Bu kural proje dokümanı Bölüm 6'nın test durumlarından biridir.
- **Dosya kuralları:** video (mp4, webm) en fazla 200 MB, PDF ve doküman (pdf, docx, pptx) en fazla 20 MB, kapak görseli (jpg, png, webp) en fazla 2 MB; tür uzantıdan değil içerikten denetlenir; sınır aşımı 413, yanlış tür 415 (FR-11, NFR-11, NFR-12).
- **Sunum:** video ileri sarılabilmeli ve dosyalar belleğe tümüyle alınmadan akış olarak gönderilmeli (NFR-02).
- **Kurulum:** sistem temiz bir makinede tek komutla (`docker compose up --build`) ayağa kalkmalı (NFR-16); ücretli ya da hesap gerektiren dış servis yok.
- **Ekip ve süre:** 4 kişi, yaklaşık 5 hafta (ADR-001).

İlk planda (18.10.2026, `5eada73`) bu ADR "video embed, dosya yükleme yok" kararını kaydedecekti. Proje tanım dokümanı dosya yüklemeyi istediği için karar 22.10.2026'daki yeniden planlamada aşağıdaki gibi değişti; o embed kararı hiç uygulanmadı.

## Karar

Yüklenen dosyalar **backend konteynerine bağlı bir Docker volume'da, yerel diskte** saklanır ve **yalnız backend üzerinden, her istekte yetki kontrolüyle** sunulur.

- **Saklama:** `content` modülünde bir `StorageService` arayüzü ve yerel disk uygulaması (#111). Diskteki dosya adını sunucu üretir (UUID); kullanıcının verdiği ad ya da yol kullanılmaz. Dosyanın meta verisi veritabanında `stored_files` tablosunda tutulur: `storage_key` (UUID), `original_name`, `mime_type`, `size_bytes`, `uploader_id` ([ER diyagramı](../diagrams/er.puml)).
- **Yükleme denetimi (#111, #112):** tür dosyanın ilk baytlarına (magic bytes) bakılarak belirlenir; türe özgü boyut sınırı Factory Method ile seçilen içerik türündedir (`LessonContentFactory`, [`docs/architecture.md`](../architecture.md) › Tasarım desenleri). Sınır aşımı `PayloadTooLargeException` (413), izin verilmeyen tür `UnsupportedMediaTypeException` (415) olarak standart hata gövdesiyle döner (`common/exception`, `docs/api/openapi.yaml` › `components.responses.PayloadTooLarge`, `UnsupportedMediaType`).
- **Sunum (#113):** `GET /api/lessons/{id}/content` kursa kayıtlı kursiyere, kursun eğitmenine ve yöneticiye dosyayı verir; diğerlerine 403, girişsize 401. Video için HTTP `Range` istekleri desteklenir (206 Partial Content). `Content-Type` ve `Content-Disposition` içerik türüne göre ayarlanır. Kapak görselleri herkese açık bir uçtan sunulur.
- **Dağıtım:** `docker-compose.yml`'a `pgdata` volume'unun yanına bir uploads volume'u ve `application.yml`'a depolama dizini ile multipart sınırları eklenir (#111).

## Değerlendirilen alternatifler

### Nesne depolama: MinIO ya da Amazon S3

Dosyalar ayrı bir nesne deposunda tutulur; tarayıcıya süreli, imzalı bir adres (presigned URL) verilir.

- **Artıları:** uygulama sunucusundan bağımsız ölçeklenir; büyük dosyaları backend taşımaz; yedekleme ve sürümleme depo tarafından sağlanır.
- **Neden seçilmedi:** S3 hesap ve ücret gerektiriyor, temiz makinede tek komutla demo kuralına uymuyor. MinIO yerel çalışır ama Compose'a bir servis, erişim anahtarları ve kova ayarı ekliyor. İmzalı adres süresi dolana kadar herkesle paylaşılabildiği için "her istekte kayıt kontrolü" kuralını (NFR-03) zayıflatıyor. Tek sunuculu, birkaç kullanıcılı bir demo için bu karmaşıklık fayda getirmiyor.

### Statik dosya sunucusu (ör. nginx, volume'u doğrudan sunar)

Dosyalar diskte durur, ayrı bir web sunucusu onları doğrudan servis eder.

- **Artıları:** çok hızlı; `Range` ve önbellek başlıkları hazır.
- **Neden seçilmedi:** dosya adresini bilen herkes dosyayı alır, yani FR-06/NFR-03 doğrudan ihlal edilir. Yetki için backend'e soran ek yapılandırma (`auth_request`, `X-Accel-Redirect`) gerekir; bu da ikinci bir sunucu ve ikinci bir yapılandırma demek. Performans ihtiyacı doğarsa bu yol, bu karar bozulmadan sonradan eklenebilir.

### Harici video platformu (YouTube / Vimeo embed)

Eğitmen videoyu harici platforma yükler, derse yalnız bağlantısını ekler. 18.10.2026'daki ilk plan buydu.

- **Artıları:** depolama, akış ve `Range` sorunu yok; bant genişliği platformda.
- **Neden seçilmedi:** proje dokümanı dosya yüklemeyi istiyor (FR-11). PDF ve dokümanları kapsamıyor. Liste dışı (unlisted) bir video bağlantısı paylaşılabildiği için kayıt kontrolü (FR-06) uygulanamıyor. Demo dış servise ve internet bağlantısına bağımlı kalıyor.

## Sonuçlar

**Olumlu:**

- Erişim kuralı tek yerde, backend'de uygulanıyor; diskteki dosyaya backend dışından yol yok. Bölüm 6'nın "kayıtsız erişim" testi doğrudan bu uçta yazılabiliyor.
- `docker compose up` dışında kurulum yok; ek hesap, anahtar ya da servis yok.
- Tür ve boyut kuralları içerik türünün sınıfında (Factory Method); yeni bir tür yükleme servisini değiştirmeden eklenebiliyor (NFR-18).
- Hatalar diğer uçlarla aynı `ErrorResponse` gövdesiyle dönüyor (413, 415).

**Olumsuz / ödünleşimler:**

- **Ölçek:** dosyalar tek bir makinenin diskinde; backend birden fazla kopya çalıştırılamaz (paylaşılan disk olmadan). Bu proje için kabul edilebilir; gerekirse `StorageService` arayüzünün arkasına nesne depolama uygulaması yazılır.
- **Yedekleme:** volume'un yedeği kendiliğinden alınmıyor; veritabanı yedeğiyle birlikte elle alınmalı. `docker compose down -v` yüklenen dosyaları da siler.
- **Performans:** büyük videolar backend'in iş parçacıkları ve ağ bağlantısı üzerinden akıyor. Akış (stream) kullanıldığı için bellek sorunu yok, ama yoğun kullanımda backend'i yavaşlatabilir.
- **Sorumluluk bizde:** tür denetimi, boyut sınırı ve `Content-Disposition` gibi işleri hazır bir servis yerine biz yazıyor ve test ediyoruz. `Range` desteği Spring MVC'nin `Resource` yanıtlarıyla geliyor, ama doğru çalıştığını (206, ileri sarma) yine bizim testimiz göstermeli.

**Kararın kodda nasıl korunduğu:**

- Yükleme testleri: uzantısı `.mp4` yapılmış çalıştırılabilir dosya 415, sınırı aşan dosya 413 (NFR-11, NFR-12; #111, #112). Bu testler Bölüm 6 testlerindendir, silinmez ve `@Disabled` yapılmaz (`CLAUDE.md` › Test beklentisi).
- İçerik servisi testleri: kayıtsız kursiyere 403, girişsize 401, kayıtlıya 200; `Range` isteğine 206 (#113).
- `CLAUDE.md` › Yapma: "Dosya depolama için MinIO/S3 ya da dış servis ekleme (ADR-004)". Bu kararı değiştirmek yeni bir ADR gerektirir.
