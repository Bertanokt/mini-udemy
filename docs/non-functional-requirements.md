# Fonksiyonel olmayan gereksinimler

Bu doküman Mini Udemy'nin fonksiyonel olmayan gereksinimlerini (NFR) tanımlar (proje raporu şablonu, bölüm 3.3). Gereksinimler proje tanım dokümanının 5. bölümündeki teknik beklentilerden türetilmiştir. Fonksiyonel gereksinimler: [requirements.md](requirements.md).

- **ID:** `NFR-NN`. Bir gereksinim silinirse numarası tekrar kullanılmaz.
- **Ölçüt:** gereksinimin karşılandığını gösteren ölçülebilir koşul.
- **Doğrulama:** **Test** (otomatik test, CI'da çalışır), **İnceleme** (kod/doküman incelemesi, PR review), **Demo** (çalışan sistem üzerinde elle gösterim).
- **Kaynak:** proje tanım dokümanı §5 ve/veya ilgili fonksiyonel gereksinim.

## 3.3.1 Performans

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-01 | Liste döndüren uç noktalar sayfalıdır; tüm kayıtlar tek yanıtta dönmez. | Katalog, yorum ve kullanıcı listeleri `page`/`size` parametresi alır; `size` en fazla 50'dir, daha büyük değer 50'ye indirilir ya da 400 ile reddedilir. | Test | §5, FR-13, FR-16, FR-39 |
| NFR-02 | Video ve dokümanlar bellek tüketmeden sunulur. | Dosyalar akış (stream) olarak gönderilir; video için HTTP `Range` isteği desteklenir (206 Partial Content). | Test | §5, FR-12 |

## 3.3.2 Güvenlik

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-03 | Ders içeriklerine erişim yetkiye bağlıdır. | Kursa kayıtlı olmayan kursiyerin içerik isteği 403 ile reddedilir; kursun eğitmeni ve yönetici erişebilir. Dosyalara doğrudan (yetkisiz) URL ile ulaşılamaz. | Test | §5, FR-06, Bölüm 6 |
| NFR-04 | Kimlik doğrulama durumsuz ve güvenlidir. | Korunan uç noktalar geçerli JWT olmadan 401 döner. Parolalar tek yönlü, tuzlu bir algoritmayla (BCrypt) saklanır; düz metin parola veritabanında ve loglarda bulunmaz. | Test, İnceleme | §5, FR-02 |
| NFR-05 | Rol ve sahiplik kuralları sunucuda denetlenir. | Başka eğitmenin kursunu düzenleme isteği 403 ile reddedilir; frontend'deki gizleme tek başına yeterli sayılmaz. | Test | §5, FR-10, Bölüm 6 |
| NFR-06 | Gizli bilgiler koda yazılmaz. | JWT anahtarı ve veritabanı parolası ortam değişkeninden okunur; repoda gizli bilgi bulunmaz. | İnceleme | CONTRIBUTING.md |

## 3.3.3 Kullanılabilirlik ve mobil uyumluluk

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-07 | Web arayüzü mobil cihazlarda kullanılabilir. | 360 px genişlikte hiçbir sayfada yatay kaydırma oluşmaz; tüm işlemler mobil görünümde yapılabilir. | Demo | §5, kapsam (bölüm 2) |
| NFR-08 | Hata mesajları anlaşılırdır. | Doğrulama hatalarında (400) hangi alanın neden hatalı olduğu `errors` içinde döner ve formda ilgili alanın yanında gösterilir. | Test, Demo | §5 |

## 3.3.4 Güvenilirlik ve veri bütünlüğü

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-09 | Mükerrer kayıt ve mükerrer sertifika oluşmaz. | Aynı kursiyer–kurs çifti için ikinci kayıt 409 döner; veritabanında benzersizlik kısıtı vardır. Bir kursiyere bir kurs için tek sertifika oluşur. | Test | FR-18, FR-27, Bölüm 6 |
| NFR-10 | İlerleme yüzdesi doğru hesaplanır. | Bölüm ve kurs ilerlemesi tamamlanan zorunlu ders sayısının toplam zorunlu ders sayısına oranıdır; sınır durumları (0 ders, tümü tamamlandı) testlidir. | Test | FR-21, Bölüm 6 |

## 3.3.5 Dosya sınırları

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-11 | Yüklenen dosyaların türü ve boyutu sınırlıdır. | Video (mp4, webm) en fazla 200 MB; PDF ve doküman (pdf, docx, pptx) en fazla 20 MB; kapak görseli (jpg, png, webp) en fazla 2 MB. Sınırı aşan dosya 413, izin verilmeyen tür 415 ile reddedilir. | Test | §5, FR-11 |
| NFR-12 | Dosya türü uzantıya göre değil içeriğe göre denetlenir. | Uzantısı `.mp4` olarak değiştirilmiş bir çalıştırılabilir dosya 415 ile reddedilir. | Test | §5, FR-11 |

## 3.3.6 Bakım yapılabilirlik

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-13 | Kod modüler monolit mimarisine uyar. | Bir modül başka modülün yalnız `service` arayüzünü ya da domain olaylarını kullanır; başka modülün repository/entity'sine erişmez. | İnceleme | [architecture.md](architecture.md), [ADR-0001](adr/0001-moduler-monolit.md) |
| NFR-14 | Her değişiklik otomatik olarak doğrulanır. | `main`'e açılan her PR'da CI backend build + testleri ve frontend lint + build'i çalıştırır; kırmızı CI ile merge yapılmaz. | İnceleme | CONTRIBUTING.md |
| NFR-15 | API sözleşmesi kodla uyumludur. | `docs/api/openapi.yaml` lint'ten hatasız geçer; uç nokta değişikliği aynı PR'da sözleşmeye işlenir. | İnceleme | CLAUDE.md |

## 3.3.7 Taşınabilirlik ve kurulum

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-16 | Sistem tek komutla ayağa kalkar. | Temiz bir makinede `docker compose up --build` ile veritabanı, backend ve frontend çalışır; veritabanı şeması Flyway ile otomatik oluşur. | Demo | §5 |
| NFR-17 | Arayüz güncel tarayıcılarda çalışır. | Chrome, Firefox ve Safari'nin güncel sürümlerinde temel senaryolar (1–5) çalışır. | Demo | §5 |

## 3.3.8 Ölçeklenebilirlik ve genişletilebilirlik

| ID | Gereksinim | Ölçüt | Doğrulama | Kaynak |
|---|---|---|---|---|
| NFR-18 | Yeni içerik türü mevcut kodu değiştirmeden eklenebilir. | Yeni tür, yalnız yeni bir fabrika/ürün sınıfı eklenerek desteklenir; yükleme servisi değişmez (Factory Method). | İnceleme | CLAUDE.md (tasarım desenleri) |
| NFR-19 | Modüller arası tepkiler gevşek bağlıdır. | Bildirim gibi yan etkiler domain olaylarıyla tetiklenir; olayı yayınlayan modül dinleyeni tanımaz (Observer). | İnceleme | [architecture.md](architecture.md) |
