# Kullanım senaryoları

Bu doküman proje tanım dokümanındaki 5 temel senaryoyu ve [use-case diyagramındaki](diagrams/use-case.puml) her kullanım durumunu (UC-xx) metin olarak açıklar (proje raporu şablonu, bölüm 3.4.1; #79). Kimlikler diyagramla aynıdır. Gereksinimler: [requirements.md](requirements.md); gereksinim → test eşlemesi: [traceability.md](traceability.md).

**Nasıl okunur**

- Her senaryo ve kullanım durumu aynı başlıklarla yazılır: **aktör**, **ön koşul**, **ana akış**, **alternatif / hata akışları**, **son koşul**, **gereksinimler ve issue'lar**.
- Hata akışlarındaki HTTP kodları [requirements.md](requirements.md)'deki kabul kriterlerinden ve API'nin standart hata gövdesinden gelir; kullanıcıya her zaman Türkçe, anlaşılır bir mesaj gösterilir (NFR-08).
- Issue durumu: `#12 ✓` kapandı (iş `main`'de), `#12` açık. **Veri tarihi:** 18.11.2026.
- Aktörler: **Ziyaretçi** (giriş yapmamış), **Kayıtlı kullanıcı** (kursiyer, eğitmen ve yöneticinin ortak işleri), **Kursiyer**, **Eğitmen**, **Yönetici**, **Sistem**.
- Gerçek ödeme kapsam dışıdır; kurslara kayıt ücretsizdir.

## İçindekiler

- [1. Temel senaryolar](#1-temel-senaryolar)
  - [Senaryo 1: Kurs yayımlama](#senaryo-1-kurs-yayımlama)
  - [Senaryo 2: Kursa kayıt ve öğrenme](#senaryo-2-kursa-kayıt-ve-öğrenme)
  - [Senaryo 3: Kaldığı yerden devam](#senaryo-3-kaldığı-yerden-devam)
  - [Senaryo 4: Sınav ve sertifika](#senaryo-4-sınav-ve-sertifika)
  - [Senaryo 5: Eğitmen analitiği](#senaryo-5-eğitmen-analitiği)
- [2. Kullanım durumları (UC-01 … UC-34)](#2-kullanım-durumları-uc-01--uc-34)
- [3. Özet tablo](#3-özet-tablo)

## 1. Temel senaryolar

Senaryolar birden fazla kullanım durumunu uçtan uca birleştirir. Her senaryonun manuel kontrol adımları [test-plan.md](test-plan.md) bölüm 6'da, uçtan uca testleri #65 ve #138'dedir.

### Senaryo 1: Kurs yayımlama

- **Aktörler:** Eğitmen (birincil), Yönetici
- **Kullanım durumları:** UC-19, UC-20, UC-21, UC-22, UC-23, UC-24, UC-27 (UC-34)
- **Ön koşul:** Eğitmen hesabıyla giriş yapılmış. En az bir kategori tanımlı.

**Ana akış**

1. Eğitmen başlık, açıklama, kategori, seviye, kapak görseli ve öğrenme hedefleriyle kurs oluşturur. Kurs **Taslak** durumunda başlar ve eğitmene aittir (UC-19).
2. Eğitmen kursa bölümler, bölümlere dersler ekler; her ders için içerik türünü (video, PDF, doküman), süresini ve zorunlu olup olmadığını belirler; bölüm ve ders sırasını düzenler (UC-20).
3. Eğitmen derslere içerik dosyalarını yükler; sistem dosyanın türünü içeriğinden ve boyutunu türün sınırına göre denetler (UC-21).
4. Eğitmen kursun soru bankasına çoktan seçmeli sorular ekler (UC-22).
5. Eğitmen bir bölüme bankadan soru seçerek bölüm sonu sınavı ekler, başarı eşiğini ve sınavın sertifika için zorunlu olup olmadığını belirler (UC-23).
6. Eğitmen kursu onaya gönderir; kurs **İncelemede** durumuna geçer (UC-24).
7. Yönetici incelemedeki kursu açar, içeriğini inceler (UC-34) ve onaylar (UC-27).
8. Kurs **Yayımlandı** durumuna geçer ve katalogda görünür. Eğitmene "kurs onaylandı" bildirimi oluşur.

**Alternatif / hata akışları**

- **7a. Red:** Yönetici kursu gerekçe yazarak reddeder. Kurs **Taslak**'a döner, gerekçe kursta saklanır ve eğitmen panelinde görünür; eğitmene "kurs reddedildi" bildirimi gerekçesiyle oluşur. Eğitmen düzeltip adım 6'dan devam eder. Gerekçesiz red kabul edilmez.
- **1a. Eksik bilgi:** Başlık ya da kategori boş → 400, hata ilgili alanın altında gösterilir.
- **3a. Geçersiz dosya:** İzin verilmeyen tür → 415; sınırı aşan dosya (video 200 MB, PDF/doküman 20 MB, kapak 2 MB) → 413.
- **4a. Geçersiz soru:** 2'den az şık ya da hiç doğru şık yok → soru kaydedilmez (400).
- **5a. Başka kursun sorusu:** Yalnız aynı kursun bankasındaki sorular seçilebilir. Eşik girilmezse %70 kabul edilir.
- **6a, 7b. Geçersiz durum geçişi:** Taslak dışındaki kurs onaya gönderilemez; İncelemede olmayan kurs onaylanamaz ya da reddedilemez → 409.
- **Yetki:** Başka bir eğitmenin kursunu düzenleme ya da ona içerik ekleme isteği → 403 (Bölüm 6 test durumu "Yetkisiz kurs düzenleme").

**Son koşul:** Kurs Yayımlandı durumunda, katalogda aranabilir ve kursiyerlerin kaydına açık.

**Gereksinimler:** FR-07, FR-08, FR-09, FR-10, FR-11, FR-23, FR-24, FR-37, FR-43
**Issue'lar:** #22 ✓, #23, #24, #25, #27, #33, #34, #38, #48 ✓, #50, #59, #62, #85, #110, #111 ✓, #112, #121 ✓, #122 ✓, #124

### Senaryo 2: Kursa kayıt ve öğrenme

- **Aktörler:** Kursiyer (birincil), Ziyaretçi (katalog adımları)
- **Kullanım durumları:** UC-04, UC-05, UC-10, UC-12 (UC-33), UC-13
- **Ön koşul:** Katalogda en az bir Yayımlandı durumunda kurs var. Kayıt adımından itibaren kursiyer hesabıyla giriş yapılmış.

**Ana akış**

1. Kullanıcı katalogda anahtar kelime, kategori, seviye ve eğitmene göre arar ve filtreler; sonuçlar sayfalı gelir ve yalnız yayımlanmış kursları içerir (UC-04).
2. Kullanıcı bir kursun ayrıntı sayfasını açar: açıklama, öğrenme hedefleri, bölüm/ders listesi, toplam süre ve değerlendirmeler görünür (UC-05).
3. Kursiyer kursa ücretsiz kayıt olur (UC-10). Eğitmene "yeni kayıt" bildirimi oluşur.
4. Kursiyer kurs oynatıcısında bir dersi açar; sistem kursiyerin kursa kayıtlı olduğunu doğrular (UC-12, UC-33). Video oynatılır ve ileri/geri sarılır, PDF tarayıcıda görüntülenir, doküman indirilir; önceki/sonraki ders geçişi vardır.
5. Kursiyer dersi tamamlandı olarak işaretler (UC-13).
6. Sistem bölüm ve kurs ilerleme yüzdesini yeniden hesaplar (tamamlanan ders / toplam ders × 100).

**Alternatif / hata akışları**

- **1a. Sonuç yok:** Filtrelere uyan kurs yoksa boş liste gösterilir.
- **3a. Giriş yapılmamış:** Ziyaretçi kayıt olmak isterse giriş sayfasına yönlendirilir (401).
- **3b. Mükerrer kayıt:** Aynı kursa ikinci kayıt → 409, veritabanında tek kayıt kalır (Bölüm 6 test durumu "Mükerrer kayıt").
- **3c. Yayımlanmamış kurs:** Yayımlandı dışındaki bir kursa kayıt reddedilir.
- **4a. Kayıtsız erişim:** Kursa kayıtlı olmayan kursiyerin ders içeriği isteği → 403 (Bölüm 6 test durumu "Kursa kayıt olmadan özel derslere erişim").
- **5a. Tekrar işaretleme:** Aynı dersi ikinci kez tamamlandı işaretlemek yeni kayıt oluşturmaz, yüzde değişmez.
- **6a. Dersi olmayan bölüm:** İlerleme %0'dır (Bölüm 6 test durumu "Yanlış ilerleme hesaplaması": 20 dersin 15'i → %75).

**Son koşul:** Kursiyer kursa kayıtlı, tamamladığı dersler işaretli ve ilerleme yüzdesi doğru.

**Gereksinimler:** FR-05, FR-06, FR-12, FR-13, FR-15, FR-17, FR-18, FR-20, FR-21, FR-43
**Issue'lar:** #26 ✓, #29 ✓, #30 ✓, #35 ✓, #36 ✓, #41, #43, #45, #46, #113, #124, #185 ✓

### Senaryo 3: Kaldığı yerden devam

- **Aktör:** Kursiyer
- **Kullanım durumları:** UC-02, UC-11, UC-14
- **Ön koşul:** Kursiyer en az bir kursa kayıtlı ve o kursta en az bir ders açmış.

**Ana akış**

1. Kursiyer oturumu kapatıp daha sonra tekrar giriş yapar (UC-02).
2. Kursiyer kayıtlı kurslar panelini açar; her kurs için ilerleme yüzdesi ve son izlenen ders görünür (UC-11).
3. Kursiyer bir kursta "Kaldığın yerden devam et"e tıklar.
4. Sistem son izlenen dersi oynatıcıda açar (UC-14).

**Alternatif / hata akışları**

- **4a. Hiç ders açılmamış:** Düğme "Derse başla" olur ve ilk tamamlanmamış dersi açar; bütün dersler tamamlanmışsa kursun ilk dersini.
- **2a. Kayıtlı kurs yok:** Panel boş gösterilir ve kataloğa yönlendirir.
- **2b. Yayından kaldırılmış kurs:** Daha önce kaydolan kursiyer kursu panelde görmeye ve içeriğe erişmeye devam eder.

**Son koşul:** Kursiyer son kaldığı dersten devam ediyor; panel ve kurs sayfası aynı ilerlemeyi gösteriyor.

**Gereksinimler:** FR-02, FR-19, FR-22
**Issue'lar:** #13 ✓, #18 ✓, #30 ✓, #37 ✓, #119, #120

### Senaryo 4: Sınav ve sertifika

- **Aktörler:** Kursiyer (birincil), Sistem, Ziyaretçi (doğrulama)
- **Kullanım durumları:** UC-15, UC-16, UC-17, UC-06
- **Ön koşul:** Kursiyer kursa kayıtlı. Kursun bölümlerinde sınav tanımlı.

**Ana akış**

1. Kursiyer bir bölüm sınavını açar; sorular doğru cevaplar olmadan gönderilir (UC-15).
2. Kursiyer cevaplarını gönderir; sistem sınavın puanlama türüne göre seçilen stratejiyle puanlar.
3. Sonuç gösterilir: puan, en yüksek puan, yüzde ve geçti/kaldı (yüzde ≥ başarı eşiği ise "geçti"). Kursiyere "sınav sonucu" bildirimi oluşur.
4. Kursiyer tüm zorunlu dersleri tamamlamış ve tüm zorunlu sınavları geçmişse sistem doğrulama kodlu sertifikayı otomatik oluşturur; kursiyere "sertifika oluşturuldu" bildirimi gider.
5. Kursiyer sertifikasını görüntüler; sayfada "platform içi katılım belgesidir, resmî akreditasyon değildir" ibaresi bulunur (UC-17).
6. Herhangi biri doğrulama koduyla sertifikayı giriş yapmadan doğrular; kursiyerin adı, kurs adı ve düzenlenme tarihi görünür (UC-06).

**Alternatif / hata akışları**

- **3a. Kaldı:** Yüzde eşiğin altındaysa sonuç "kaldı" olur; deneme sayısı sınırsızdır, kursiyer adım 1'den tekrar dener. Önceki denemeler listelenir (UC-16).
- **1a. Kayıtsız kursiyer:** Kursa kayıtlı olmayan kursiyer sınava giremez (403).
- **4a. Koşul sağlanmadı:** Eksik zorunlu ders ya da geçilmemiş zorunlu sınav varsa sertifika oluşmaz.
- **4b. Mükerrer sertifika:** Koşul tekrar sağlansa da aynı kurs için ikinci sertifika oluşmaz.
- **6a. Geçersiz kod:** "Bulunamadı" gösterilir.

**Son koşul:** Kursiyerin sınav denemeleri kayıtlı; koşullar sağlandıysa tek ve doğrulanabilir bir sertifikası var.

**Gereksinimler:** FR-25, FR-26, FR-27, FR-28, FR-29, FR-43
**Issue'lar:** #42, #49 ✓, #52 ✓, #54, #55 ✓, #57, #58, #124, #129, #130, #131, #141

### Senaryo 5: Eğitmen analitiği

- **Aktör:** Eğitmen
- **Kullanım durumu:** UC-26
- **Ön koşul:** Eğitmen hesabıyla giriş yapılmış. Eğitmenin en az bir yayımlanmış ve kayıt almış kursu var.

**Ana akış**

1. Eğitmen panelini açar; kendi kursları durumları ve kayıtlı kursiyer sayılarıyla listelenir.
2. Eğitmen bir kursu seçer; tamamlanma oranı (kursu %100 tamamlayan / kayıtlı kursiyer) ve her sınav için deneme sayısı ile başarı oranı görünür.
3. Eğitmen kursiyer listesini açar; her kursiyer için kayıt tarihi, ilerleme yüzdesi, son erişim ve sınav sonuçları görünür.
4. Eğitmen kursun değerlendirme istatistiklerini (ortalama puan, puan dağılımı) görür.

**Alternatif / hata akışları**

- **1a. Reddedilmiş kurs:** Taslak'a dönen kursta red gerekçesi listede görünür.
- **2a. Kayıt yok:** Oranlar %0 ya da "veri yok" olarak gösterilir.
- **3a. Başka eğitmenin kursu:** Başka eğitmenin kursuna ait veriler istenirse → 403.

**Son koşul:** Gösterilen değerler veritabanındaki kayıtlarla aynı.

**Gereksinimler:** FR-33, FR-34, FR-35, FR-36
**Issue'lar:** #28, #38, #133, #134, #135, #143

## 2. Kullanım durumları (UC-01 … UC-34)

Modül sırasıyla (A–L). Senaryolarda ayrıntılı anlatılan adımlar burada kısaltılmıştır.

### A. Kullanıcı ve yetkilendirme

#### UC-01 Kayıt ol

- **Aktör:** Ziyaretçi
- **Ön koşul:** Kullanıcı giriş yapmamış.
- **Ana akış:**
  1. Ziyaretçi ad soyad, e-posta, parola ve rolünü (kursiyer ya da eğitmen) girer.
  2. Sistem bilgileri doğrular ve hesabı oluşturur.
  3. Kullanıcı giriş sayfasına yönlendirilir.
- **Alternatif / hata:** Kayıtlı e-posta → 409 (e-posta alanında). Parola 8 karakterden kısa ya da alan eksik → 400. Yönetici rolüyle kayıt kabul edilmez.
- **Son koşul:** Seçilen rolde aktif bir hesap var.
- **Gereksinimler / issue'lar:** FR-01 · #12 ✓, #17 ✓

#### UC-02 Giriş yap

- **Aktör:** Ziyaretçi
- **Ön koşul:** Kullanıcının aktif bir hesabı var.
- **Ana akış:**
  1. Kullanıcı e-posta ve parolasını girer.
  2. Sistem bilgileri doğrular ve oturum belirteci (JWT) verir.
  3. Kullanıcı rolüne uygun sayfaya yönlendirilir.
- **Alternatif / hata:** Yanlış e-posta ya da parola → 401; hangisinin yanlış olduğu söylenmez. Pasifleştirilmiş hesap giriş yapamaz. Parolasını unutan kullanıcı UC-03'e geçer (<<extend>>).
- **Son koşul:** Kullanıcının geçerli bir oturumu var.
- **Gereksinimler / issue'lar:** FR-02, FR-39 · #13 ✓, #18 ✓, #19 ✓

#### UC-03 Parolayı yenile

- **Aktör:** Ziyaretçi
- **Ön koşul:** Kullanıcı giriş sayfasında "parolamı unuttum"u seçmiş (UC-02'yi genişletir).
- **Ana akış:**
  1. Kullanıcı e-posta adresini girer.
  2. Sistem tek kullanımlık, süreli bir yenileme bağlantısı üretir.
  3. Kullanıcı bağlantıyı açıp yeni parolasını belirler.
- **Alternatif / hata:** Kayıtlı olmayan e-posta için de aynı yanıt verilir (kullanıcı varlığı açığa çıkmaz). Kullanılmış ya da süresi geçmiş bağlantı reddedilir.
- **Son koşul:** Kullanıcı yeni parolasıyla giriş yapabilir; bağlantı geçersiz.
- **Gereksinimler / issue'lar:** FR-03 · #115, #117

#### UC-07 Profili yönet

- **Aktör:** Kayıtlı kullanıcı
- **Ön koşul:** Kullanıcı giriş yapmış.
- **Ana akış:**
  1. Kullanıcı profil sayfasını açar.
  2. Ad soyad ve kısa biyografisini günceller.
  3. Sistem değişiklikleri kaydeder ve profilde gösterir.
- **Alternatif / hata:** E-posta ve rol bu işlemle değiştirilemez. Geçersiz alan → 400.
- **Son koşul:** Profil güncel.
- **Gereksinimler / issue'lar:** FR-04 · #116, #117

#### UC-33 Erişim yetkisini doğrula

- **Aktör:** Sistem (UC-12 ve UC-34 tarafından dahil edilir, <<include>>)
- **Ön koşul:** Korumalı bir kaynağa (ör. ders içeriği) istek gelmiş.
- **Ana akış:**
  1. Sistem isteğin oturum belirtecini doğrular.
  2. Kullanıcının rolünü ve kaynağa ilişkisini denetler: ders içeriğinde kursiyer kursa kayıtlı olmalı; kursun eğitmeni ve yönetici her zaman erişebilir.
  3. Yetki varsa istek işlenir.
- **Alternatif / hata:** Belirteç yok ya da geçersiz → 401. Rol yetersiz ya da kursa kayıt yok → 403. Kontrol arayüzde gizlenmiş olsa bile sunucuda yapılır.
- **Son koşul:** Yalnız yetkili kullanıcı kaynağa erişmiş.
- **Gereksinimler / issue'lar:** FR-05, FR-06 · #10 ✓, #14 ✓, #19 ✓, #113

### B. Kurs oluşturma ve yönetimi

#### UC-19 Kurs oluştur / düzenle

- **Aktör:** Eğitmen
- **Ön koşul:** Eğitmen giriş yapmış.
- **Ana akış:**
  1. Eğitmen başlık, açıklama, kategori, seviye, kapak görseli ve öğrenme hedeflerini girer.
  2. Sistem kursu Taslak durumunda, eğitmene ait olarak oluşturur.
  3. Eğitmen daha sonra kurs bilgilerini düzenleyebilir ya da Taslak kursu silebilir.
- **Alternatif / hata:** Başlık ya da kategori eksik → 400. Başka eğitmenin kursunu düzenleme/silme → 403. Kurs yalnız Taslak ve Yayından kaldırıldı durumlarında düzenlenebilir, yalnız Taslak'ta silinebilir; aksi halde istek reddedilir.
- **Son koşul:** Eğitmene ait, Taslak durumunda bir kurs var.
- **Gereksinimler / issue'lar:** FR-07, FR-10 · #22 ✓, #23, #33

#### UC-20 Bölüm ve ders yönet

- **Aktör:** Eğitmen
- **Ön koşul:** Eğitmenin düzenlenebilir durumda bir kursu var.
- **Ana akış:**
  1. Eğitmen kursa bölüm ekler.
  2. Bölüme ders ekler; içerik türünü (video, PDF, doküman), süresini ve zorunlu olup olmadığını belirtir.
  3. Bölüm ve derslerin sırasını değiştirir.
- **Alternatif / hata:** Başka eğitmenin kursu → 403. Eksik alan → 400.
- **Son koşul:** Bölümler ve dersler eğitmenin belirlediği sırayla gösterilir; kursun toplam süresi derslerin toplamıdır.
- **Gereksinimler / issue'lar:** FR-08 · #24, #25, #34

#### UC-24 Kursu onaya gönder

- **Aktör:** Eğitmen
- **Ön koşul:** Kurs eğitmene ait ve Taslak ya da Yayından kaldırıldı durumunda.
- **Ana akış:**
  1. Eğitmen panelinde kursu onaya gönderir.
  2. Sistem geçişin kursun mevcut durumunda izinli olduğunu denetler.
  3. Kurs İncelemede durumuna geçer ve yöneticinin inceleme listesine düşer.
- **Alternatif / hata:** İzinli olmayan geçiş (ör. zaten İncelemede ya da Yayımlandı) → 409. Başka eğitmenin kursu → 403.
- **Son koşul:** Kurs İncelemede.
- **Gereksinimler / issue'lar:** FR-09 · #27, #38, #85, #123

#### UC-25 Kursu yayından kaldır

- **Aktörler:** Eğitmen (kendi kursu), Yönetici
- **Ön koşul:** Kurs Yayımlandı durumunda.
- **Ana akış:**
  1. Eğitmen ya da yönetici kursu yayından kaldırır; yönetici gerekçe yazar.
  2. Kurs Yayından kaldırıldı durumuna geçer; gerekçe kursta saklanır.
  3. Kurs katalogda görünmez ve yeni kayıt almaz.
- **Alternatif / hata:** Yayımlandı olmayan kurs → 409. Başka eğitmenin kursu → 403. Daha önce kaydolmuş kursiyerler içeriğe erişmeye devam eder. Eğitmen kursu düzenleyip yeniden onaya gönderebilir (UC-24).
- **Son koşul:** Kurs Yayından kaldırıldı.
- **Gereksinimler / issue'lar:** FR-09, FR-38 · #62, #85, #123

### C. Video ve eğitim materyalleri

#### UC-21 Ders içeriği yükle

- **Aktör:** Eğitmen
- **Ön koşul:** Eğitmenin düzenlenebilir bir kursunda ders var.
- **Ana akış:**
  1. Eğitmen derse içerik dosyası seçer.
  2. Sistem dosya türünü uzantıdan değil içerikten tespit eder ve türün boyut sınırını denetler.
  3. Dosya yetki kontrollü depoya kaydedilir ve derse bağlanır.
- **Alternatif / hata:** İzin verilmeyen tür → 415. Sınırı aşan dosya (video mp4/webm 200 MB; PDF ve doküman pdf/docx/pptx 20 MB; kapak jpg/png/webp 2 MB) → 413. Başka eğitmenin kursu → 403.
- **Son koşul:** Ders içeriği kursa kayıtlı kursiyerlere açılmaya hazır.
- **Gereksinimler / issue'lar:** FR-11 · #34, #110, #111 ✓, #112
- **Not:** Bir derse ana içeriğin yanında ek doküman bağlanıp bağlanamayacağı açık bir tasarım kararıdır (FR-11); #110'dan önce karar verilecek ve bu bölüm ona göre güncellenecek.

#### UC-12 Ders izle

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer kursa kayıtlı.
- **Ana akış:**
  1. Kursiyer kurs oynatıcısında bir dersi seçer.
  2. Sistem erişim yetkisini doğrular (UC-33, <<include>>).
  3. İçerik türüne göre sunulur: video oynatılır ve ileri/geri sarılır; PDF tarayıcıda görüntülenir; doküman indirilir.
  4. Kursiyer bölüm/ders listesinden ya da önceki/sonraki geçişiyle başka derse geçer.
- **Alternatif / hata:** Kayıtsız kursiyer → 403. İçerik bulunamadı → 404.
- **Son koşul:** Ders son izlenen ders olarak kaydedilir (UC-14).
- **Gereksinimler / issue'lar:** FR-06, FR-12 · #45, #113

### D. Kurs kataloğu ve arama

#### UC-04 Kurs ara / filtrele

- **Aktör:** Ziyaretçi (ve tüm kayıtlı kullanıcılar)
- **Ön koşul:** Yok.
- **Ana akış:**
  1. Kullanıcı anahtar kelime girer ve kategori, seviye, eğitmen ya da en az puan filtresi seçer.
  2. Sistem başlık ve açıklamada arar, filtreleri uygular.
  3. Sonuçlar sayfalı listelenir.
- **Alternatif / hata:** Sonuç yoksa boş liste. Geçersiz parametre → 400.
- **Son koşul:** Listede yalnız Yayımlandı durumundaki ve tüm filtrelere uyan kurslar var.
- **Gereksinimler / issue'lar:** FR-13, FR-14 · #26 ✓, #35 ✓, #143

#### UC-05 Kurs ayrıntısını görüntüle

- **Aktör:** Ziyaretçi (ve tüm kayıtlı kullanıcılar)
- **Ön koşul:** Kurs Yayımlandı durumunda.
- **Ana akış:**
  1. Kullanıcı katalogdan bir kursu açar.
  2. Açıklama, öğrenme hedefleri, bölüm/ders listesi ve toplam süre gösterilir.
  3. Ortalama puan, puan dağılımı ve yorumlar (sayfalı) gösterilir.
- **Alternatif / hata:** Kurs yok ya da yayımlanmamış → 404. Ders içerikleri yalnız kayıtlı kursiyere açılır (UC-33).
- **Son koşul:** Kullanıcı kayıt kararı için gerekli bilgiyi görmüş.
- **Gereksinimler / issue'lar:** FR-15, FR-16 · #26 ✓, #36 ✓, #126 ✓, #127 ✓, #185 ✓

### E. Kursa kayıt

#### UC-10 Kursa kayıt ol

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer giriş yapmış; kurs Yayımlandı durumunda.
- **Ana akış:**
  1. Kursiyer kurs ayrıntı sayfasında "Kayıt ol"a tıklar.
  2. Sistem kaydı ödeme adımı olmadan oluşturur.
  3. Kursun eğitmenine "yeni kayıt" bildirimi oluşur.
- **Alternatif / hata:** Aynı kursa ikinci kayıt → 409, veritabanında tek kayıt. Yayımlandı dışındaki kurs → kayıt reddedilir. Giriş yapılmamış → 401.
- **Son koşul:** Kursiyer kursun ders içeriklerine erişebilir.
- **Gereksinimler / issue'lar:** FR-17, FR-18, FR-43 · #29 ✓, #30 ✓, #36 ✓, #124

#### UC-11 Kayıtlı kursları görüntüle

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer giriş yapmış.
- **Ana akış:**
  1. Kursiyer kayıtlı kurslar panelini açar.
  2. Her kurs için ilerleme yüzdesi ve son izlenen ders gösterilir.
  3. Kursiyer bir kursa tıklayınca kaldığı derse gider (UC-14).
- **Alternatif / hata:** Kayıtlı kurs yoksa boş panel.
- **Son koşul:** —
- **Gereksinimler / issue'lar:** FR-19 · #30 ✓, #37 ✓, #119

### F. İzleme ve ilerleme

#### UC-13 Dersi tamamla

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer kursa kayıtlı ve dersi açmış.
- **Ana akış:**
  1. Kursiyer dersi tamamlandı olarak işaretler.
  2. Sistem tamamlama kaydını oluşturur ve ders tamamlandı olayını yayınlar.
  3. Bölüm ve kurs ilerleme yüzdesi yeniden hesaplanır.
- **Alternatif / hata:** Aynı dersi tekrar işaretlemek yeni kayıt oluşturmaz. Kayıtsız kursiyer → 403.
- **Son koşul:** Ders listede işaretli; yüzde = tamamlanan / toplam ders × 100 (20 dersin 15'i → %75; dersi olmayan bölüm %0).
- **Gereksinimler / issue'lar:** FR-20, FR-21 · #41, #42, #43, #46

#### UC-14 Kaldığı yerden devam et

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer kursa kayıtlı.
- **Ana akış:**
  1. Kursiyer kurs sayfasında ya da kayıtlı kurslar panelinde "Kaldığın yerden devam et"e tıklar.
  2. Sistem son izlenen dersi açar.
- **Alternatif / hata:** Hiç ders açılmamışsa düğme "Derse başla" olur ve ilk tamamlanmamış dersi açar.
- **Son koşul:** Kursiyer son kaldığı derste.
- **Gereksinimler / issue'lar:** FR-22 · #119, #120

### G. Sınav ve değerlendirme

#### UC-22 Soru bankası yönet

- **Aktör:** Eğitmen
- **Ön koşul:** Eğitmenin bir kursu var.
- **Ana akış:**
  1. Eğitmen kursun soru bankasını açar.
  2. Çoktan seçmeli soru ekler, düzenler ya da siler.
- **Alternatif / hata:** 2'den az şık ya da doğru şık yok → soru kaydedilmez (400). Bir sınavda kullanılan ya da denemelerde cevaplanmış soru silinemez (409). Başka eğitmenin kursu → 403.
- **Son koşul:** Soru bankası güncel.
- **Gereksinimler / issue'lar:** FR-23 · #121 ✓, #122 ✓

#### UC-23 Bölüm sınavı oluştur

- **Aktör:** Eğitmen
- **Ön koşul:** Kursun soru bankasında soru var.
- **Ana akış:**
  1. Eğitmen bir bölüm seçer ve bankadan sorular seçer.
  2. Başarı eşiğini (yüzde) ve sınavın sertifika için zorunlu olup olmadığını belirler.
  3. Sistem sınavı bölüme bağlar.
- **Alternatif / hata:** Eşik girilmezse %70. Başka kursun bankasından soru → reddedilir (400). Başka eğitmenin kursu → 403.
- **Son koşul:** Bölümün sınavı var.
- **Gereksinimler / issue'lar:** FR-24 · #48 ✓, #50

#### UC-15 Sınava gir

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer kursa kayıtlı; bölümde sınav var.
- **Ana akış:**
  1. Kursiyer sınavı açar; sorular doğru cevaplar olmadan gelir.
  2. Cevaplarını gönderir.
  3. Sistem sınavın puanlama türüne göre seçilen stratejiyle puanlar ve sonucu kaydeder.
  4. Puan, en yüksek puan, yüzde ve geçti/kaldı gösterilir; kursiyere "sınav sonucu" bildirimi oluşur.
- **Alternatif / hata:** Kayıtsız kursiyer → 403. Yüzde eşiğin altında → "kaldı"; deneme sayısı sınırsız.
- **Son koşul:** Deneme kaydedilmiş; geçildiyse sertifika koşulu yeniden değerlendirilir (UC-17).
- **Gereksinimler / issue'lar:** FR-25, FR-43 · #49 ✓, #52 ✓, #54, #57, #58, #124

#### UC-16 Sınav sonuçlarını görüntüle

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer sınava en az bir kez girmiş.
- **Ana akış:**
  1. Kursiyer sınavın deneme geçmişini açar.
  2. Denemeler en yeniden eskiye tarih, puan ve geçti/kaldı ile listelenir.
- **Alternatif / hata:** Deneme yoksa boş liste. Başkasının denemeleri görüntülenemez.
- **Son koşul:** —
- **Gereksinimler / issue'lar:** FR-26 · #55 ✓, #141

### H. Dijital sertifika

#### UC-17 Sertifikayı görüntüle

- **Aktörler:** Kursiyer, Sistem
- **Ön koşul:** Kursiyer kursun tüm zorunlu derslerini tamamlamış ve tüm zorunlu sınavlarını geçmiş.
- **Ana akış:**
  1. Koşul sağlandığında sistem benzersiz, tahmin edilemeyen doğrulama kodlu sertifikayı otomatik oluşturur ve kursiyere bildirir.
  2. Kursiyer sertifikasını görüntüler.
  3. Sayfada "platform içi katılım belgesidir, resmî akreditasyon değildir" ibaresi görünür.
- **Alternatif / hata:** Koşul sağlanmadıysa sertifika yok. Aynı kurs için ikinci sertifika oluşmaz.
- **Son koşul:** Kursiyerin o kurs için tek bir sertifikası var.
- **Gereksinimler / issue'lar:** FR-27, FR-29, FR-43 · #124, #129, #130, #131

#### UC-06 Sertifika doğrula

- **Aktör:** Ziyaretçi (herkes)
- **Ön koşul:** Yok; giriş gerekmez.
- **Ana akış:**
  1. Kullanıcı doğrulama sayfasına kodu girer.
  2. Geçerli kodda kursiyerin adı, kurs adı ve düzenlenme tarihi gösterilir; akreditasyon dışı ibaresi görünür.
- **Alternatif / hata:** Geçersiz kod → "bulunamadı".
- **Son koşul:** —
- **Gereksinimler / issue'lar:** FR-28, FR-29 · #130, #131

### I. Puanlama ve yorumlar

#### UC-18 Kursu değerlendir

- **Aktör:** Kursiyer
- **Ön koşul:** Kursiyer kursa kayıtlı.
- **Ana akış:**
  1. Kursiyer 1–5 yıldız puan ve yorum girer.
  2. Sistem değerlendirmeyi kaydeder; kursun ortalama puanı güncellenir.
  3. Kursiyer daha sonra kendi değerlendirmesini düzenleyebilir.
- **Alternatif / hata:** Kayıtlı olmayan kullanıcı → 403. İkinci değerlendirme → 409. Puan 1–5 dışında → 400.
- **Son koşul:** Kursiyerin o kurs için tek bir değerlendirmesi var.
- **Gereksinimler / issue'lar:** FR-30, FR-31 · #126 ✓, #127 ✓

#### UC-31 Yorumları moderasyonla

- **Aktör:** Yönetici
- **Ön koşul:** Yönetici giriş yapmış.
- **Ana akış:**
  1. Yönetici uygunsuz bir yorumu gerekçe yazarak gizler.
  2. Gizlenen yorum herkese açık listede görünmez ve ortalama puana katılmaz.
  3. Yönetici gerekirse yorumu yeniden açar.
- **Alternatif / hata:** Yönetici dışındaki rol → 403. Yorum yok → 404.
- **Son koşul:** Yorumun görünürlüğü ve gerekçesi kayıtlı.
- **Gereksinimler / issue'lar:** FR-32 · #142, #146

### J. Eğitmen paneli

#### UC-26 Eğitmen istatistiklerini görüntüle

- **Aktör:** Eğitmen
- **Ön koşul:** Eğitmen giriş yapmış.
- **Ana akış:** [Senaryo 5](#senaryo-5-eğitmen-analitiği), adım 1–4.
- **Alternatif / hata:** Başka eğitmenin kursu → 403. Kayıt yoksa oranlar %0.
- **Son koşul:** Değerler veritabanındaki kayıtlarla aynı.
- **Gereksinimler / issue'lar:** FR-33, FR-34, FR-35, FR-36 · #28, #38, #133, #134, #135, #143

### K. Yönetici paneli

#### UC-27 Kursu onayla / reddet

- **Aktör:** Yönetici
- **Ön koşul:** Kurs İncelemede durumunda.
- **Ana akış:**
  1. Yönetici incelemedeki kurslar listesinden bir kursu açar.
  2. Kurs içeriğini inceler (UC-34, <<include>>).
  3. Onaylar: kurs Yayımlandı olur ve katalogda görünür; eğitmene "kurs onaylandı" bildirimi gider.
- **Alternatif / hata:** Gerekçeyle reddeder: kurs Taslak'a döner, gerekçe kursta saklanır, eğitmene gerekçeli bildirim gider. Gerekçesiz red → 400. İncelemede olmayan kurs → 409.
- **Son koşul:** Kurs Yayımlandı ya da gerekçesiyle Taslak.
- **Gereksinimler / issue'lar:** FR-09, FR-37, FR-43 · #59, #62, #85, #124

#### UC-34 Kurs içeriğini incele

- **Aktör:** Yönetici (UC-27 tarafından dahil edilir, <<include>>)
- **Ön koşul:** Kurs İncelemede durumunda.
- **Ana akış:**
  1. Yönetici kursun bölüm/ders yapısını ve ders içeriklerini açar.
  2. Sistem yönetici rolüyle içerik erişimine izin verir (UC-33).
- **Alternatif / hata:** Yönetici dışındaki rol → 403.
- **Son koşul:** Yönetici onay/red kararı için içeriği görmüş.
- **Gereksinimler / issue'lar:** FR-06, FR-37 · #59, #62, #113

#### UC-28 Kullanıcıları yönet

- **Aktör:** Yönetici
- **Ön koşul:** Yönetici giriş yapmış.
- **Ana akış:**
  1. Yönetici kullanıcıları rol filtresi ve aramayla sayfalı listeler.
  2. Bir hesabı pasifleştirir ya da yeniden aktifleştirir.
- **Alternatif / hata:** Yönetici kendi hesabını pasifleştiremez (409). Pasif kullanıcı giriş yapamaz (UC-02).
- **Son koşul:** Hesabın durumu güncel.
- **Gereksinimler / issue'lar:** FR-39 · #60 ✓, #63 ✓

#### UC-29 Kategorileri yönet

- **Aktör:** Yönetici
- **Ön koşul:** Yönetici giriş yapmış.
- **Ana akış:**
  1. Yönetici kategori ekler, adını düzenler ya da siler.
- **Alternatif / hata:** Aynı adda kategori → 409. Kursu olan kategori silinemez → 409.
- **Son koşul:** Kategori listesi güncel; kurs formunda ve katalog filtresinde kullanılır.
- **Gereksinimler / issue'lar:** FR-40 · #109 ✓, #118 ✓, #146

#### UC-09 Kurs / yorum şikâyet et

- **Aktör:** Kayıtlı kullanıcı
- **Ön koşul:** Kullanıcı giriş yapmış.
- **Ana akış:**
  1. Kullanıcı bir kursu ya da yorumu gerekçe yazarak şikâyet eder.
  2. Şikâyet "açık" durumunda yöneticinin listesine düşer.
- **Alternatif / hata:** Gerekçe eksik → 400. Kurs ya da yorum yok → 404.
- **Son koşul:** Açık bir şikâyet kaydı var.
- **Gereksinimler / issue'lar:** FR-41 · #144

#### UC-30 Şikâyetleri incele

- **Aktör:** Yönetici
- **Ön koşul:** Açık şikâyet var.
- **Ana akış:**
  1. Yönetici şikâyeti açar.
  2. Durumunu "incelendi" ya da "reddedildi" yapar.
  3. Gerekirse yorumu gizler (UC-31) ya da kursu yayından kaldırır (UC-25).
- **Alternatif / hata:** Yönetici dışındaki rol → 403.
- **Son koşul:** Şikâyetin durumu güncel.
- **Gereksinimler / issue'lar:** FR-41 · #144, #146

#### UC-32 Platform raporlarını görüntüle

- **Aktör:** Yönetici
- **Ön koşul:** Yönetici giriş yapmış.
- **Ana akış:**
  1. Yönetici rapor sayfasını açar ve isterse tarih aralığı seçer.
  2. Role göre kullanıcı sayısı, duruma göre kurs sayısı, kayıt, tamamlanma ve sertifika sayıları ile en çok kayıt alan kurslar gösterilir.
- **Alternatif / hata:** Geçersiz tarih aralığı → 400. Yönetici dışındaki rol → 403.
- **Son koşul:** —
- **Gereksinimler / issue'lar:** FR-42 · #145, #147

### L. Bildirimler

#### UC-08 Bildirimleri görüntüle

- **Aktör:** Kayıtlı kullanıcı
- **Ön koşul:** Kullanıcı giriş yapmış.
- **Ana akış:**
  1. Kullanıcı okunmamış bildirim sayısını görür.
  2. Bildirim listesini açar; bildirimler en yeniden eskiye listelenir.
  3. Bir bildirimi okundu işaretler; okunmamış sayısı düşer.
- **Alternatif / hata:** Başkasının bildirimi görüntülenemez ya da işaretlenemez (403/404).
- **Son koşul:** Okundu durumu güncel.
- **Bildirim üreten olaylar (FR-43):** kursa kayıt → eğitmen; yayımlanmış kursa yeni ders → kayıtlı kursiyerler; kurs onayı → eğitmen; kurs reddi (gerekçesiyle) → eğitmen; sınav sonucu → kursiyer; sertifika oluşturulması → kursiyer.
- **Gereksinimler / issue'lar:** FR-43, FR-44 · #42, #124, #125, #136

## 3. Özet tablo

| UC | Kullanım durumu | Aktör | Senaryo | Gereksinimler |
|---|---|---|---|---|
| UC-01 | Kayıt ol | Ziyaretçi | 1 | FR-01 |
| UC-02 | Giriş yap | Ziyaretçi | 3 | FR-02, FR-39 |
| UC-03 | Parolayı yenile | Ziyaretçi | – | FR-03 |
| UC-04 | Kurs ara / filtrele | Ziyaretçi | 2 | FR-13, FR-14 |
| UC-05 | Kurs ayrıntısını görüntüle | Ziyaretçi | 2 | FR-15, FR-16 |
| UC-06 | Sertifika doğrula | Ziyaretçi | 4 | FR-28, FR-29 |
| UC-07 | Profili yönet | Kayıtlı kullanıcı | – | FR-04 |
| UC-08 | Bildirimleri görüntüle | Kayıtlı kullanıcı | – | FR-43, FR-44 |
| UC-09 | Kurs / yorum şikâyet et | Kayıtlı kullanıcı | – | FR-41 |
| UC-10 | Kursa kayıt ol | Kursiyer | 2 | FR-17, FR-18, FR-43 |
| UC-11 | Kayıtlı kursları görüntüle | Kursiyer | 3 | FR-19 |
| UC-12 | Ders izle | Kursiyer | 2 | FR-06, FR-12 |
| UC-13 | Dersi tamamla | Kursiyer | 2 | FR-20, FR-21 |
| UC-14 | Kaldığı yerden devam et | Kursiyer | 3 | FR-22 |
| UC-15 | Sınava gir | Kursiyer | 4 | FR-25, FR-43 |
| UC-16 | Sınav sonuçlarını görüntüle | Kursiyer | 4 | FR-26 |
| UC-17 | Sertifikayı görüntüle | Kursiyer, Sistem | 4 | FR-27, FR-29, FR-43 |
| UC-18 | Kursu değerlendir | Kursiyer | – | FR-30, FR-31 |
| UC-19 | Kurs oluştur / düzenle | Eğitmen | 1 | FR-07, FR-10 |
| UC-20 | Bölüm ve ders yönet | Eğitmen | 1 | FR-08 |
| UC-21 | Ders içeriği yükle | Eğitmen | 1 | FR-11 |
| UC-22 | Soru bankası yönet | Eğitmen | 1 | FR-23 |
| UC-23 | Bölüm sınavı oluştur | Eğitmen | 1 | FR-24 |
| UC-24 | Kursu onaya gönder | Eğitmen | 1 | FR-09 |
| UC-25 | Kursu yayından kaldır | Eğitmen, Yönetici | – | FR-09, FR-38 |
| UC-26 | Eğitmen istatistiklerini görüntüle | Eğitmen | 5 | FR-33 … FR-36 |
| UC-27 | Kursu onayla / reddet | Yönetici | 1 | FR-09, FR-37, FR-43 |
| UC-28 | Kullanıcıları yönet | Yönetici | – | FR-39 |
| UC-29 | Kategorileri yönet | Yönetici | – | FR-40 |
| UC-30 | Şikâyetleri incele | Yönetici | – | FR-41 |
| UC-31 | Yorumları moderasyonla | Yönetici | – | FR-32 |
| UC-32 | Platform raporlarını görüntüle | Yönetici | – | FR-42 |
| UC-33 | Erişim yetkisini doğrula | Sistem | 2 | FR-05, FR-06 |
| UC-34 | Kurs içeriğini incele | Yönetici | 1 | FR-06, FR-37 |

44 fonksiyonel gereksinimin tamamı en az bir kullanım durumuna bağlıdır.
