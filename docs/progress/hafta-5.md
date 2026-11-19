# İlerleme raporu — Hafta 5 (18.11–24.11.2026)

> **TASLAK.** Hafta 24.11'de bitiyor; bu taslak 19.11.2026 18:05 (İstanbul saati) verisidir. Hafta sonunda sayılar güncellenip "hafta sonu" sütunu eklenecek.

**Milestone:** Hafta 5: Bug fix, test, seed data, rapor, demo (bitiş 24.11.2026)
**Kaynak:** GitHub API (milestone, issue, PR ve yorumları, CI çalışmaları) ve `git log origin/main`. Tamamlanan iş, kapanma nedeni "completed" olan issue'lardır.

## 1. Özet (19.11 itibarıyla)

| | Sayı |
|---|---|
| Planlanan issue (Hafta 5 milestone'u) | 19 |
| Tamamlanan (Hafta 5, 18.11'den beri) | 6 (#242, #56, #249, #256, #67, #64) |
| Kesilen (not planned) | 1 (#73, 17.11 freeze kararı) |
| Açık (Hafta 5) | 11 |
| Hafta içinde tamamlanan issue (tüm milestone'lar) | 52 (Hafta 4: 17, Hafta 2: 15, Hafta 3: 13, Hafta 5: 6, Hafta 1: 1) |
| Önceki haftalardan devreden açık issue | Hafta 1: 0, Hafta 2: 3, Hafta 3: 8 |
| Merge edilen PR (hafta içinde) | 49 |
| Merge edilen PR (toplam, ekip) | 120 |
| CI'da çalışan test (`main`, `ee2012b`) | 893 birim + 13 mimari + 147 entegrasyon = 1053, 0 hata |
| `main`'de CI | 49 çalışma: 34 başarılı, 15 iptal (yeni push), başarısız yok |

## 2. Tamamlanan önemli işler (18–19.11)

- **Özellikler (feature freeze kapsamında kalanlar):** bölüm ve ders CRUD (#24, #25), içerik yükleme ve sunma (#112, #113), ders oynatıcı ve içerik token'ı (#45, ADR-012), kaldığı yerden devam (#119), sertifika (#129), bildirim modülü (#124), yayından kaldırma (#123), kurs düzenleme/silme (#23), eğitmen analitiği (#133–#135), platform raporu (#145, #147), sınav çözme ekranları (#57, #50).
- **Test:** uçtan uca testler Senaryo 1–5 (#65, #138 kısmi), Bölüm 6 kritik durumları ve rol × uç erişim matrisi (#139), servis test issue'larının kırmızı kontrolü (#31, #32, #44, #56, #61, #114, #128, #132; #137 kısmi), seed verisi (#67).
- **Doküman:** üç sekans diyagramı (#39, #64, #140), demo senaryosu (#70, Docker denemesi kaldı), Hafta 4 raporu (#96), test raporu taslağı (#99).

## 3. Açık issue'lar (Hafta 5 milestone'u)

| Rol | Issue'lar |
|---|---|
| kalite | #68 (bug bash), #70 (demo: Docker denemesi), #72 (README + ekran görüntüleri), #97 (diyagram–kod: 9 diyagramın 7'si kontrol edildi), #99 (test raporu: taslak), #101 (bu rapor) |
| mimar | #71, #100, #230 |
| veri | #257 |
| alan | #95 (SOLID dokümanı) |

## 4. Kişi bazında merge edilen PR'lar (18.11–19.11)

| Kişi | PR açan (merge edilen) | Merge eden |
|---|---|---|
| Bertan | 16 | 19 |
| Veronika | 14 | 29 |
| Eray | 11 | – |
| Sedat | 8 | 1 |
| **Toplam** | **49** | **49** |

Sedat bu hafta 8 PR açtı (önceki haftalarda toplam 4); alan rolünün açık işleri 19'dan 11'e indi.

### Hızlı kontrol

Başkalarının açtığı ve hafta içinde merge edilen PR'ların hepsinde Kalite ve Süreç sorumlusunun kontrol yorumu var. Merge'den önce kontrol edilmeyen 7 PR (#228, #231, #232, #234–#237, 18.11 sabahı, Bertan) geriye dönük kontrol edildi; engelleyici sorun çıkmadı. Ardından Bertan'ın karar ilkelerine "PR'da 'Kontrol edildi ✓' yoksa merge etme" şartı eklendi (#255, R6). 19.11 öğleden sonra Bertan müsait olmadığı için, onun talimatıyla Veronika'nın 8 PR'ı her biri güncel `main` ile yeniden CI'dan geçirilerek merge edildi (PR yorumlarında not var).

## 5. CI ve testler

- Test sayısı: 10.11'de 1 → 17.11 sonunda 404 → 19.11'de 1053.
- Entegrasyon testleri (Testcontainers, PostgreSQL 17) 18.11'de başladı; bugün 16 sınıf, 147 test.
- Kırmızı kontrollerde 3 test boşluğu bulunup kapatıldı (#56, #44, #145) (ayrıntı: test-report.md › 5).
- İki kez GitHub tarafında geçici Maven indirme hatası (wget) CI'ı testlere ulaşmadan durdurdu; yeniden çalıştırınca geçti.

## 6. Karşılaşılan sorunlar

- **Çakışan migration ve PR'lar:** aynı dosyalara dokunan PR'lar (#225, #233, #245) art arda rebase gerektirdi; aynı anda gelen iki migration'da (#262, #263) zaman damgası sırası gözetilerek merge edildi.
- **Ekranı eksik akışlar:** kurs oluşturma formu (#33), yönetici onay ekranı (#62), sertifika görüntüleme/doğrulama (#130), bildirim listesi (#125) henüz yok; demo bu adımlarda API kullanıyor.
- **Docker'sız üye:** Veronika'nın makinesinde Docker/Java/Node yok; tarayıcı denemeleri ve bug bash için Docker'lı bir üyeye ihtiyaç var.

## 7. Kalan haftanın (20–24.11) riski

- **Bug bash (#68) ve demo provası (#106):** seed verisi hazır; çalıştırma Docker'lı üyelere bağlı. Bug bash'te çıkacak P1 hatalar demo öncesi kapanmalı (test-plan › 7.6).
- **Eksik uçlar:** #125 (bildirim listesi), #130 (sertifika doğrulama) gelmezse Senaryo 4'ün son adımı demoda API ile ya da hiç gösterilemez; #137 ve #138 de bunlara bağlı.
- **Diyagramlar:** sınıf (#51) ve bileşen (#91) diyagramları değerlendirmede ağırlıklı; #97 bunları bekliyor.
- **Rapor:** proje raporu (#69, #102–#104) test raporunu (#99) ve diyagramları bekliyor.
