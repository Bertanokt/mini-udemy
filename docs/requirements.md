# Fonksiyonel gereksinimler

Bu doküman Mini Udemy'nin fonksiyonel gereksinimlerini tanımlar (proje raporu şablonu, bölüm 3.2). Gereksinimler proje tanım dokümanının 3. bölümündeki modüllerden (A–L) ve 4. bölümündeki temel kullanım senaryolarından türetilmiştir.

- **ID:** `FR-NN`. Bir gereksinim silinirse numarası tekrar kullanılmaz.
- **Aktör:** Kursiyer, Eğitmen, Yönetici, Ziyaretçi (giriş yapmamış kullanıcı), Sistem.
- **Kaynak:** proje tanım dokümanındaki modül (A–L).
- **Kabul kriteri:** gereksinimin karşılandığını gösteren, test edilebilir ve ölçülebilir koşul.
- **Öncelik:** **P1** = dokümandaki 5 temel senaryonun, Bölüm 6'daki test durumlarının ya da tasarım çıktılarının çalışması için gerekli; **P2** = senaryo dışı ayrıntı.

Fonksiyonel olmayan gereksinimler (performans, güvenlik, mobil uyumluluk, dosya sınırları vb.) ayrı dokümandadır (#78).

## A. Kullanıcı ve yetkilendirme

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-01 | Ziyaretçi | Kullanıcı ad soyad, e-posta ve parola ile kursiyer ya da eğitmen olarak kayıt olabilir. | A | Geçerli bilgilerle kayıt olan kullanıcı oluşturulur. Kayıtlı e-postayla ikinci kayıt reddedilir (409). Yönetici rolüyle kayıt olunamaz. Parola en az 8 karakter olmalıdır. | P1 |
| FR-02 | Ziyaretçi | Kullanıcı e-posta ve parolasıyla giriş yapabilir. | A | Doğru bilgilerle giriş yapan kullanıcıya oturum (JWT) verilir ve rolüne uygun sayfaya yönlendirilir. Yanlış e-posta ya da parolada giriş reddedilir (401) ve hangisinin yanlış olduğu söylenmez. | P1 |
| FR-03 | Ziyaretçi | Kullanıcı parolasını unuttuğunda parola yenileme isteyebilir ve tek kullanımlık bağlantıyla yeni parola belirleyebilir. | A | Yenileme bağlantısı yalnız bir kez ve süresi içinde kullanılabilir; kullanılmış ya da süresi geçmiş bağlantı reddedilir. Kayıtlı olmayan e-posta için de aynı yanıt verilir (kullanıcı varlığı açığa çıkmaz). | P2 |
| FR-04 | Kursiyer, Eğitmen | Kullanıcı kendi profilini görüntüleyip ad soyad ve kısa biyografisini güncelleyebilir. | A | Güncellenen bilgiler profilde görünür. E-posta ve rol bu işlemle değiştirilemez. | P2 |
| FR-05 | Sistem | Her işlem için kullanıcının rolü sunucu tarafında denetlenir. | A | Giriş yapmamış kullanıcının korumalı isteği 401, yetkisiz rolün isteği 403 ile reddedilir. Kontrol arayüzde gizlenmiş olsa bile sunucuda yapılır. | P1 |
| FR-06 | Sistem | Ders içeriklerine erişim, kursa kayıt ve kullanıcının yetkisine bağlıdır. | A, C | Kursa kayıtlı olmayan kursiyerin ders içeriği isteği 403 ile reddedilir. Kursun eğitmeni ve yönetici içeriğe erişebilir. | P1 |

## B. Kurs oluşturma ve yönetimi

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-07 | Eğitmen | Eğitmen başlık, açıklama, kategori, seviye, kapak görseli ve öğrenme hedefleriyle kurs oluşturabilir. | B | Oluşturulan kurs "Taslak" durumundadır ve oluşturan eğitmene aittir. Başlık ve kategori zorunludur. | P1 |
| FR-08 | Eğitmen | Eğitmen kursuna bölümler, bölümlere dersler ekleyebilir; bölüm ve derslerin sırasını değiştirebilir. | B, C | Bölümler ve dersler eğitmenin belirlediği sırayla gösterilir. Her ders için içerik türü (video, PDF, doküman), süre ve zorunlu olup olmadığı belirtilir. | P1 |
| FR-09 | Eğitmen, Yönetici | Kurs Taslak, İncelemede, Yayımlandı ve Yayından kaldırıldı durumlarından birinde bulunur; durumlar arası geçişler kurallara bağlıdır. | B, K | İzin verilen geçişler: Taslak → İncelemede (eğitmen onaya gönderir); İncelemede → Yayımlandı (yönetici onaylar); İncelemede → Taslak (yönetici gerekçeyle reddeder); Yayımlandı → Yayından kaldırıldı; Yayından kaldırıldı → İncelemede. Bunların dışındaki geçiş reddedilir (409). Yalnız "Yayımlandı" durumundaki kurslar katalogda görünür. | P1 |
| FR-10 | Sistem | Bir kursu yalnız sahibi olan eğitmen düzenleyebilir ve silebilir. | B | Başka bir eğitmenin ya da kursiyerin düzenleme/silme isteği 403 ile reddedilir. Kurs yalnız "Taslak" ve "Yayından kaldırıldı" durumlarında düzenlenebilir, yalnız "Taslak" durumunda silinebilir. | P1 |

## C. Video ve eğitim materyalleri

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-11 | Eğitmen | Eğitmen derslere video, PDF ve yardımcı doküman yükleyebilir. | C | İzin verilen türler ve sınırlar: video (mp4, webm) en fazla 200 MB; PDF ve doküman (pdf, docx, pptx) en fazla 20 MB; kapak görseli (jpg, png, webp) en fazla 2 MB. Tür dosya içeriğinden denetlenir. İzin verilmeyen tür 415, sınırı aşan dosya 413 ile reddedilir. | P1 |
| FR-12 | Kursiyer | Kayıtlı kursiyer kurs oynatıcısında dersleri sırasıyla izleyebilir/okuyabilir. | C | Video oynatıcıda oynatılır ve ileri/geri sarılabilir; PDF tarayıcıda görüntülenir; doküman indirilebilir. Oynatıcıda bölüm/ders listesi ve önceki/sonraki ders geçişi bulunur. | P1 |

Erişim kontrolü: FR-06.

## D. Kurs kataloğu ve arama

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-13 | Ziyaretçi, Kursiyer | Kullanıcı yayımlanmış kursları anahtar kelime, kategori, seviye ve eğitmene göre arayıp filtreleyebilir. | D | Sonuçlar yalnız "Yayımlandı" durumundaki kursları içerir ve tüm filtrelere uyar. Sonuçlar sayfalı gelir. Anahtar kelime başlık ve açıklamada aranır. | P1 |
| FR-14 | Ziyaretçi, Kursiyer | Kullanıcı kursları ortalama puana göre filtreleyip sıralayabilir. | D | "En az N yıldız" filtresi yalnız ortalaması N ve üstü kursları döndürür. | P2 |
| FR-15 | Ziyaretçi, Kursiyer | Kurs ayrıntı sayfasında açıklama, öğrenme hedefleri (kazanımlar), bölüm/ders listesi ve toplam süre gösterilir. | D | Toplam süre derslerin sürelerinin toplamıdır. Ders içerikleri yalnız kayıtlı kursiyere açılır (FR-06). | P1 |
| FR-16 | Ziyaretçi, Kursiyer | Kurs ayrıntı sayfasında değerlendirmeler (ortalama puan, puan dağılımı, yorumlar) gösterilir. | D, I | Ortalama puan gizlenmemiş değerlendirmelerden hesaplanır. Yorumlar sayfalı gelir. | P2 |

## E. Kursa kayıt

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-17 | Kursiyer | Kursiyer yayımlanmış bir kursa ücretsiz kayıt olabilir. | E | Kayıt sonrası kursiyer ders içeriklerine erişebilir. "Yayımlandı" dışındaki kurslara kayıt reddedilir. Ödeme adımı yoktur. | P1 |
| FR-18 | Sistem | Aynı kursiyerin aynı kursa ikinci kez kaydı engellenir. | E | İkinci kayıt isteği 409 ile reddedilir ve veritabanında tek kayıt bulunur. | P1 |
| FR-19 | Kursiyer | Kursiyer kayıtlı olduğu kursları bir panelde görür. | E | Her kurs için ilerleme yüzdesi ve son izlenen ders gösterilir. Kursa tıklayınca kaldığı derse gidilir. | P1 |

Yayından kaldırılan bir kursa daha önce kaydolmuş kursiyerler içeriğe erişmeye devam eder; kursa yeni kayıt alınmaz.

## F. İzleme ve ilerleme takibi

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-20 | Kursiyer | Kursiyer izlediği dersi tamamlandı olarak işaretleyebilir. | F | Tamamlanan ders listede işaretli görünür. Aynı dersi tekrar işaretlemek yeni kayıt oluşturmaz. | P1 |
| FR-21 | Sistem | Bölüm ve kurs ilerleme yüzdesi tamamlanan derslerden otomatik hesaplanır. | F | Yüzde = tamamlanan ders sayısı / toplam ders sayısı × 100. Örnek: 20 dersin 15'i tamamlandığında kurs ilerlemesi %75. Dersi olmayan bölümde ilerleme %0'dır. Ders eklenir ya da silinirse yüzde yeniden hesaplanır. | P1 |
| FR-22 | Kursiyer | Kursiyer kursa geri döndüğünde son izlediği dersten devam edebilir. | F | Kurs sayfasında ve kayıtlı kurslar panelinde son izlenen ders ve ilerleme yüzdesi gösterilir; "Devam et" son izlenen dersi açar. Hiç ders izlenmemişse ilk ders açılır. | P1 |

## G. Sınav ve değerlendirme

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-23 | Eğitmen | Eğitmen kursu için çoktan seçmeli sorulardan oluşan bir soru bankası oluşturabilir. | G | Her soruda en az 2 şık ve en az 1 doğru şık bulunur; aksi halde soru kaydedilmez. Bir sınavda kullanılan soru silinemez. | P1 |
| FR-24 | Eğitmen | Eğitmen bir bölüme, soru bankasından soru seçerek bölüm sonu sınavı ekleyebilir ve başarı eşiğini belirleyebilir. | G | Başarı eşiği yüzde olarak girilir; girilmezse %70 kabul edilir. Sınavın sertifika için zorunlu olup olmadığı belirtilir. Yalnız aynı kursun bankasındaki sorular seçilebilir. | P1 |
| FR-25 | Kursiyer, Sistem | Kayıtlı kursiyer sınava girer; cevapları otomatik puanlanır. | G | Sonuç; puan, en yüksek puan, yüzde ve geçti/kaldı bilgisini içerir. Yüzde başarı eşiğine eşit ya da üstündeyse "geçti". Doğru cevaplar sınav sırasında kursiyere gönderilmez. Deneme sayısı sınırsızdır. | P1 |
| FR-26 | Kursiyer | Kursiyer bir sınavdaki önceki denemelerini ve sonuçlarını görebilir. | G | Denemeler en yeniden eskiye tarih, puan ve geçti/kaldı bilgisiyle listelenir. | P2 |

## H. Dijital sertifika

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-27 | Sistem | Koşulları sağlayan kursiyer için doğrulama kodlu sertifika otomatik oluşturulur. | H | Koşul: kursun tüm zorunlu dersleri tamamlanmış ve tüm zorunlu sınavları geçilmiştir (puan başarı eşiğine eşit ya da üstünde). Koşul sağlandığında sertifika bir kez oluşturulur; bir kursiyere bir kurs için ikinci sertifika oluşmaz. Her sertifikanın tahmin edilemeyen, benzersiz bir doğrulama kodu vardır. | P1 |
| FR-28 | Ziyaretçi | Herkes doğrulama koduyla bir sertifikanın geçerliliğini kontrol edebilir. | H | Geçerli kodla kursiyerin adı, kurs adı ve düzenlenme tarihi gösterilir; geçersiz kodla "bulunamadı" gösterilir. Giriş gerekmez. | P1 |
| FR-29 | Sistem | Sertifikada resmî akreditasyon iddiası bulunmaz. | H | Sertifika sayfasında ve doğrulama sayfasında "Bu belge platform içi katılım belgesidir, resmî akreditasyon değildir" ibaresi görünür. | P1 |

## I. Kurs puanlama ve yorumlar

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-30 | Kursiyer | Kursa kayıtlı kursiyer 1–5 yıldız puan ve yorum bırakabilir. | I | Puan 1 ile 5 arasında tam sayıdır. Kayıtlı olmayan kullanıcının değerlendirmesi reddedilir (403). | P2 |
| FR-31 | Sistem | Bir kursiyer bir kurs için yalnız bir değerlendirme yapabilir. | I | İkinci değerlendirme isteği 409 ile reddedilir; kursiyer mevcut değerlendirmesini düzenleyebilir. | P2 |
| FR-32 | Yönetici | Yönetici uygunsuz yorumları gizleyebilir ve yeniden açabilir. | I, K | Gizlenen yorum herkese açık listede görünmez ve ortalama puana katılmaz. Gizleme gerekçesi kaydedilir. | P2 |

## J. Eğitmen paneli

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-33 | Eğitmen | Eğitmen kendi kurslarını durumlarıyla ve kayıtlı kursiyer sayılarıyla listeler. | J | Liste yalnız eğitmenin kendi kurslarını içerir. Reddedilip Taslak'a dönen kursta red gerekçesi görünür. | P1 |
| FR-34 | Eğitmen | Eğitmen her kursu için tamamlanma oranını ve sınav başarı istatistiklerini görür. | J | Tamamlanma oranı = kursu %100 tamamlayan kursiyer / kayıtlı kursiyer. Her sınav için deneme sayısı ve başarı oranı gösterilir. Değerler veritabanındaki kayıtlarla aynıdır. | P1 |
| FR-35 | Eğitmen | Eğitmen kursundaki kursiyerlerin ilerlemesini izler. | J | Her kursiyer için kayıt tarihi, ilerleme yüzdesi, son erişim ve sınav sonuçları listelenir. Başka eğitmenin kursundaki veriler görüntülenemez (403). | P1 |
| FR-36 | Eğitmen | Eğitmen kurslarının değerlendirme istatistiklerini (ortalama puan, puan dağılımı) görür. | J, I | Değerler FR-16'daki hesapla aynıdır. | P2 |

## K. Yönetici paneli

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-37 | Yönetici | Yönetici incelemedeki kursları inceleyip onaylar ya da gerekçeyle reddeder. | K | Onaylanan kurs "Yayımlandı" olur ve katalogda görünür. Reddedilen kurs "Taslak"a döner; red gerekçesi zorunludur ve kursta saklanır. İncelemede olmayan kursa bu işlem uygulanamaz (409). | P1 |
| FR-38 | Yönetici, Eğitmen | Yönetici (gerekçeyle) ya da kursun eğitmeni yayımlanmış kursu yayından kaldırabilir. | K, B | Yayından kaldırılan kurs katalogda görünmez ve yeni kayıt almaz. | P1 |
| FR-39 | Yönetici | Yönetici kullanıcıları ve eğitmenleri listeleyip hesaplarını pasifleştirebilir/aktifleştirebilir. | K | Liste rol filtresi ve arama ile sayfalı gelir. Pasif kullanıcı giriş yapamaz. Yönetici kendi hesabını pasifleştiremez. | P2 |
| FR-40 | Yönetici | Yönetici kategorileri ekler, düzenler ve siler. | K | Kategori adı benzersizdir. Kursu olan kategori silinemez (409). | P2 |
| FR-41 | Kullanıcı, Yönetici | Kullanıcı bir kursu ya da yorumu gerekçeyle şikâyet edebilir; yönetici şikâyetleri inceler. | K | Şikâyetin durumu (açık, incelendi, reddedildi) yönetici tarafından değiştirilir; gerekirse yorum gizlenir ya da kurs yayından kaldırılır. | P2 |
| FR-42 | Yönetici | Yönetici platform raporlarını görür. | K | Rapor; role göre kullanıcı sayısı, duruma göre kurs sayısı, kayıt, tamamlanma ve sertifika sayıları ile en çok kayıt alan kursları içerir; tarih aralığına göre filtrelenebilir. | P2 |

## L. Bildirimler

| ID | Aktör | Gereksinim | Kaynak | Kabul kriteri | Öncelik |
|---|---|---|---|---|---|
| FR-43 | Sistem | Belirli olaylarda ilgili kullanıcıya uygulama içi bildirim oluşturulur. | L | Olay → alıcı: kursa kayıt → eğitmen; yayımlanmış kursa yeni ders → kayıtlı kursiyerler; kurs onayı → eğitmen; kurs reddi (gerekçesiyle) → eğitmen; sınav sonucu → kursiyer; sertifika oluşturulması → kursiyer. Her olay için yalnız ilgili alıcılara tek bildirim oluşur. | P1 |
| FR-44 | Kursiyer, Eğitmen | Kullanıcı bildirimlerini listeler, okunmamış sayısını görür ve okundu olarak işaretler. | L | Bildirimler en yeniden eskiye listelenir. Okundu işaretlenen bildirim okunmamış sayısından düşer. Kullanıcı başkasının bildirimini göremez. | P1 |

## Kapsam dışı

- **Gerçek ödeme entegrasyonu:** kurs kayıtları ücretsizdir (proje tanım dokümanı, bölüm 2).
- **Bağımsız mobil uygulama:** ilk sürümde mobil uyumlu web arayüzü yeterlidir (proje tanım dokümanı, bölüm 2).

## Temel senaryolar → gereksinimler

| Senaryo (proje tanım dokümanı, bölüm 4) | Gereksinimler |
|---|---|
| 1. Kurs yayımlama | FR-07, FR-08, FR-09, FR-11, FR-23, FR-24, FR-37 |
| 2. Kursa kayıt ve öğrenme | FR-13, FR-15, FR-17, FR-12, FR-06, FR-20, FR-21 |
| 3. Kaldığı yerden devam | FR-19, FR-22 |
| 4. Sınav ve sertifika | FR-25, FR-27, FR-28, FR-29 |
| 5. Eğitmen analitiği | FR-33, FR-34, FR-35 |

## Bölüm 6 test durumları → gereksinimler

| Test durumu (proje tanım dokümanı, bölüm 6) | Gereksinim |
|---|---|
| Kursa kayıt olmadan özel derslere erişim | FR-06 |
| Mükerrer kayıt | FR-18 |
| Yetkisiz kurs düzenleme | FR-10 |
| Yanlış ilerleme hesaplaması | FR-21 |
