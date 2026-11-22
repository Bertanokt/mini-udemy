# Bireysel teknik katkı — Veronika (@vliashenko6), Kalite ve Süreç Sorumlusu

Son güncelleme: 22.11.2026

## Yaptığım işler

- **Gereksinim ve plan** — [#156](https://github.com/Bertanokt/mini-udemy/pull/156) NFR'ler ([#78](https://github.com/Bertanokt/mini-udemy/issues/78)), [#174](https://github.com/Bertanokt/mini-udemy/pull/174) test planı ([#92](https://github.com/Bertanokt/mini-udemy/issues/92)), [#188](https://github.com/Bertanokt/mini-udemy/pull/188), [#192](https://github.com/Bertanokt/mini-udemy/pull/192) Gantt ve risk ([#82](https://github.com/Bertanokt/mini-udemy/issues/82)), [#175](https://github.com/Bertanokt/mini-udemy/pull/175) JaCoCo ([#89](https://github.com/Bertanokt/mini-udemy/issues/89))
- **Uçtan uca testler** — [#239](https://github.com/Bertanokt/mini-udemy/pull/239) Testcontainers altyapısı, [#243](https://github.com/Bertanokt/mini-udemy/pull/243), [#246](https://github.com/Bertanokt/mini-udemy/pull/246), [#250](https://github.com/Bertanokt/mini-udemy/pull/250) Senaryo 1–3 ([#65](https://github.com/Bertanokt/mini-udemy/issues/65)), [#268](https://github.com/Bertanokt/mini-udemy/pull/268), [#312](https://github.com/Bertanokt/mini-udemy/pull/312) Senaryo 4–5 ([#138](https://github.com/Bertanokt/mini-udemy/issues/138)), [#269](https://github.com/Bertanokt/mini-udemy/pull/269) Bölüm 6 ve rol × uç erişim matrisi ([#139](https://github.com/Bertanokt/mini-udemy/issues/139))
- **Servis testleri ve kırmızı kontrol** — [#248](https://github.com/Bertanokt/mini-udemy/pull/248) ([#56](https://github.com/Bertanokt/mini-udemy/issues/56)), [#261](https://github.com/Bertanokt/mini-udemy/pull/261) ([#44](https://github.com/Bertanokt/mini-udemy/issues/44)), [#285](https://github.com/Bertanokt/mini-udemy/pull/285) ([#137](https://github.com/Bertanokt/mini-udemy/issues/137)); #15, #31, #32, #61, #114, #128, #132 kırmızı kontrolle kapandı
- **Kod** — [#206](https://github.com/Bertanokt/mini-udemy/pull/206) kurs oluşturma ([#22](https://github.com/Bertanokt/mini-udemy/issues/22)), [#238](https://github.com/Bertanokt/mini-udemy/pull/238) bölüm CRUD ([#24](https://github.com/Bertanokt/mini-udemy/issues/24)), [#267](https://github.com/Bertanokt/mini-udemy/pull/267), [#276](https://github.com/Bertanokt/mini-udemy/pull/276) platform raporu ([#145](https://github.com/Bertanokt/mini-udemy/issues/145), [#147](https://github.com/Bertanokt/mini-udemy/issues/147)), [#264](https://github.com/Bertanokt/mini-udemy/pull/264), [#282](https://github.com/Bertanokt/mini-udemy/pull/282) seed verisi ([#67](https://github.com/Bertanokt/mini-udemy/issues/67))
- **Raporlar ve dokümanlar** — ilerleme raporları [#176](https://github.com/Bertanokt/mini-udemy/pull/176), [#177](https://github.com/Bertanokt/mini-udemy/pull/177), [#187](https://github.com/Bertanokt/mini-udemy/pull/187), [#247](https://github.com/Bertanokt/mini-udemy/pull/247), [#280](https://github.com/Bertanokt/mini-udemy/pull/280); izlenebilirlik matrisi [#190](https://github.com/Bertanokt/mini-udemy/pull/190), [#281](https://github.com/Bertanokt/mini-udemy/pull/281), [#318](https://github.com/Bertanokt/mini-udemy/pull/318) ([#98](https://github.com/Bertanokt/mini-udemy/issues/98)); test raporu [#279](https://github.com/Bertanokt/mini-udemy/pull/279) ([#99](https://github.com/Bertanokt/mini-udemy/issues/99)); demo senaryosu [#275](https://github.com/Bertanokt/mini-udemy/pull/275), [#311](https://github.com/Bertanokt/mini-udemy/pull/311) ([#70](https://github.com/Bertanokt/mini-udemy/issues/70)); diyagram–kod kontrolü [#316](https://github.com/Bertanokt/mini-udemy/pull/316) ([#97](https://github.com/Bertanokt/mini-udemy/issues/97)); bug bash listesi [#330](https://github.com/Bertanokt/mini-udemy/pull/330); README [#331](https://github.com/Bertanokt/mini-udemy/pull/331); rapor bölüm 5–8 [#287](https://github.com/Bertanokt/mini-udemy/pull/287) ([#104](https://github.com/Bertanokt/mini-udemy/issues/104))
- **Hızlı kontrol** — ekibin PR'larının hepsi; merge'den önce kontrol edilmeyenler (18.11'de 7, 21.11'de 19) sonradan kontrol edilip yorumlandı

## Kararlar ve nedenleri

- **Her NFR ölçülebilir:** "hızlı olmalı" test edilemez; "sayfa boyutu en fazla 100, aşan 400" ya da "360 px'te yatay kaydırma yok" test edilebilir. Dosya sınırlarını (video 200 MB, doküman 20 MB, kapak 2 MB; 413/415) NFR-11'de belirledik.
- **Kırmızı kontrol:** coverage yüksekken bile testin hatayı yakaladığını ayrıca kanıtladım: kodu bilerek bozup CI'da kırmızıyı gördüm, sonra geri aldım. Dört test açığı buldu (#56, #44, #145, #137); #137'de dinleyici bozulunca 1054 testin hepsi yeşil kalıyordu.
- **Erişim matrisi sözleşmeden okunuyor:** rolleri elle yazmadım; `AccessMatrixIT` OpenAPI'deki `x-roles`'u koddaki yetkiyle karşılaştırıyor. Yeni uç eklenince matrise kendiliğinden giriyor.
- **Hızlı kontrolün dört maddesi:** CI, `Closes` + kanıt tablosu, Ne/Neden ↔ kod, OpenAPI/diyagram güncelliği. İddiaları kodu okuyarak kontrol ettim.

## Karşılaştığım problemler

- **Docker/Java/Node yok:** bütün doğrulamayı CI'da yaptım; IT'ye özgü kırmızı kontrollerde surefire'ı geçici kapatan geçici PR'lar kullandım. Diyagram görüntülerini PlantUML'i olan üyeler üretti.
- **Sahte işlem yöneticisi (#137):** `REQUIRES_NEW` dinleyici sessizce çalışmıyordu; askıya almayı destekleyen bir test yöneticisi yazdım.
- **Merge hatası (#315):** dalı güncelledikten sonra eski commit'in yeşil CI'ına bakıp merge ettim. `main` CI'ı yeşil geçti, yorumu düzelttim; o günden beri yalnız yeni commit'in CI'ı bitince ve o commit'e kilitli (`--match-head-commit`) merge ediyorum.

## Araç kullanımı

Kod, test ve doküman taslaklarını Claude Code ile ürettim. Neyin test edileceğine, kırmızı kontrol bozmalarına ve PR kararlarına ben karar verdim; her PR'ı CI ve kırmızı kontrolle doğruladım.
