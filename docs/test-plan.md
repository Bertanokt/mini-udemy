# Test planı

Bu doküman Mini Udemy'nin hangi seviyede, hangi araçla ve kimin tarafından test edileceğini tanımlar (proje raporu şablonu, bölüm 7.5). Kaynaklar: fonksiyonel gereksinimler [requirements.md](requirements.md) (FR-01…FR-44), fonksiyonel olmayan gereksinimler [non-functional-requirements.md](non-functional-requirements.md) (NFR-01…NFR-19), proje tanım dokümanı bölüm 6 ve [CLAUDE.md](../CLAUDE.md)'deki test beklentileri.

Test sonuçları ve gerçekleşen coverage bu dokümanda değil, test raporunda (`test-report.md`, #99) yer alır. FR → issue → test izlenebilirliği `traceability.md`'de (#98) tutulur.

## 1. Test seviyeleri ve araçlar

| Seviye | Kısaltma | Araç | Neyi test eder | Adlandırma | Nerede çalışır |
|---|---|---|---|---|---|
| Unit | **U** | JUnit 5, Mockito, AssertJ (`spring-boot-starter-test`) | Service katmanındaki iş kuralları, desen sınıfları (State, Strategy, Factory Method), mapper'lar, hata dönüşümü. Bağımlılıklar Mockito ile taklit edilir; veritabanı yoktur. | `*Test`, metot adı davranışı anlatır: `enrollTwice_returnsConflict` | `./mvnw verify`, CI |
| Entegrasyon | **E** | Spring Boot Test + MockMvc, Testcontainers (gerçek PostgreSQL 17) | Uçtan uca HTTP isteği → controller → service → veritabanı. Yetki (401/403), benzersizlik kısıtları, Flyway şeması, dosya akışı, senaryolar 1–5. | `*IT` | `./mvnw verify`, CI (Docker gerekir) |
| Mimari | **M** | ArchUnit | Katman yönü (controller → service → repository), modüller arası erişim kuralı, `common`'un modüllere bağımsızlığı ([architecture.md](architecture.md) bölüm 3–4). | `*ArchTest` | `./mvnw verify`, CI |
| Statik inceleme | **İ** | PR kontrolü, OpenAPI lint (`redocly lint`) | Kodla ölçülemeyen kurallar: gizli bilgi yok, doküman güncel, desen doğru kullanılmış. | – | PR'da |
| Manuel | **D** | Tarayıcı (Chrome, Firefox, Safari), mobil görünüm (360 px) | Arayüz akışları, oynatıcı, sertifika metni, mobil uyumluluk, kurulum. Bölüm 5'teki kontrol listesiyle yapılır. | – | Bug bash (#68), demo provası (#106) |

Notlar:

- Frontend için otomatik UI testi yazılmaz (Playwright vb. eklenmez, `CLAUDE.md`); frontend `npm run lint && npm run build` ile CI'da denetlenir, akışlar manuel test edilir.
- Testcontainers ve ArchUnit şu an `pom.xml`'de yok; ilk entegrasyon testini (#65) ve ilk ArchUnit kuralını (#86) ekleyen PR bağımlılığı da ekler ve açıklamasında belirtir.
- Coverage JaCoCo ile ölçülür: `./mvnw verify` → `backend/target/site/jacoco/index.html`, CI'da `jacoco-report` artifact'ı (#89).

## 2. Sorumluluklar

| Kim | Sorumluluk |
|---|---|
| Özelliği geliştiren (her üye) | Kendi PR'ındaki service public metotları için unit test: başarılı yol + en az bir hata yolu (yetki, bulunamadı, çakışma). Issue'nun her kabul kriteri bir teste ya da yazılı bir manuel kontrole karşılık gelir; PR'da kriter → kanıt tablosu verilir. |
| Kalite ve Süreç Sorumlusu (Veronika) | Bu plan; servis test issue'ları (#15, #31, #32, #44, #56, #61, #114, #128, #132, #137); Bölüm 6 kritik testleri ve API erişim matrisi (#139); entegrasyon testleri (#65, #138); bug bash (#68); test raporu (#99); her PR'da hızlı kontrol (CI yeşil, `Closes #…`, kriter → kanıt tablosu, "Ne / Neden" PR sahibinin kendi cümleleri, kod değiştiyse OpenAPI/diyagram güncel). |
| Yazılım Mimarı (Bertan) | ArchUnit kuralları (#86) ve mimari kuralların inceleme ile denetimi. |
| Veri ve Entegrasyon Sorumlusu (Eray) | OpenAPI'nin kodla eşitlenmesi (#66), Flyway migration'larının entegrasyon testlerinde temiz kurulması. |
| Alan Tasarımcısı (Sedat) | Diyagram–kod tutarlılık kontrolüne (#97) destek; senaryoların demo provasında doğrulanması. |

## 3. Test ortamı ve çalıştırma

```bash
cd backend && ./mvnw verify                    # unit + entegrasyon + mimari testler, JaCoCo raporu
cd frontend && npm run lint && npm run build   # frontend denetimi
docker compose up --build                      # manuel test ve demo için tüm sistem
```

- `test` profili kullanılır; veritabanı gerektiren testler Testcontainers ile her çalıştırmada temiz bir PostgreSQL'e Flyway şemasını kurar. Testler birbirinin verisine güvenmez.
- CI (`.github/workflows/ci.yml`) her PR'da ve `main`'e her push'ta aynı komutları çalıştırır. Kırmızı CI ile merge yapılmaz (NFR-14).
- Bir hata düzeltilirken önce hatayı gösteren kırmızı test yazılır, sonra düzeltilir. Yeni testin doğru sebeple kırmızıya döndüğü bir kez görülür.

## 4. Gereksinim → test eşlemesi

Seviye kısaltmaları bölüm 1'deki tablodadır. "Test issue'su" sütunundaki issue o testi yazma işidir; boşsa test, özelliği geliştiren PR'da yazılır.

### 4.1 Fonksiyonel gereksinimler

| FR | Ne doğrulanır | Seviye | Test issue'su |
|---|---|---|---|
| FR-01 | Geçerli kayıt → 201; kayıtlı e-posta → 409; `ADMIN` rolüyle kayıt → 400; 8 karakterden kısa parola → 400 | U, E | #15 |
| FR-02 | Doğru bilgiler → JWT; yanlış e-posta ve yanlış parola → aynı 401 | U, E | #15 |
| FR-03 | Token yalnız bir kez ve süresi içinde geçerli; kayıtsız e-postaya aynı 202 | U | #15 |
| FR-04 | Ad soyad ve biyografi güncellenir; e-posta/rol alanı gönderilirse 400 | U | #15 |
| FR-05 | Korumalı uç token olmadan 401, yanlış rolle 403 (tüm uçlar için erişim matrisi) | E | #139 |
| FR-06 | Kayıtsız kursiyerin içerik isteği 403; eğitmen ve yönetici erişir | U, E | #139, #114 |
| FR-07 | Yeni kurs "Taslak" ve oluşturana ait; başlık/kategori eksikse 400 | U | #31 |
| FR-08 | Bölüm/ders sırası kaydedildiği gibi döner; içerik türü, süre ve zorunluluk saklanır | U | #31 |
| FR-09 | İzin verilen 5 geçiş başarılı, diğerleri 409; katalogda yalnız "Yayımlandı" | U | #31, #61 |
| FR-10 | Başka eğitmen/kursiyerin düzenleme ve silmesi 403; durum dışı düzenleme/silme reddedilir | U, E | #31, #139 |
| FR-11 | İzinli tür ve boyut kabul; aşan boyut 413, izinsiz tür 415 | U | #114 |
| FR-12 | Video oynatılır ve sarılır, PDF görüntülenir, doküman iner; önceki/sonraki ders | E, D | #114, #68 |
| FR-13 | Filtreler (anahtar kelime, kategori, seviye, eğitmen) birlikte uygulanır; yalnız yayımlanmış kurslar; sayfalı | U, E | #65 |
| FR-14 | "En az N yıldız" filtresi yalnız ortalaması ≥ N olan kursları döndürür | U | – |
| FR-15 | Toplam süre = ders sürelerinin toplamı; içerik yalnız kayıtlıya açık | U, E | #65 |
| FR-16 | Ortalama gizlenmemiş değerlendirmelerden; yorumlar sayfalı | U | #128 |
| FR-17 | Yayımlanmış kursa kayıt başarılı; diğer durumlarda reddedilir | U, E | #32, #65 |
| FR-18 | İkinci kayıt 409 ve veritabanında tek satır (eşzamanlı istek dahil) | U, E | #32, #139 |
| FR-19 | Panelde her kurs için ilerleme yüzdesi ve son izlenen ders | E | #65 |
| FR-20 | Dersi tamamlamak işaretler; tekrar işaretlemek yeni kayıt oluşturmaz | U | #44 |
| FR-21 | 20 dersin 15'i → %75; dersi olmayan bölüm → %0; ders eklenince/silinince yeniden hesap | U, E | #44, #139 |
| FR-22 | "Devam et" son izlenen dersi, hiç izlenmemişse ilk dersi açar | U, E | #44, #65 |
| FR-23 | 2'den az şık ya da doğru şık yoksa soru kaydedilmez; sınavda kullanılan soru silinemez | U | – |
| FR-24 | Eşik girilmezse %70; başka kursun sorusu seçilemez | U | – |
| FR-25 | Her puanlama stratejisi doğru puanlar; eşiğe eşit yüzde "geçti"; doğru cevaplar sınav sırasında gönderilmez | U | #56 |
| FR-26 | Denemeler en yeniden eskiye listelenir | U | #56 |
| FR-27 | Tüm zorunlu dersler + zorunlu sınavlar → tek sertifika; ikinci sertifika oluşmaz; kod benzersiz | U, E | #132 |
| FR-28 | Geçerli kod → ad, kurs, tarih; geçersiz kod → bulunamadı; giriş gerekmez | U, E | #132 |
| FR-29 | Sertifika ve doğrulama sayfasında akreditasyon dışı ibaresi | D | #68 |
| FR-30 | Puan 1–5 dışı → 400; kayıtsız kullanıcı → 403 | U | #128 |
| FR-31 | İkinci değerlendirme 409; mevcut değerlendirme düzenlenebilir | U | #128 |
| FR-32 | Gizlenen yorum listede görünmez ve ortalamaya katılmaz; gerekçe saklanır | U | #128 |
| FR-33 | Eğitmen yalnız kendi kurslarını görür; reddedilen kursta gerekçe | U, E | #138 |
| FR-34 | Tamamlanma ve sınav başarı oranları veritabanındaki kayıtlarla aynı | U, E | #138 |
| FR-35 | Kursiyer ilerleme listesi; başka eğitmenin kursu 403 | U, E | #138, #139 |
| FR-36 | Değerlendirme istatistikleri FR-16 hesabıyla aynı | U | – |
| FR-37 | Onay → "Yayımlandı"; red → "Taslak" + zorunlu gerekçe; incelemede olmayan kurs 409 | U | #61 |
| FR-38 | Yayından kaldırılan kurs katalogda yok ve yeni kayıt almaz; eski kayıtlılar erişir | U, E | #61 |
| FR-39 | Pasif kullanıcı giriş yapamaz; yönetici kendini pasifleştiremez | U | – |
| FR-40 | Kategori adı benzersiz; kursu olan kategori silinemez (409) | U | – |
| FR-41 | Şikâyet durumu yalnız yönetici tarafından değişir | U | – |
| FR-42 | Rapor sayıları veritabanıyla aynı; tarih aralığı filtresi | U | #145 |
| FR-43 | 6 olayın her biri yalnız ilgili alıcılara tek bildirim üretir | U | #137 |
| FR-44 | Bildirimler yeniden eskiye; okundu sayısı düşer; başkasının bildirimi görülemez | U, E | #139 |

### 4.2 Fonksiyonel olmayan gereksinimler

| NFR | Ne doğrulanır | Seviye | Test issue'su |
|---|---|---|---|
| NFR-01 | Liste uçları `page`/`size` alır; `size` > 50 sınırlanır ya da 400 | E | #65 |
| NFR-02 | Video `Range` isteğine 206 Partial Content döner; dosya akışla gönderilir | E | #114 |
| NFR-03 | Kayıtsız kursiyerin içerik ve dosya isteği 403; dosyaya doğrudan URL ile ulaşılamaz | E | #139, #114 |
| NFR-04 | Korumalı uçlar JWT olmadan 401; parola BCrypt ile saklanır, logda düz parola yok | E, İ | #139, #15 |
| NFR-05 | Başka eğitmenin kursunu düzenleme 403 (sunucuda) | E | #139 |
| NFR-06 | JWT anahtarı ve DB parolası ortam değişkeninden; repoda gizli bilgi yok | İ | PR hızlı kontrolü |
| NFR-07 | 360 px'te yatay kaydırma yok; tüm işlemler mobil görünümde yapılabilir | D | #68 |
| NFR-08 | 400 yanıtında alan bazlı `errors`; formda ilgili alanın altında gösterilir | U, D | `GlobalExceptionHandlerTest`, #68 |
| NFR-09 | Mükerrer kayıt 409 + DB benzersizlik kısıtı; kurs başına tek sertifika | U, E | #32, #132, #139 |
| NFR-10 | İlerleme formülü ve sınır durumları (0 ders, tümü tamamlandı) | U | #44 |
| NFR-11 | 200 / 20 / 2 MB sınırları; aşan 413, izinsiz tür 415 | U | #114 |
| NFR-12 | Uzantısı `.mp4` yapılmış çalıştırılabilir dosya 415 | U | #114 |
| NFR-13 | Modüller yalnız başka modülün `service` arayüzünü ya da olaylarını kullanır | M | #86 |
| NFR-14 | Her PR'da CI çalışır; kırmızı CI ile merge yok | İ | PR hızlı kontrolü |
| NFR-15 | `openapi.yaml` lint'ten hatasız; kod ile sözleşme eşit | İ | #66 |
| NFR-16 | Temiz makinede `docker compose up --build` ile sistem ayağa kalkar | D | #68, #106 |
| NFR-17 | Chrome, Firefox ve Safari'de senaryo 1–5 çalışır | D | #68 |
| NFR-18 | Yeni içerik türü yalnız yeni fabrika/ürün sınıfıyla eklenir | İ, U | #97, #114 |
| NFR-19 | Yan etkiler domain olaylarıyla; yayınlayan modül dinleyeni tanımaz | İ, U, M | #137, #86 |

## 5. Bölüm 6 kritik test durumları

Proje tanım dokümanı bölüm 6'daki dört durum ve dosya denetimleri her zaman testli kalır; bu testler silinmez ve `@Disabled` yapılmaz (`CLAUDE.md`).

| Durum | Gereksinim | Beklenen | Test |
|---|---|---|---|
| Kursa kayıt olmadan özel derse erişim | FR-06, NFR-03 | 403 | U + E, #139 |
| Mükerrer kayıt | FR-18, NFR-09 | 409, veritabanında tek kayıt | U + E, #139 |
| Yetkisiz kurs düzenleme | FR-10, NFR-05 | 403 | U + E, #139 |
| Yanlış ilerleme hesaplaması | FR-21, NFR-10 | 20 dersin 15'i → %75; 0 ders → %0 | U + E, #139 |
| Dosya türü ve boyutu | FR-11, NFR-11, NFR-12 | 413 / 415 | U, #114 |

## 6. Manuel test kontrol listesi (bug bash, #68)

Her madde `docker compose up --build` ile ayağa kalkan sistemde, seed verisiyle (#67) denenir. Bulunan her hata ekran görüntüsü ve adımlarla issue olarak açılır (`bug` etiketi, öncelik P1/P2).

**Kurulum**
- [ ] Temiz bir makinede `docker compose up --build` hatasız tamamlanır; frontend, backend health ve veritabanı çalışır (NFR-16).

**Senaryo 1: Kurs yayımlama (eğitmen + yönetici)**
- [ ] Eğitmen olarak kayıt olup giriş yapılır.
- [ ] Kurs oluşturulur; bölüm ve ders eklenir, sırası değiştirilir.
- [ ] Video, PDF ve doküman yüklenir; 200 MB'tan büyük video ve izinsiz tür anlaşılır bir hata mesajıyla reddedilir.
- [ ] Soru bankasına soru eklenir, bölüme sınav eklenir.
- [ ] Kurs onaya gönderilir; yönetici reddeder (gerekçe eğitmen panelinde görünür), sonra onaylar; kurs katalogda görünür.

**Senaryo 2: Kursa kayıt ve öğrenme (kursiyer)**
- [ ] Katalogda arama ve filtreler çalışır; ayrıntı sayfasında toplam süre doğru.
- [ ] Kayıt olmadan ders içeriği açılmaz.
- [ ] Kursa kayıt olunur; ikinci kez kayıt denemesi anlaşılır bir mesajla reddedilir.
- [ ] Video oynatılır ve ileri/geri sarılır; PDF tarayıcıda açılır; doküman iner.
- [ ] Ders tamamlandı olarak işaretlenir; ilerleme yüzdesi doğru artar.

**Senaryo 3: Kaldığı yerden devam**
- [ ] Çıkış yapıp tekrar girilir; panelde ilerleme ve son ders görünür; "Devam et" doğru dersi açar.

**Senaryo 4: Sınav ve sertifika**
- [ ] Sınav çözülür; sonuçta puan, yüzde ve geçti/kaldı görünür; eşiğin altında kalınınca tekrar denenebilir.
- [ ] Tüm zorunlu dersler ve sınavlar bitince sertifika oluşur; akreditasyon dışı ibaresi görünür (FR-29).
- [ ] Doğrulama sayfasında geçerli kod bilgileri, geçersiz kod "bulunamadı" gösterir.

**Senaryo 5: Eğitmen analitiği**
- [ ] Eğitmen panelinde kayıt sayısı, tamamlanma oranı ve sınav başarı oranı seed verisiyle tutarlı.

**Yetki ve hatalar**
- [ ] Kursiyer eğitmen/yönetici sayfalarını açamaz; başka eğitmenin kursunu düzenleyemez.
- [ ] Form hataları ilgili alanın altında Türkçe ve anlaşılır gösterilir (NFR-08).
- [ ] Bildirimler doğru olaylarda gelir ve okundu işaretlenince sayı düşer.

**Uyumluluk**
- [ ] Tarayıcı geliştirici araçlarında 360 px genişlikte hiçbir sayfada yatay kaydırma yok (NFR-07).
- [ ] Senaryo 1–5 Chrome, Firefox ve Safari'nin güncel sürümlerinde denenir (NFR-17).

## 7. Başarı ölçütleri

Proje teslim edilebilir sayılmak için:

1. `main`'de CI yeşil: backend build + tüm testler, frontend lint + build.
2. Tüm P1 FR'lerin en az bir otomatik testi ya da yazılı manuel kontrolü var ve izlenebilirlik matrisinde (#98) gösterilmiş.
3. Bölüm 5'teki Bölüm 6 testleri ve dosya denetimi testleri mevcut ve yeşil; hiçbiri `@Disabled` değil.
4. Servis katmanında (`com.miniudemy.*.service`) satır coverage'ı en az **%70** (JaCoCo raporu, #89). Genel coverage bilgi amaçlı raporlanır.
5. ArchUnit kurallarında ihlal yok; kurallar gevşetilmemiş.
6. Bug bash'te açılan P1 hataların hepsi demo öncesi kapatılmış.
7. Demo provasında (#106) senaryo 1–5 hatasız tamamlanıyor.

## 8. Takvim

| Ne | Ne zaman |
|---|---|
| Servis unit testleri | Her özellikle aynı PR'da, Hafta 2–4 |
| Bölüm 6 testleri + erişim matrisi (#139), entegrasyon testleri (#65, #138) | Hafta 4 (feature freeze, 17.11.2026) |
| Bug bash (#68), test raporu (#99) | Hafta 5 (24.11.2026) |
| Demo provası (#106) | Sunum öncesi (teslim 02.12.2026) |
