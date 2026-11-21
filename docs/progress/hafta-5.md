# İlerleme raporu — Hafta 5 (18.11–24.11.2026)

> **TASLAK.** Hafta 24.11'de bitiyor; bu taslak 21.11.2026 akşamı verisidir (ilk sürüm 19.11 18:05). Hafta sonunda sayılar güncellenip bug bash (#68) sonucu eklenecek.

**Milestone:** Hafta 5: Bug fix, test, seed data, rapor, demo (bitiş 24.11.2026)
**Kaynak:** GitHub API (milestone, issue, PR ve yorumları, CI çalışmaları) ve `git log origin/main`. Tamamlanan iş, kapanma nedeni "completed" olan issue'lardır.

## 1. Özet (21.11 itibarıyla)

| | Sayı |
|---|---|
| Planlanan issue (Hafta 5 milestone'u) | 19 (1'i kesildi: #73) |
| Tamamlanan (Hafta 5 milestone'u) | 12: #56, #64, #67, #70, #71, #95, #98, #100, #230, #242, #249, #256 |
| Açık (Hafta 5) | 6: #68 bug bash, #72 README, #97 diyagram–kod (PR #316), #99 test raporu, #101 bu rapor, #257 |
| Hafta içinde tamamlanan issue (tüm milestone'lar) | 82 (Hafta 4: 25, Hafta 3: 21, Hafta 2: 18, Hafta 5: 11, milestone'suz: 6, Hafta 1: 1) |
| Önceki haftalardan devreden açık issue | Hafta 1–3: 0, Hafta 4: 1 (#66) |
| Projenin tamamı | 154 issue'dan 139'u kapandı (%90); 6 issue kesildi (not planned) |
| Merge edilen PR (hafta içinde) | 87 |
| Merge edilen PR (toplam, ekip) | 158 |
| CI'da çalışan test (`main`, `a27bcf7`) | 993 birim + 13 mimari + 182 entegrasyon = 1188, 0 hata |
| `main`'de CI (hafta içinde) | 87 çalışma: 70 başarılı, 16 iptal (yeni push), 1 başarısız (GitHub tarafında geçici Maven indirme hatası; testlere ulaşmadan) |

## 2. Tamamlanan önemli işler (18–21.11)

- **Özellikler (feature freeze kapsamında kalanlar):** bölüm ve ders CRUD (#24, #25), içerik yükleme ve sunma (#112, #113), ders oynatıcı ve içerik token'ı (#45, ADR-012), kaldığı yerden devam (#119, #120), sertifika (#129) ile görüntüleme ve herkese açık doğrulama (#130, #131), bildirim modülü (#124), bildirim API'si (#125) ve zil (#136), yayından kaldırma (#123), kurs düzenleme/silme (#23), eğitmen analitiği (#133–#135), platform raporu (#145, #147), sınav çözme ekranları (#57, #50, #58, #304), yönetici kurs onayı (#62), eğitmen kurs formu (#33), bölüm/ders yönetimi ekranı (#34), Tamamlandı düğmesi ve ilerleme çubukları (#46).
- **P2 işler ("İnce" kapsam):** parola yenileme ve profil (#115–#117), yorum moderasyonu (#142), şikâyetler (#144, #146), mobil uyum ve ortak durum bileşenleri (#71).
- **Test:** uçtan uca testler Senaryo 1–5 (#65, #138), Bölüm 6 kritik durumları ve rol × uç erişim matrisi (#139), bildirim dinleyici testleri (#137), servis test issue'larının kırmızı kontrolü (#31, #32, #44, #56, #61, #114, #128, #132), seed verisi ve örnek bildirimler (#67).
- **Doküman:** sınıf diyagramı (#51), bileşen diyagramı ve CI'da kodla karşılaştırılması (#91), SOLID dokümanı (#95), kütüphaneler listesi (#100), üç sekans diyagramı (#39, #64, #140), demo senaryosu ve temiz kurulumda yürünmesi (#70), izlenebilirlik matrisi güncellemeleri (#98: otomatik testi olan FR 6 → 43/44), rapor bölüm 1–3 (#102, taslak) ve 5–8 (#104, taslak), sunum soru havuzu (#107).

## 3. Açık issue'lar (21.11)

| Rol | Issue'lar |
|---|---|
| kalite | #68 (bug bash, 24.11'den sonra), #72 (README: özellik listesi; ekran görüntüleri Docker'lı üyede), #97 (diyagram–kod: 9 diyagramın 9'u kontrol edildi; PR #316 PNG/SVG render bekliyor), #99 (test raporu: taslak, bug bash bekliyor), #101 (bu rapor) |
| veri | #257, #66 (OpenAPI eşitleme: springdoc kararı bekleniyor) |
| milestone'suz | #305 (katalogda puan filtresi) |
| Sunum milestone'u | #69, #102–#107 (rapor, slaytlar, demo provası, soru–cevap provası) |

## 4. Kişi bazında merge edilen PR'lar (18.11–21.11)

| Kişi | PR açan (merge edilen) | Merge eden |
|---|---|---|
| Bertan | 30 | 38 |
| Sedat | 24 | 1 |
| Veronika | 22 | 48 |
| Eray | 11 | – |
| **Toplam** | **87** | **87** |

Sedat bu hafta 24 PR açtı (önceki haftalarda toplam 4); alan rolündeki 33 işin 31'i kapandı. 21.11'de demo yürüyüşünde açılan sınav ekranı işleri (#304, #58, #296) Eray'dan Bertan'a devredildi.

### Hızlı kontrol

- 18.11 sabahı merge'den önce kontrol edilmeyen 7 PR (#228, #231, #232, #234–#237) geriye dönük kontrol edildi; engelleyici sorun çıkmadı. Ardından Bertan'ın karar ilkelerine "PR'da 'Kontrol edildi ✓' yoksa merge etme" şartı eklendi (#255, R6).
- **21.11'de iki dalgada 19 PR** (#294, #297–#302, #307–#310 sabah; #295, #320–#326 akşam) hızlı kontrol beklenmeden merge edildi. Hepsi merge sonrası kontrol edildi ve yorumlandı: 16'sı temizdi. 3'ünde eksik vardı ve PR yorumlarına yazıldı:
  - #297: sınıf diyagramında yeni servis yok;
  - #309: kanıt tablosu yok, tarayıcı denemesi yok, `demo.md` eski;
  - #310: kanıt tablosu yok, tarayıcı denemesi yok.
- Merge sürecinde bir hata (21.11, Veronika): #315, `main` ile güncellendikten hemen sonra, yeni commit'in CI'ı bitmeden merge edildi. Merge sonrası `main` CI'ı yeşil geçti, PR yorumu düzeltildi. O günden beri merge, güncellenen commit'in CI'ı bitince ve yalnız o commit'e kilitlenerek (`--match-head-commit`) yapılıyor.

## 5. CI ve testler

- Test sayısı: 10.11'de 1 → 17.11 sonunda 404 → 19.11'de 1053 → 21.11'de 1188.
- Entegrasyon testleri (Testcontainers, PostgreSQL 17) 18.11'de başladı; bugün 21 sınıf, 182 test (122'si erişim matrisi).
- Kırmızı kontrollerde 4 test boşluğu bulunup kapatıldı: #56, #44, #145 ve #137 (dinleyici `@EventListener` yapılınca mevcut 1054 testin hepsi yeşil kalıyordu). Ayrıntı: `test-report.md` › 5.
- Diğer üyeler de kendi PR'larında kırmızı kontrol yaptı ve PR açıklamasına yazdı (#303, #315, #322, #326).
- Bileşen diyagramı artık CI'da kodla karşılaştırılıyor (`ComponentDiagramTest`, #91); ER diyagramı ile migration'lar 20/20 aynı (#97).

## 6. Karşılaşılan sorunlar

- **Çakışan migration ve PR'lar:** aynı dosyalara dokunan PR'lar (#225, #233, #245; 21.11'de `globals.css`'e dokunan #288, #289, #300, #309) art arda rebase gerektirdi. Aynı anda gelen iki migration'da (#262, #263) zaman damgası sırası gözetilerek merge edildi.
- **Eksik ekranlar (19.11'deki liste) 20–21.11'de tamamlandı:** kurs formu (#33), yönetici onayı (#62), sertifika sayfası ve doğrulama (#130, #131), bildirim zili (#136). Ekranı olmayan yalnız katalogda puan filtresi (#305) ve kullanıcının şikâyet oluşturması kaldı.
- **Kontrolsüz merge'ler:** bkz. 4 › Hızlı kontrol.
- **Docker'sız üye:** Veronika'nın makinesinde Docker/Java/Node yok. Tarayıcı denemeleri ve bug bash için Docker'lı bir üye gerekiyor; diyagram görüntüleri de (PlantUML) diğer üyelerde üretiliyor.

## 7. Kalan haftanın (22–24.11) riski

- **Bug bash (#68):** 24.11 kod dondurmasından sonra (karar: Bertan). Kontrol listesi `test-plan.md` › 6'da. Çıkan P1 hatalar demo öncesi kapanmalı (test-plan › 7.6).
- **İlk build süresi:** temiz makinede `docker compose up --build` ~22 dk; sunum günü image'lar önceden build edilmeli (#70, #106).
- **Açık küçük işler:** #66 (springdoc kararı), #305 (puan filtresi), #97'nin PNG/SVG'leri.
- **Rapor:** bölüm 1–3 (#102) ve 5–8 (#104) taslak; bölüm 4 (#103) başlamadı. Word birleştirme (#69) 02.12 teslimden önce.
