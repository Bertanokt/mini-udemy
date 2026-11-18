# İlerleme raporu — Hafta 4 (11.11–17.11.2026)

**Milestone:** Hafta 4: Sınav çözme/puanlama, admin, entegrasyon (feature freeze sonu, bitiş 17.11.2026)
**Rapor tarihi:** 18.11.2026
**Veri tarihi:** 18.11.2026 17:10 (İstanbul saati). Kaynak: GitHub API (milestone, issue, PR ve yorumları, CI çalışmaları) ve `git log origin/main`.

Hafta sonundaki durum (17.11) ile bugünkü durum (18.11) ayrı sütunlarda verilmiştir. Board sütun geçmişi kayıtlı olmadığından planlanan iş listesi milestone'dan alınmıştır. Tamamlanan iş, issue'nun kapanma nedeni "completed" olanlardır; "not planned" kapananlar sayılmaz (plan.md › 6.1).

## 1. Özet

| | 17.11.2026 (hafta sonu) | 18.11.2026 (bugün) |
|---|---|---|
| Planlanan issue (Hafta 4 milestone'u) | 33 | 33 |
| Tamamlanan issue (Hafta 4) | 5 | 9 |
| Kesilen issue (not planned, Hafta 4) | 1 (#185) | 1 |
| Devreden (açık) issue (Hafta 4) | 27 | 23 |
| Hafta 1'den devreden açık issue | 1 | 0 |
| Hafta 2'den devreden açık issue | 18 | 8 |
| Hafta 3'ten devreden açık issue | 21 | 15 |
| Hafta içinde tamamlanan issue (tüm milestone'lar) | 57 | – |
| Merge edilen PR (hafta içinde) | 71 | – |
| Merge edilen PR (toplam, ekip) | 71 | 91 |
| CI'da çalışan test (`main`) | 404, 0 hata | 672 birim + 2 entegrasyon, 0 hata |
| `main`'de son CI çalışması | yeşil (17.11 22:15) | yeşil (18.11) |

Hafta 3'te `main`'e hiç değişiklik girmemişti; Hafta 4'te ekip 71 PR merge etti ve 57 issue kapattı. Bunların çoğu önceki haftaların işiydi (Hafta 1: 25, Hafta 2: 14, Hafta 3: 12); Hafta 4'ün kendi 33 işinden 5'i hafta içinde bitti.

## 2. Tamamlanan issue'lar (Hafta 4 milestone'u)

| Issue | Başlık | Kapanış | Rol |
|---|---|---|---|
| #53 | Flyway migration: quiz_attempts, attempt_answers | 15.11 | veri |
| #52 | Desen: Strategy — ScoringStrategy ve uygulamaları | 17.11 | alan |
| #55 | GET /api/me/quizzes/{id}/attempts | 17.11 | veri |
| #60 | Admin: kullanıcı / eğitmen yönetimi | 17.11 | veri |
| #63 | FE: Admin kullanıcı yönetimi | 17.11 | veri |

Hafta bittikten sonra (18.11) kapananlar: #54 (sınav çözme ve puanlama), #57 (FE sınav çözme ekranı), #94 (UML dağıtım diyagramı), #143 (katalogda puan filtresi).

Kesilen: #185 (kurs süresinde N+1 sorgu; 17.11 freeze kararı, raporda "bilinen performans sınırı" olarak geçecek).

## 3. Devreden issue'lar (17.11.2026 sonunda açık, Hafta 4 milestone'u)

| Rol | Issue'lar |
|---|---|
| alan | #54, #57, #58, #129, #140, #142, #144, #146, #184 |
| kalite | #65, #66, #96, #132, #137, #138, #139, #145, #147 |
| mimar | #94, #130, #131, #136 |
| veri | #133, #134, #135, #141, #143 |

27 issue. 18.11'de bunlardan #54, #57, #94 ve #143 kapandı. #65 (uçtan uca testler) üç parça hâlinde ilerliyor: altyapı ve Senaryo 2 merge edildi (#239), Senaryo 1 (#246) ve Senaryo 2'nin içerik adımı (#243) incelemede, Senaryo 3 kaldığı yerden devam ucunu (#119, PR #245) bekliyor.

Önceki haftalardan devreden açık iş 17.11 sonunda 40'tı (1 + 18 + 21); 18.11'de 23'e indi (0 + 8 + 15). Tüm projede bugün 67 açık issue var; 57'sinin sahibi yok.

## 4. Kişi bazında merge edilen PR'lar (11.11–17.11)

| Kişi | PR açan (merge edilen) | Merge eden |
|---|---|---|
| Bertan | 28 | 38 |
| Eray | 28 | – |
| Veronika | 12 | 33 |
| Sedat | 3 | – |
| **Toplam** | **71** | **71** |

Veronika'nın PR'ları: #156 (NFR dokümanı), #157 ve #189 (katkı notları), #174 (test planı), #175 (JaCoCo raporu), #176, #177, #187 (Hafta 1–3 ilerleme raporları), #188 (proje planı), #190 (izlenebilirlik matrisi), #192 (risk tablosu ve freeze kararı), #206 (`POST /api/courses`, #22).

### Hızlı kontrol (Kalite ve Süreç)

Hafta içinde başkalarının açtığı 59 PR merge edildi. 28'inde Kalite ve Süreç sorumlusunun hızlı kontrolü (CI, `Closes #` ve kanıt tablosu, Ne/Neden–kod uyumu, doküman) merge'den önce yapıldı. 11–16.11 arasında kontrol ara sıra yapıldı (#164, #172, #186); diğer PR'lar (çoğu doküman, migration ve altyapı) kontrolsüz merge edildi. #191'den itibaren kontrol her PR'a uygulanıyor. Süreç başladıktan sonra kontrolsüz merge edilen 3 PR (#196, #197, #198) geriye dönük kontrol edildi; bu kontrolde `?next=` yönlendirmesinde açık yönlendirme (open redirect) açığı bulundu ve #199 ile düzeltildi.

## 5. CI ve testler

- Hafta içinde `main`'de 71 CI çalışması oldu: 48 başarılı, 23 iptal (aynı dal için yeni push gelince eski çalışma iptal ediliyor, `cancel-in-progress`), başarısız yok.
- CI'da çalışan test sayısı: 10.11'de 1, 17.11 sonunda 404 (53 test sınıfı), 18.11'de 672 birim + 2 entegrasyon.
- Mimari kurallar ArchUnit ile CI'da denetleniyor (#86, 18.11'de merge edildi).
- Entegrasyon testi altyapısı (Testcontainers, PostgreSQL 17, failsafe, `*IT`) 18.11'de `main`'e girdi (#239); ilk test Senaryo 2'yi (arama → kayıt → ders tamamlama → ilerleme) gerçek veritabanıyla sınıyor.
- JaCoCo coverage raporu her CI çalışmasında `jacoco-report` artifact'ı olarak üretiliyor (#89).

## 6. Karşılaşılan sorunlar

- **Takvim kayması (R1):** Hafta 4'ün 33 işinden yalnız 5'i hafta içinde bitti; ekip haftayı büyük ölçüde Hafta 1–3'ün birikmiş işlerine harcadı.
- **Kontrolsüz merge (R6):** Süreç başladıktan sonra da üç PR hızlı kontrol beklenmeden merge edildi; birinde güvenlik açığı çıktı (#199). Aynı durum 18.11'de de sürdü (#224–#237 arasında 9 PR ve #225); hepsine geriye dönük kontrol yorumu yazıldı, engelleyici sorun bulunmadı.
- **Sahipsiz işler (R4):** Freeze kararından sonra 111 açık issue'nun 109'u sahipsizdi; bugün 67 açık issue'nun 57'si sahipsiz.
- **Çakışan PR'lar:** Aynı dosyalara dokunan PR'lar (ör. #225, #233, #245) birbiri ardına merge edildikçe tekrar tekrar rebase gerekti; bu, merge'leri geciktirdi.

## 7. Gelecek haftanın (Hafta 5, 18–24.11) riski

- **Freeze sonrası kapsam:** Plan gereği yalnız bug fix ve test yapılmalı; ancak Hafta 2–4'ten 46 açık issue (8 + 15 + 23) var ve bunların bir kısmı özellik işi. 17.11 kararındaki "İnce" kapsamlara (plan.md › 6.1) uyulması gerekiyor.
- **Entegrasyon testleri:** #65'in Senaryo 3'ü #119'un (PR #245) merge edilmesini, #138 (Senaryo 4–5: sertifika ve eğitmen istatistikleri) #65, #129 ve #133'ü bekliyor. Bu işler gecikirse test raporu (#99) da gecikir.
- **Alan rolü (R10):** Açık işlerin en çoğu hâlâ `rol:alan`'da; dağıtım kararı bekliyor.
- **Demo ortamı (R8):** Seed data (#67) ve demo provası (#106) Hafta 5 ve Sunum milestone'larında; tek komutla kurulumun (`docker compose up --build`) uçtan uca denenmesi gerekiyor.
