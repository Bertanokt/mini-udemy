# Katkı — Veronika (Kalite ve Süreç Sorumlusu)

GitHub: [@vliashenko6](https://github.com/vliashenko6) · Son güncelleme: 16.11.2026

## Yaptığım işler

| Issue | PR | Çıktı |
|---|---|---|
| [#78 Fonksiyonel olmayan gereksinimler](https://github.com/Bertanokt/mini-udemy/issues/78) | [#156](https://github.com/Bertanokt/mini-udemy/pull/156) | `docs/non-functional-requirements.md`: 3.3.1–3.3.8 başlıklarında 19 NFR (ölçüt, doğrulama yöntemi, kaynak); `requirements.md` ve `docs/README.md`'den link |
| [#92 Test planı](https://github.com/Bertanokt/mini-udemy/issues/92) | [#174](https://github.com/Bertanokt/mini-udemy/pull/174) | `docs/test-plan.md`: test seviyeleri ve araçları (JUnit 5, Mockito, Testcontainers, ArchUnit), sorumlular, FR-01…44 ve NFR-01…19 → test eşlemesi, bug bash listesi, 7 başarı ölçütü |
| [#89 JaCoCo coverage raporu](https://github.com/Bertanokt/mini-udemy/issues/89) | [#175](https://github.com/Bertanokt/mini-udemy/pull/175) | `backend/pom.xml`'e `jacoco-maven-plugin`; CI her çalışmada HTML raporu `jacoco-report` artifact'ı olarak saklıyor; `README.md`'de raporun nereden açılacağı |
| [#83](https://github.com/Bertanokt/mini-udemy/issues/83), [#90](https://github.com/Bertanokt/mini-udemy/issues/90), [#93](https://github.com/Bertanokt/mini-udemy/issues/93) İlerleme raporları | [#176](https://github.com/Bertanokt/mini-udemy/pull/176), [#177](https://github.com/Bertanokt/mini-udemy/pull/177), [#187](https://github.com/Bertanokt/mini-udemy/pull/187) | `docs/progress/hafta-1.md`, `hafta-2.md`, `hafta-3.md`: planlanan / tamamlanan / devreden issue'lar, kişi bazında PR'lar, CI ve test sayısı, sorunlar ve riskler |
| [#82 Gantt, görev dağılımı, risk analizi](https://github.com/Bertanokt/mini-udemy/issues/82) | [#188](https://github.com/Bertanokt/mini-udemy/pull/188) | `docs/plan.md` ve `docs/diagrams/gantt.mmd`: planlanan ve gerçekleşen takvim, 7 iş paketi, rol × milestone görev dağılımı, 9 risk, bütçe |

**Review ve hızlı kontroller**

| PR | Ne yaptım |
|---|---|
| [#154](https://github.com/Bertanokt/mini-udemy/pull/154) (Eray, API sözleşmesi) | NFR-04 (401) ve NFR-08 (alan bazlı hata) ile karşılaştırdım, onayladım. `fullName` alanının sözleşmede 100, ER diyagramında 150 karakter olduğunu tespit ettim; Eray #162'de 100'e eşitledi |
| [#153](https://github.com/Bertanokt/mini-udemy/pull/153) (Eray, ER diyagramı) | NFR-09/10/11/12 ve FR'lerle karşılaştırdım, onayladım. FR-38 için yayından kaldırma gerekçesinin nerede tutulacağını sordum; Eray migration'a ayrı `unpublish_reason` kolonu ekledi |
| [#155](https://github.com/Bertanokt/mini-udemy/pull/155) (Eray, katkı notu) | Linkleri ve bilgileri kontrol ettim, onayladım |
| [#164](https://github.com/Bertanokt/mini-udemy/pull/164) (Bertan, ADR-004/005) | İlk hızlı kontrolüm: CI, `Closes` + kanıt tablosu, Ne/Neden, OpenAPI/diyagram; "Kontrol edildi ✓" yazıp merge ettim |
| [#172](https://github.com/Bertanokt/mini-udemy/pull/172) (Bertan, User entity) | Entity'nin kolonlarını `users` migration'ıyla tek tek karşılaştırdım; `full_name` uzunluğunun artık migration, ER ve kodda aynı (100) olduğunu doğruladım |
| [#186](https://github.com/Bertanokt/mini-udemy/pull/186) (Bertan, giriş ucu) | Kodu Ne/Neden ve OpenAPI ile karşılaştırdım (aynı 401, pasif hesapta 403, istek/yanıt şemaları); merge ettim |

## Kararlar ve nedenleri

- **Her NFR ölçülebilir bir ölçütle yazıldı.** "Hızlı olmalı" test edilemez; "size en fazla 50" ya da "360 px'te yatay kaydırma yok" test edilebilir. §5 dosya türü ve boyutu denetimi istiyor; sınırları (video 200 MB, doküman 20 MB, kapak 2 MB; aşan 413, izin verilmeyen tür 415) NFR-11'de biz belirledik.
- **Test planında her FR ve NFR bir test yöntemine bağlandı.** Böylece hiçbir gereksinim testsiz kalmıyor; test issue'su olmayan 7 FR tabloda "–" ile açıkça görünüyor.
- **Coverage hedefi servis katmanında %70.** İş kurallarının çoğu servis katmanında; %100 bu sürede gerçekçi değil. İlk ölçüm %79,7.
- **Coverage raporu CI'da saklanıyor.** Herkes bilgisayarına bir şey kurmadan her PR'ın raporunu indirebiliyor.
- **İlerleme raporları sonradan yazıldığı için geriye dönük tarih atmadım.** Her raporun başında veri tarihi var; hafta sonundaki durum ile raporun yazıldığı günkü durum ayrı sütunlarda.
- **Hızlı kontrolde dört maddeye bakıyorum:** CI yeşil mi, `Closes #…` ve kanıt tablosu var mı, Ne/Neden kodla uyuşuyor mu, kod değiştiyse OpenAPI ve diyagram güncellendi mi.
- **Gantt'ı Mermaid ile çizdim.** GitHub Mermaid'i doğrudan gösteriyor; yerelde Java gerekmiyor.

## Karşılaştığım problemler

- NFR dosyası bana mesajla gönderilmişti ama indirmemiştim; içeriği mesajdan alarak ekledim. Claude'un tarayıcı panelinde kopyala-yapıştır çalışmadı; dosya içeriği editöre başka yolla girildi.
- Hafta 1–3 raporlarını haftalar bittikten sonra yazdım. Board sütunların geçmişini saklamadığı için planlanan işleri milestone'dan aldım ve bunu raporlarda belirttim.
- Hafta 3 raporunda P2 kesme kuralının uygulanmadığını tespit ettim (Hafta 3'ün P1'leri bitmediği hâlde Hafta 4'ün 10 P2'si açıktı); Bertan freeze öncesi karar vereceğini yazdı.
- Test planı PR'ında "her FR'yi ayrı bir test issue'su ile eşleştirdim" yazmıştım; Bertan 7 FR'nin test issue'su olmadığını fark etti.
- Bilgisayarımda Java yok; JaCoCo raporunun üretildiğini CI'daki artifact ile doğruladım.
- #186'daki `AuthServiceTest`, #15'in giriş testlerini karşılıyor; #15'te bana kayıt testleri kalıyor.

## Planlanan

- AuthService kayıt testleri ([#15](https://github.com/Bertanokt/mini-udemy/issues/15)), #12 merge edilince
- Hafta 4 ve 5 ilerleme raporları ([#96](https://github.com/Bertanokt/mini-udemy/issues/96), [#101](https://github.com/Bertanokt/mini-udemy/issues/101))
- İzlenebilirlik matrisi ([#98](https://github.com/Bertanokt/mini-udemy/issues/98)), test raporu ([#99](https://github.com/Bertanokt/mini-udemy/issues/99)), bug bash ([#68](https://github.com/Bertanokt/mini-udemy/issues/68))
- Açılan her PR'ın hızlı kontrolü

_Bu dosyanın taslağında ve GitHub'daki adımların bir kısmında Claude'dan yardım aldım._
