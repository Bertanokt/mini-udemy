# İlerleme raporu — Hafta 3 (04.11–10.11.2026)

**Milestone:** Hafta 3: Ders izleme, ilerleme, sınav oluşturma (bitiş 10.11.2026)
**Rapor tarihi:** 16.11.2026
**Veri tarihi:** 16.11.2026 10:52 (İstanbul saati). Kaynak: GitHub API (milestone, issue, PR, CI çalışmaları) ve `git log origin/main`.

Bu rapor hafta bittikten sonra yazıldı. Hafta sonundaki durum (10.11) ile bugünkü durum (16.11) ayrı sütunlarda verilmiştir; geriye dönük tarih atılmamıştır. Board sütun geçmişi kayıtlı olmadığından planlanan iş listesi milestone'dan alınmıştır.

## 1. Özet

| | 10.11.2026 (hafta sonu) | 16.11.2026 (bugün) |
|---|---|---|
| Planlanan issue (Hafta 3 milestone'u) | 33 | 33 |
| Tamamlanan (kapanan) issue | 0 | 4 |
| Devreden (açık) issue | 33 | 29 |
| Hafta 1'den devreden açık issue | 27 | 9 |
| Hafta 2'den devreden açık issue | 32 | 27 |
| Merge edilen PR (hafta içinde) | 0 | – |
| Merge edilen PR (toplam, ekip) | 0 | 36 |
| `main`'deki test metodu (`@Test`) | 1 | 57 |
| `main`'de CI çalışması | yok (hafta içinde `main`'e commit girmedi) | son çalışma yeşil (15.11) |

## 2. Tamamlanan issue'lar

Hafta içinde (04.11–10.11) kapanan issue yok. Hafta bittikten sonra kapananlar:

| Issue | Başlık | Kapanış | PR | Kişi |
|---|---|---|---|---|
| #89 | JaCoCo coverage raporu CI artifact'ı olarak | 14.11 | #175 | Veronika |
| #92 | Test planı | 14.11 | #174 | Veronika |
| #40 | Flyway migration: lesson_progress, course_progress | 15.11 | #168 | Eray |
| #47 | Flyway migration: soru bankası ve sınavlar | 15.11 | #169 | Eray |

## 3. Devreden issue'lar (16.11.2026 itibarıyla, hepsi açık)

Açık 29 issue'nun hiçbiri birine atanmamış ve hiçbirinde açık PR yok.

| Rol | Issue'lar |
|---|---|
| mimar | #17 (FE kayıt sayfası), #41 (ders tamamlama API'si), #45 (FE kurs oynatıcısı), #87 (C4 diyagramı), #91 (bileşen diyagramı), #115 (parola yenileme, P2), #116 (profil, P2), #117 (FE profil + parola, P2), #125 (bildirim API'si) |
| alan | #43 (bölüm ve kurs ilerleme yüzdesi), #46 (FE tamamlandı butonu + ilerleme çubukları), #48 (bölüm sonu sınavı), #50 (FE sınav oluşturma), #62 (FE admin onay/red), #119 (kaldığı yerden devam API'si), #120 (FE kaldığı yerden devam), #123 (yayından kaldırma), #124 (bildirim modülü) |
| veri | #28 (eğitmenin kursları API'si), #38 (FE eğitmen paneli), #49 (öğrenci sınav görünümü), #121 (soru bankası CRUD), #122 (FE soru bankası), #126 (puan ve yorum, P2), #127 (FE yorumlar, P2) |
| kalite | #44 (ProgressService testleri), #61 (admin + kurs durum geçişi testleri), #93 (bu rapor), #128 (ReviewService testleri, P2) |

23'ü P1, 6'sı P2 (#115, #116, #117, #126, #127, #128).

## 4. Kişi bazında merge edilen PR'lar

| Kişi | 04.11–10.11 arası | 16.11'e kadar toplam |
|---|---|---|
| Bertan | 0 | 15 |
| Eray | 0 | 12 |
| Veronika | 0 | 6 |
| Sedat | 0 | 3 |

Hafta içinde `main`'e commit ya da PR girmedi. Ekipteki ilk PR merge'i 11.11'de oldu; 16.11'e kadarki 36 PR'ın hepsi 11–15.11 arasında merge edildi.

## 5. CI ve testler

- Hafta içinde `main`'de CI çalışması olmadı (`main`'e push yok). Bir önceki çalışma 23.10'da yeşildi.
- 10.11'de `main`'de 1 test metodu vardı (`HealthControllerTest`). 16.11'de 57 test metodu var: `common` modülünde 47 (hata yönetimi, güvenlik, JWT), `auth`'ta 3 (`UserMapperTest`), `course`'ta 7 (`CourseTest`, `CourseEnumsTest`).
- JaCoCo coverage raporu (#89) 14.11'den beri her CI çalışmasında `jacoco-report` artifact'ı olarak üretiliyor.
- Hafta 3'ün test issue'ları (#44, #61) ilerleme ve kurs durum kodunu (#43, #59, #119, #123) bekliyor.

## 6. Karşılaşılan sorunlar

- Hafta 3 boyunca ekipten `main`'e hiç değişiklik girmedi; Hafta 1 ve Hafta 2'nin 59 issue'su hafta sonunda hâlâ açıktı.
- Hafta 3'ün işleri önceki haftaların işlerine bağlı: ders tamamlama ve kaldığı yerden devam (#41, #119) kursa kayıt API'sini (#30, Hafta 2) ve ilerleme migration'ını (#40) bekliyor; soru bankası (#121) sınav migration'ını (#47) bekliyor. #40 ve #47 15.11'de kapandı, #30 hâlâ açık.
- Açık 29 issue'nun hiçbirinin sahibi yok.

## 7. Gelecek haftanın riski

- **Feature freeze 17.11.2026.** Hafta 1–3'ten 65 açık issue (9 + 27 + 29) ve Hafta 4'ün işleri aynı döneme sıkışıyor. P1'lerin sahiplendirilmesi ve hangi işlerin freeze'den sonra kalacağının netleşmesi gerekiyor.
- **P2 kesme kuralı uygulanmadı.** CONTRIBUTING'e göre 11.11 planlamasında Hafta 3'ün P1'leri bitmemişse Hafta 4'ün P2'leri "not planned" olarak kapatılır. Hafta 3'ün P1'leri bitmedi; Hafta 4'ün 10 P2'si (#55, #60, #63, #141–#147) hâlâ açık.
- Bölüm 6 kritik test durumlarından ilerleme yüzdesi (20 dersin 15'i → %75) bu milestone'daki koda (#43) bağlı; bu kod gecikirse #44 ve #139 da gecikir.
