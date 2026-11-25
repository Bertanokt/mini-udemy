# İlerleme raporu — Hafta 5 (18.11–24.11.2026)

> Son sürüm: 25.11.2026, **24.11 sonu** (23:59) verisiyle. Ara sürümler 19.11 ve 21.11. Bug bash (#68) hafta bittikten sonra, 26–27.11'de; sonucu `test-report.md` › 6'ya ve bu rapora ek olarak (bölüm 8) yazılacak.

**Milestone:** Hafta 5: Bug fix, test, seed data, rapor, demo (bitiş 24.11.2026)
**Kaynak:** GitHub API (milestone, issue, PR ve yorumları, CI çalışmaları) ve `git log origin/main`. Tamamlanan iş, kapanma nedeni "completed" olan issue'lardır. Tarihler Türkiye saatiyle.

## 1. Özet (24.11 sonu)

| | Sayı |
|---|---|
| Planlanan issue (Hafta 5 milestone'u) | 19 (1'i kesildi: #73) |
| Tamamlanan (Hafta 5 milestone'u) | 14: #56, #64, #67, #70, #71, #95, #97, #98, #100, #230, #242, #249, #256, #257 |
| Açık (Hafta 5) | 4: #68 bug bash, #72 README (25.11'de kapandı), #99 test raporu, #101 bu rapor |
| Hafta içinde tamamlanan issue (tüm milestone'lar) | 85 (Hafta 4: 25, Hafta 3: 21, Hafta 2: 18, Hafta 5: 13, milestone'suz: 6, Hafta 1: 1, Sunum: 1) |
| Hafta içinde kesilen (not planned) | 2: #66 (OpenAPI eşitleme, Bertan'ın kararı), #141 |
| Önceki haftalardan devreden açık issue | 0 (Hafta 1–4 milestone'ları kapandı) |
| Projenin tamamı | 161 issue'dan 150'si kapandı; 8'i kesildi. Planlı 153 issue'nun 142'si tamamlandı (%93) |
| Merge edilen PR (hafta içinde) | 104 |
| Merge edilen PR (toplam, ekip) | 175 |
| CI'da çalışan test (`main`, `7dc6b3e`) | 997 birim + 13 mimari + 184 entegrasyon = 1194, 0 hata |
| `main`'de CI (hafta içinde) | 104 çalışma: 87 başarılı, 16 iptal (yeni push), 1 başarısız (GitHub tarafında geçici Maven indirme hatası; testlere ulaşmadan) |

## 2. Tamamlanan önemli işler (18–24.11)

- **Özellikler (feature freeze kapsamında kalanlar):** bölüm ve ders CRUD (#24, #25), içerik yükleme ve sunma (#112, #113), ders oynatıcı ve içerik token'ı (#45, ADR-012), kaldığı yerden devam (#119, #120), sertifika (#129) ile görüntüleme ve herkese açık doğrulama (#130, #131), bildirim modülü (#124), bildirim API'si (#125) ve zil (#136), yayından kaldırma (#123), kurs düzenleme/silme (#23), eğitmen analitiği (#133–#135; ilerleme dağılımı backend'de bütün kurstan, #257), platform raporu (#145, #147), sınav çözme ekranları (#57, #50, #58, #304), yönetici kurs onayı (#62), eğitmen kurs formu (#33), bölüm/ders yönetimi ekranı (#34), Tamamlandı düğmesi ve ilerleme çubukları (#46).
- **P2 işler ("İnce" kapsam):** parola yenileme ve profil (#115–#117), yorum moderasyonu (#142), şikâyetler (#144, #146), mobil uyum ve ortak durum bileşenleri (#71).
- **Arayüz (22.11):** tasarım token'ları, ortak bileşen stilleri ve sayfa bazlı görsel düzenlemeler (PR #334, #335).
- **Test:** uçtan uca testler Senaryo 1–5 (#65, #138), Bölüm 6 kritik durumları ve rol × uç erişim matrisi (#139), bildirim dinleyici testleri (#137), servis test issue'larının kırmızı kontrolü (#31, #32, #44, #56, #61, #114, #128, #132), demo kalitesinde seed verisi: 10 kurs, video/PDF/DOCX içerik, açık bir şikâyet (#67, PR #337).
- **Doküman:** sınıf diyagramı (#51), bileşen diyagramı ve CI'da kodla karşılaştırılması (#91), diyagram–kod kontrolü (#97: 9 diyagram, ER ↔ migration 20/20), SOLID dokümanı (#95), kütüphaneler listesi (#100), üç sekans diyagramı (#39, #64, #140), demo senaryosu ve temiz kurulumda yürünmesi (#70), izlenebilirlik matrisi (#98: otomatik testi olan FR 6 → 43/44), test raporu 23.11 coverage'ı (#99 ara sürüm, PR #345), rapor bölüm 1–3 (#102, taslak), bölüm 4 (#103) ve 5–8 (#104, taslak), sunum soru havuzu (#107), bireysel katkı sayfaları (PR #333, #338, #341).

## 3. Açık issue'lar (24.11 sonu)

| Rol | Issue'lar |
|---|---|
| kalite | #68 (bug bash, 26–27.11), #72 (README ve ekran görüntüleri; PR #350, 25.11'de kapandı), #99 (test raporu: yalnız bug bash bölümü boş), #101 (bu rapor) |
| milestone'suz | #305 (katalogda puan filtresi; PR #342 25.11'de merge edildi) |
| Sunum milestone'u | #69, #102, #104–#107 (rapor birleştirme, slaytlar, demo provası, soru–cevap provası) |

## 4. Kişi bazında merge edilen PR'lar (18.11–24.11)

| Kişi | PR açan (merge edilen) | Merge eden |
|---|---|---|
| Bertan | 35 | 45 |
| Veronika | 30 | 58 |
| Sedat | 27 | 1 |
| Eray | 12 | – |
| **Toplam** | **104** | **104** |

Sedat bu hafta 27 PR açtı (önceki haftalarda toplam 4). 21.11'de demo yürüyüşünde açılan sınav ekranı işleri (#304, #58, #296) Eray'dan Bertan'a devredildi. Kod dondurmasından önceki son iki günde (23–24.11) yalnız 2 PR merge edildi (#343, #345); 24.11'de `main`'e commit yok.

### Hızlı kontrol

- 18.11 sabahı merge'den önce kontrol edilmeyen 7 PR (#228, #231, #232, #234–#237) geriye dönük kontrol edildi; engelleyici sorun çıkmadı. Ardından Bertan'ın karar ilkelerine "PR'da 'Kontrol edildi ✓' yoksa merge etme" şartı eklendi (#255, R6).
- **21.11'de iki dalgada 19 PR** (#294, #297–#302, #307–#310 sabah; #295, #320–#326 akşam) hızlı kontrol beklenmeden merge edildi. Hepsi merge sonrası kontrol edildi ve yorumlandı: 16'sı temizdi. 3'ünde eksik vardı ve PR yorumlarına yazıldı:
  - #297: sınıf diyagramında yeni servis yok;
  - #309: kanıt tablosu yok, tarayıcı denemesi yok, `demo.md` eski;
  - #310: kanıt tablosu yok, tarayıcı denemesi yok.
- **22.11 sabahı 6 PR** (#332–#337) yine kontrol beklenmeden merge edildi; aynı gün merge sonrası kontrol edildi. 4'ü temizdi. #334 ve #335'te (arayüz düzenlemeleri) bağlı issue ve kanıt tablosu yoktu; görsel kontroller (360 px, karanlık tema, odak halkası, düğme adları) bug bash'e bırakıldı.
- Haftada toplam 32 PR merge sonrası kontrol edildi (7 + 19 + 6).
- Merge sürecinde bir hata (21.11, Veronika): #315, `main` ile güncellendikten hemen sonra, yeni commit'in CI'ı bitmeden merge edildi. Merge sonrası `main` CI'ı yeşil geçti, PR yorumu düzeltildi. O günden beri merge, güncellenen commit'in CI'ı bitince ve yalnız o commit'e kilitlenerek (`--match-head-commit`) yapılıyor.

## 5. CI ve testler

- Test sayısı: 10.11'de 1 → 17.11 sonunda 404 → 19.11'de 1053 → 21.11'de 1188 → 23.11'de 1194.
- Entegrasyon testleri (Testcontainers, PostgreSQL 17) 18.11'de başladı; hafta sonunda 21 sınıf, 184 test (122'si erişim matrisi).
- Coverage (JaCoCo, 23.11): servis katmanı satır %98,2, dal %93,3; tüm backend satır %96,3, dal %79,2. Ayrıntı: `test-report.md`.
- Kırmızı kontrollerde 4 test boşluğu bulunup kapatıldı: #56, #44, #145 ve #137 (dinleyici `@EventListener` yapılınca mevcut 1054 testin hepsi yeşil kalıyordu). Ayrıntı: `test-report.md` › 5.
- Diğer üyeler de kendi PR'larında kırmızı kontrol yaptı ve PR açıklamasına yazdı (#303, #315, #322, #326).
- Bileşen diyagramı CI'da kodla karşılaştırılıyor (`ComponentDiagramTest`, #91); ER diyagramı ile migration'lar 20/20 aynı (#97).

## 6. Karşılaşılan sorunlar

- **Çakışan migration ve PR'lar:** aynı dosyalara dokunan PR'lar (#225, #233, #245; 21.11'de `globals.css`'e dokunan #288, #289, #300, #309) art arda rebase gerektirdi. Aynı anda gelen iki migration'da (#262, #263) zaman damgası sırası gözetilerek merge edildi.
- **Eksik ekranlar (19.11'deki liste) 20–21.11'de tamamlandı:** kurs formu (#33), yönetici onayı (#62), sertifika sayfası ve doğrulama (#130, #131), bildirim zili (#136). Hafta sonunda ekranı olmayan yalnız katalogda puan filtresi (#305; PR #342 23.11'de açıldı, 25.11'de merge edildi) ve kullanıcının şikâyet oluşturması kaldı.
- **Kontrolsüz merge'ler:** bkz. 4 › Hızlı kontrol.
- **Docker'sız üye:** Veronika'nın makinesinde Docker/Java/Node yok. Tarayıcı denemeleri ve bug bash için Docker'lı bir üye gerekiyor; diyagram görüntüleri (PlantUML) ve ekran görüntüleri diğer üyelerde üretiliyor.

## 7. Sonraki adımlar ve riskler (25.11–02.12)

| Tarih | İş | Sahip |
|---|---|---|
| 26–27.11 | Bug bash (#68), `test-plan.md` › 6; bulgular issue olarak açılıp test raporuna (#99) girer | Veronika, Bertan |
| 28.11 | Demo provası (#106), Senaryo 1–5 süre tutularak; ardından yedek demo kaydı | Sedat |
| 29–30.11 | Soru–cevap provası (#107), `docs/soru-havuzu.md` | herkes |
| 30.11 | Katkı sayfaları en fazla 1 sayfa (`docs/katki/eray.md` ~1500 kelime) | Eray |
| 01.12 | Rapor birleştirme ve ön sayfalar (#69); bölüm 1–3 ve 5–8'deki ⟨…⟩ yerleri | Bertan, Sedat, Veronika |
| 02.12 | Teslim | ekip |

- **Bug bash'te P1 hata çıkarsa** demo öncesi kapanmalı (test-plan › 7.6).
- **İlk build süresi:** temiz makinede `docker compose up --build` ~22 dk; sunum günü image'lar önceden build edilmeli (#70, #106).
- **Rapordaki yer tutucular:** birleştirilmiş raporda (PR #352) 26 ⟨…⟩ yer tutucu var; çoğu bug bash ve demo provası sonuçlarını bekliyor.

## 8. Bug bash sonucu (ek)

> 26–27.11'deki bug bash'ten sonra doldurulacak: süre, yürünen madde sayısı, bulunan hatalar (öncelik ve sahip), kapananlar.
