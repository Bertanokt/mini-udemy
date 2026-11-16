# İzlenebilirlik matrisi

Proje tanım dokümanının maddelerinin (modüller A–L, senaryo 1–5, §5–7) ve her gereksinimin hangi issue'larla geliştirildiğini ve hangi testle doğrulandığını gösterir (#98). Proje raporuna tablo olarak eklenecek.
**Veri tarihi:** 16.11.2026. Issue durumları GitHub'dan, test eşlemesi [test-plan.md](test-plan.md) bölüm 4'ten, mevcut testler `backend/src/test` altından alındı. Hafta 5'te testler yazıldıkça "Bugün `main`'deki test" sütunu güncellenecek.

**Nasıl okunur**

- `#12 ✓`: issue kapandı (iş `main`'de). `#12`: issue açık.
- Test seviyeleri ([test-plan.md](test-plan.md) bölüm 1): **U** unit, **E** entegrasyon, **M** mimari (ArchUnit), **İ** inceleme, **D** demo / manuel.
- "Test (plan)" sütunundaki issue o testi yazma işidir. "–" ise test, özelliği geliştiren PR'da yazılır (test planı bölüm 4).

## 1. Doküman maddesi → gereksinim → test

### 1.1 Modüller (A–L)

| Modül | Gereksinimler | Test issue'ları |
|---|---|---|
| A. Kullanıcı ve yetkilendirme | FR-01 … FR-06 | #15, #139, #114 |
| B. Kurs oluşturma ve yönetimi | FR-07, FR-08, FR-09, FR-10, FR-38 | #31, #61, #139 |
| C. Video ve eğitim materyalleri | FR-06, FR-08, FR-11, FR-12 | #114, #139, #31, #68 |
| D. Kurs kataloğu ve arama | FR-13, FR-14, FR-15, FR-16 | #65, #128 |
| E. Kursa kayıt | FR-17, FR-18, FR-19 | #32, #65, #139 |
| F. İzleme ve ilerleme takibi | FR-20, FR-21, FR-22 | #44, #65, #139 |
| G. Sınav ve değerlendirme | FR-23, FR-24, FR-25, FR-26 | #56 |
| H. Dijital sertifika | FR-27, FR-28, FR-29 | #132, #68 |
| I. Kurs puanlama ve yorumlar | FR-16, FR-30, FR-31, FR-32, FR-36 | #128 |
| J. Eğitmen paneli | FR-33, FR-34, FR-35, FR-36 | #138, #139 |
| K. Yönetici paneli | FR-09, FR-32, FR-37 … FR-42 | #31, #61, #128, #145 |
| L. Bildirimler | FR-43, FR-44 | #137, #139 |

Her modülün en az bir test issue'su var. Modüllerin içinde test issue'su olmayan gereksinimler bölüm 3'te.

### 1.2 Temel senaryolar (bölüm 4)

| Senaryo | Gereksinimler ([requirements.md](requirements.md)) | Uçtan uca test | Demo |
|---|---|---|---|
| 1. Kurs yayımlama | FR-07, FR-08, FR-09, FR-11, FR-23, FR-24, FR-37 | #65 | #70, #106 |
| 2. Kursa kayıt ve öğrenme | FR-13, FR-15, FR-17, FR-12, FR-06, FR-20, FR-21 | #65 | #70, #106 |
| 3. Kaldığı yerden devam | FR-19, FR-22 | #65 | #70, #106 |
| 4. Sınav ve sertifika | FR-25, FR-27, FR-28, FR-29 | #138 | #70, #106 |
| 5. Eğitmen analitiği | FR-33, FR-34, FR-35 | #138 | #70, #106 |

### 1.3 §5 teknik beklentiler

§5 kaynaklı 11 fonksiyonel olmayan gereksinim: NFR-01, 02, 03, 04, 05, 07, 08, 11, 12, 16, 17 ([non-functional-requirements.md](non-functional-requirements.md)). Her birinin geliştirme issue'su ve testi bölüm 2.2'de.

### 1.4 §6 test durumları

| Test durumu | Gereksinim | Beklenen | Test |
|---|---|---|---|
| Kursa kayıt olmadan özel derslere erişim | FR-06, NFR-03 | 403 | #139, #114 |
| Mükerrer kayıt | FR-18, NFR-09 | 409, veritabanında tek kayıt | #139, #32 |
| Yetkisiz kurs düzenleme | FR-10, NFR-05 | 403 | #139, #31 |
| Yanlış ilerleme hesaplaması | FR-21, NFR-10 | 20 dersin 15'i → %75; 0 ders → %0 | #139, #44 |

Bu dört durumun testi henüz yok; hepsi #139'da ve ilgili servis testlerinde yazılacak. Test planı gereği bu testler silinmez ve `@Disabled` yapılmaz.

### 1.5 §7

| Madde | Geliştirme | Doğrulama |
|---|---|---|
| Örnek kurs verileri (#67'nin açıklamasına göre) | #67 seed data | #68 bug bash, #70 demo senaryosu |

§7'nin tam metni bu repoda yok; satır #67'deki atfa göre yazıldı. Rapora geçmeden önce proje tanım dokümanıyla karşılaştırılacak.

## 2. Gereksinim → issue → test

### 2.1 Fonksiyonel gereksinimler

| FR | Gereksinim | Doküman | Öncelik | Geliştirme issue'ları | Test (plan) | Bugün `main`'deki test |
|---|---|---|---|---|---|---|
| FR-01 | Kullanıcı ad soyad, e-posta ve parola ile kursiyer ya da eğitmen olarak kayıt olabilir | A | P1 | #12, #17 | U, E: #15 | – |
| FR-02 | Kullanıcı e-posta ve parolasıyla giriş yapabilir | A | P1 | #13 ✓, #18 | U, E: #15 | `AuthServiceTest`, `AuthControllerTest` (giriş: 200, aynı 401, pasif 403) |
| FR-03 | Kullanıcı parolasını unuttuğunda parola yenileme isteyebilir ve tek kullanımlık bağlantıyla yeni parola belirleyebilir | A | P2 | #115, #117 | U: #15 | – |
| FR-04 | Kullanıcı kendi profilini görüntüleyip ad soyad ve kısa biyografisini güncelleyebilir | A | P2 | #116, #117 | U: #15 | – |
| FR-05 | Her işlem için kullanıcının rolü sunucu tarafında denetlenir | A | P1 | #10 ✓, #14, #19 | E: #139 | `SecurityConfigTest` (token yok 401, rol yetersiz 403); uç bazında matris #139'da |
| FR-06 | Ders içeriklerine erişim, kursa kayıt ve kullanıcının yetkisine bağlıdır | A, C | P1 | #113, #45 | U, E: #139, #114 | – |
| FR-07 | Eğitmen başlık, açıklama, kategori, seviye, kapak görseli ve öğrenme hedefleriyle kurs oluşturabilir | B | P1 | #22, #33 | U: #31 | `CourseTest.newCourse_startsAsDraft` |
| FR-08 | Eğitmen kursuna bölümler, bölümlere dersler ekleyebilir; bölüm ve derslerin sırasını değiştirebilir | B, C | P1 | #24, #25, #34 | U: #31 | – |
| FR-09 | Kurs Taslak, İncelemede, Yayımlandı ve Yayından kaldırıldı durumlarından birinde bulunur; durumlar arası geçişler kurallara bağlıdır | B, K | P1 | #85, #27, #59, #123 | U: #31, #61 | `CourseEnumsTest` (durum değerleri migration ile aynı); geçiş kuralları henüz yok |
| FR-10 | Bir kursu yalnız sahibi olan eğitmen düzenleyebilir ve silebilir | B | P1 | #23 | U, E: #31, #139 | – |
| FR-11 | Eğitmen derslere video, PDF ve yardımcı doküman yükleyebilir | C | P1 | #110, #111, #112, #34 | U: #114 | – |
| FR-12 | Kayıtlı kursiyer kurs oynatıcısında dersleri sırasıyla izleyebilir/okuyabilir | C | P1 | #45, #113 | E, D: #114, #68 | – |
| FR-13 | Kullanıcı yayımlanmış kursları anahtar kelime, kategori, seviye ve eğitmene göre arayıp filtreleyebilir | D | P1 | #26, #35 ✓ | U, E: #65 | – |
| FR-14 | Kullanıcı kursları ortalama puana göre filtreleyip sıralayabilir | D | P2 | #143 | U: – | – |
| FR-15 | Kurs ayrıntı sayfasında açıklama, öğrenme hedefleri (kazanımlar), bölüm/ders listesi ve toplam süre gösterilir | D | P1 | #26, #36, #185 | U, E: #65 | `CourseTest.totalDuration_*` (toplam süre = ders süreleri toplamı) |
| FR-16 | Kurs ayrıntı sayfasında değerlendirmeler (ortalama puan, puan dağılımı, yorumlar) gösterilir | D, I | P2 | #126, #127, #143 | U: #128 | – |
| FR-17 | Kursiyer yayımlanmış bir kursa ücretsiz kayıt olabilir | E | P1 | #30, #36 | U, E: #32, #65 | – |
| FR-18 | Aynı kursiyerin aynı kursa ikinci kez kaydı engellenir | E | P1 | #29 ✓, #30 | U, E: #32, #139 | – |
| FR-19 | Kursiyer kayıtlı olduğu kursları bir panelde görür | E | P1 | #30, #37, #119 | E: #65 | – |
| FR-20 | Kursiyer izlediği dersi tamamlandı olarak işaretleyebilir | F | P1 | #41, #46 | U: #44 | – |
| FR-21 | Bölüm ve kurs ilerleme yüzdesi tamamlanan derslerden otomatik hesaplanır | F | P1 | #43, #46 | U, E: #44, #139 | – |
| FR-22 | Kursiyer kursa geri döndüğünde son izlediği dersten devam edebilir | F | P1 | #119, #120 | U, E: #44, #65 | – |
| FR-23 | Eğitmen kursu için çoktan seçmeli sorulardan oluşan bir soru bankası oluşturabilir | G | P1 | #121, #122 | U: – | – |
| FR-24 | Eğitmen bir bölüme, soru bankasından soru seçerek bölüm sonu sınavı ekleyebilir ve başarı eşiğini belirleyebilir | G | P1 | #48, #50 | U: – | – |
| FR-25 | Kayıtlı kursiyer sınava girer; cevapları otomatik puanlanır | G | P1 | #49, #52, #54, #57, #58 | U: #56 | – |
| FR-26 | Kursiyer bir sınavdaki önceki denemelerini ve sonuçlarını görebilir | G | P2 | #55, #141 | U: #56 | – |
| FR-27 | Koşulları sağlayan kursiyer için doğrulama kodlu sertifika otomatik oluşturulur | H | P1 | #129 | U, E: #132 | – |
| FR-28 | Herkes doğrulama koduyla bir sertifikanın geçerliliğini kontrol edebilir | H | P1 | #130, #131 | U, E: #132 | – |
| FR-29 | Sertifikada resmî akreditasyon iddiası bulunmaz | H | P1 | #130, #131 | D: #68 | – |
| FR-30 | Kursa kayıtlı kursiyer 1–5 yıldız puan ve yorum bırakabilir | I | P2 | #126, #127 | U: #128 | – |
| FR-31 | Bir kursiyer bir kurs için yalnız bir değerlendirme yapabilir | I | P2 | #126 | U: #128 | – |
| FR-32 | Yönetici uygunsuz yorumları gizleyebilir ve yeniden açabilir | I, K | P2 | #142, #146 | U: #128 | – |
| FR-33 | Eğitmen kendi kurslarını durumlarıyla ve kayıtlı kursiyer sayılarıyla listeler | J | P1 | #28, #38 | U, E: #138 | – |
| FR-34 | Eğitmen her kursu için tamamlanma oranını ve sınav başarı istatistiklerini görür | J | P1 | #133, #135 | U, E: #138 | – |
| FR-35 | Eğitmen kursundaki kursiyerlerin ilerlemesini izler | J | P1 | #134, #135 | U, E: #138, #139 | – |
| FR-36 | Eğitmen kurslarının değerlendirme istatistiklerini (ortalama puan, puan dağılımı) görür | J, I | P2 | #143, #135 | U: – | – |
| FR-37 | Yönetici incelemedeki kursları inceleyip onaylar ya da gerekçeyle reddeder | K | P1 | #59, #62 | U: #61 | – |
| FR-38 | Yönetici (gerekçeyle) ya da kursun eğitmeni yayımlanmış kursu yayından kaldırabilir | K, B | P1 | #123, #62 | U, E: #61 | – |
| FR-39 | Yönetici kullanıcıları ve eğitmenleri listeleyip hesaplarını pasifleştirebilir/aktifleştirebilir | K | P2 | #60, #63 | U: – | `AuthServiceTest`, `SecurityConfigTest` (pasif hesap → 403); yönetim ekranı yok |
| FR-40 | Yönetici kategorileri ekler, düzenler ve siler | K | P2 | #118, #109, #146 | U: – | – |
| FR-41 | Kullanıcı bir kursu ya da yorumu gerekçeyle şikâyet edebilir; yönetici şikâyetleri inceler | K | P2 | #144, #146 | U: – | – |
| FR-42 | Yönetici platform raporlarını görür | K | P2 | #145, #147 | U: #145 | – |
| FR-43 | Belirli olaylarda ilgili kullanıcıya uygulama içi bildirim oluşturulur | L | P1 | #124, #42 | U: #137 | – |
| FR-44 | Kullanıcı bildirimlerini listeler, okunmamış sayısını görür ve okundu olarak işaretler | L | P1 | #125, #136 | U, E: #139 | – |

### 2.2 Fonksiyonel olmayan gereksinimler

| NFR | Gereksinim | Kaynak | Geliştirme issue'ları | Test (plan) | Bugün `main`'deki test |
|---|---|---|---|---|---|
| NFR-01 | Liste döndüren uç noktalar sayfalıdır; tüm kayıtlar tek yanıtta dönmez. | §5, FR-13, FR-16, FR-39 | #26, #60, #126 | E: #65 | – |
| NFR-02 | Video ve dokümanlar bellek tüketmeden sunulur. | §5, FR-12 | #113 | E: #114 | – |
| NFR-03 | Ders içeriklerine erişim yetkiye bağlıdır. | §5, FR-06, Bölüm 6 | #113 | E: #139, #114 | – |
| NFR-04 | Kimlik doğrulama durumsuz ve güvenlidir. | §5, FR-02 | #10 ✓, #12 | E, İ: #139, #15 | `SecurityConfigTest`, `JwtServiceTest` (JWT yok/bozuk/süresi dolmuş → 401) |
| NFR-05 | Rol ve sahiplik kuralları sunucuda denetlenir. | §5, FR-10, Bölüm 6 | #23 | E: #139 | – |
| NFR-06 | Gizli bilgiler koda yazılmaz. | CONTRIBUTING.md | #10 ✓ | İ: PR hızlı kontrolü | `JwtPropertiesTest` (gizli anahtar yoksa ya da kısaysa uygulama açılmaz) |
| NFR-07 | Web arayüzü mobil cihazlarda kullanılabilir. | §5, kapsam (bölüm 2) | #71 | D: #68 | – |
| NFR-08 | Hata mesajları anlaşılırdır. | §5 | #11 ✓ | U, D: `GlobalExceptionHandlerTest`, #68 | `GlobalExceptionHandlerTest` (400 + alan bazlı `errors`, sıra korunur) |
| NFR-09 | Mükerrer kayıt ve mükerrer sertifika oluşmaz. | FR-18, FR-27, Bölüm 6 | #29 ✓, #30, #129 | U, E: #32, #132, #139 | – |
| NFR-10 | İlerleme yüzdesi doğru hesaplanır. | FR-21, Bölüm 6 | #43 | U: #44 | – |
| NFR-11 | Yüklenen dosyaların türü ve boyutu sınırlıdır. | §5, FR-11 | #111, #112 | U: #114 | `GlobalExceptionHandlerTest` (413/415 gövdesi); sınır denetimi #111'de |
| NFR-12 | Dosya türü uzantıya göre değil içeriğe göre denetlenir. | §5, FR-11 | #111 | U: #114 | – |
| NFR-13 | Kod modüler monolit mimarisine uyar. | architecture.md, ADR-0001 | #6 ✓, #86 | M: #86 | – |
| NFR-14 | Her değişiklik otomatik olarak doğrulanır. | CONTRIBUTING.md | #84 ✓ | İ: PR hızlı kontrolü | – |
| NFR-15 | API sözleşmesi kodla uyumludur. | CLAUDE.md | #66 | İ: #66 | – |
| NFR-16 | Sistem tek komutla ayağa kalkar. | §5 | #73 | D: #68, #106 | – |
| NFR-17 | Arayüz güncel tarayıcılarda çalışır. | §5 | #71 | D: #68 | – |
| NFR-18 | Yeni içerik türü mevcut kodu değiştirmeden eklenebilir. | CLAUDE.md (tasarım desenleri) | #110 | İ, U: #97, #114 | – |
| NFR-19 | Modüller arası tepkiler gevşek bağlıdır. | architecture.md | #42, #124 | İ, U, M: #137, #86 | – |

## 3. Test issue'su olmayan maddeler ve nedenleri

| Madde | Neden | Nasıl doğrulanacak |
|---|---|---|
| FR-14, FR-36 (P2) | Ayrı test issue'su açılmadı | Özellik #143'te geliştirilirken aynı PR'da unit test |
| FR-23, FR-24 (P1) | Ayrı test issue'su açılmadı | #121 ve #48'in PR'larında unit test (test planı bölüm 4) |
| FR-39, FR-40, FR-41 (P2) | Ayrı test issue'su açılmadı | #60, #118, #144'ün PR'larında unit test. FR-39'un "pasif kullanıcı giriş yapamaz" kısmı bugün `AuthServiceTest` ve `SecurityConfigTest`'te testli |
| NFR-06, NFR-14 | Otomatik test değil, inceleme | Her PR'da hızlı kontrol: repoda gizli bilgi yok, CI yeşil |
| FR-29, NFR-07, NFR-16, NFR-17 | Görsel ya da ortam kontrolü; otomatik testi anlamlı değil | Demo / manuel: #68 bug bash, #106 demo provası |

P2 gereksinimler (FR-14, 36, 39, 40, 41) Hafta 4'ün P2 kararına göre kapsamdan çıkarsa bu satırlar "kapsam dışı" olarak güncellenecek.

## 4. Özet (16.11.2026)

| | FR | NFR |
|---|---|---|
| Gereksinim sayısı | 44 | 19 |
| En az bir geliştirme issue'su olan | 44 | 19 |
| Test issue'su ya da manuel doğrulaması olan | 37 | 19 |
| Bugün `main`'de en az bir otomatik testi olan | 6 (FR-02, 05, 07, 09, 15, 39) | 4 (NFR-04, 06, 08, 11) |

Bugünkü testlerin çoğu altyapı ve auth içindir (70 test metodu). Kurs, kayıt, ilerleme, sınav ve sertifika testleri bu modüllerin kodu geldikçe yazılacak.
