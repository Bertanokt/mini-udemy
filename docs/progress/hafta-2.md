# İlerleme raporu — Hafta 2 (28.10–03.11.2026)

**Milestone:** Hafta 2: Kurs yönetimi + kayıt (bitiş 03.11.2026)
**Rapor tarihi:** 14.11.2026
**Veri tarihi:** 14.11.2026 14:38 (İstanbul saati). Kaynak: GitHub API (milestone, issue, PR, CI çalışmaları) ve `git log origin/main`.

Bu rapor hafta bittikten sonra yazıldı. Hafta sonundaki durum (03.11) ile bugünkü durum (14.11) ayrı sütunlarda verilmiştir; geriye dönük tarih atılmamıştır. Board sütun geçmişi kayıtlı olmadığından planlanan iş listesi milestone'dan alınmıştır.

## 1. Özet

| | 03.11.2026 (hafta sonu) | 14.11.2026 (bugün) |
|---|---|---|
| Planlanan issue (Hafta 2 milestone'u) | 32 | 32 |
| Tamamlanan (kapanan) issue | 0 | 0 |
| Devreden (açık) issue | 32 | 32 |
| Hafta 1'den devreden açık issue | 27 | 14 |
| Merge edilen PR | 0 | 0 (Hafta 2 issue'larını kapatan) |
| `main`'deki test metodu (`@Test`) | 1 | 23 |
| `main`'de CI çalışması | yok (hafta içinde `main`'e commit girmedi) | son çalışma yeşil (13.11) |

## 2. Tamamlanan issue'lar

Hafta 2 milestone'unda henüz kapanan issue yok.

## 3. Devreden issue'lar (14.11.2026 itibarıyla, hepsi açık)

Üzerinde açık PR olanlar:

| Issue | Başlık | Atanan | Açık PR |
|---|---|---|---|
| #29 | Flyway migration: enrollments | Eray | #167 |
| #35 | FE: Kurs kataloğu + filtreler | Eray | #171 |
| #84 | ADR-004 yerel dosya depolama · ADR-005 ayrı frontend + REST + önce OpenAPI | Bertan | #164 |

Rol bazında diğer açık issue'lar:

| Rol | Issue'lar |
|---|---|
| mimar | #16, #18, #19 (FE auth, giriş, route koruma), #23 (kurs düzenle/sil), #81 (ADR-002/003), #86 (ArchUnit), #111 (dosya depolama), #113 (yetki kontrollü içerik servisi) |
| alan | #22 (kurs oluşturma), #27 (onaya gönder), #33 (FE kurs formu), #39 (sekans: kursa kayıt), #42 (Observer), #51 (sınıf diyagramı), #59 (admin onay/red), #85 (State), #110 (Factory Method) |
| veri | #25 (ders CRUD), #30 (kursa kayıt API'si), #34 (FE bölüm/ders yönetimi), #36 (FE kurs detayı), #37 (FE kayıtlı kurslar), #112 (içerik yükleme API'si), #118 (kategori yönetimi, P2) |
| kalite | #24 (bölüm CRUD), #31 (CourseService + State testleri), #32 (EnrollmentService testleri), #90 (bu rapor), #114 (içerik ve depolama testleri) |

Hepsi P1'dir; yalnız #118 P2.

## 4. Kişi bazında merge edilen PR'lar

| Kişi | 28.10–03.11 arası | Hafta 2 issue'larına bağlı açık PR (14.11) |
|---|---|---|
| Bertan | 0 | #164 |
| Eray | 0 | #167, #171 |
| Sedat | 0 | – |
| Veronika | 0 | – |

Hafta içinde `main`'e commit ya da PR girmedi. Ekibin genel PR durumu Hafta 1 raporunda (bölüm 4) verilmiştir.

## 5. CI ve testler

- Hafta içinde `main`'de CI çalışması olmadı (`main`'e push yok). Son çalışma 23.10'da yeşildi.
- 03.11'de `main`'de 1 test metodu vardı (`HealthControllerTest`). 14.11'de 23 test metodu var, hepsi `common` modülünde. Hafta 2'nin test issue'ları (#31, #32, #114) kurs, kayıt ve içerik kodunu bekliyor.

## 6. Karşılaşılan sorunlar

- Hafta 2 başladığında Hafta 1'in 27 issue'sunun hiçbiri kapanmamıştı; Hafta 2'nin kurs ve kayıt işleri auth ve entity işlerine (#8, #10, #21) bağlı olduğu için başlayamadı.
- Hafta 2'de 32 issue'dan yalnız 3'ü birine atanmış durumda (#29, #35, #84); diğerlerinin sahibi yok.

## 7. Gelecek haftanın riski

- Feature freeze 17.11.2026. Hafta 2'nin 32 ve Hafta 1'in 14 açık issue'su ile Hafta 3–4 işleri aynı döneme sıkışıyor; planlama toplantısında P2'lerin kesilmesi ve P1'lerin sahiplendirilmesi gerekiyor.
- Bölüm 6 kritik durumlarından ikisi (mükerrer kayıt, yetkisiz kurs düzenleme) bu milestone'daki kodla (#23, #30) ilgili; bu kod gecikirse #139'daki kritik testler de gecikir.
