# Katkı — Veronika (Kalite Sorumlusu)

GitHub: [@vliashenko6](https://github.com/vliashenko6)

## Yaptığım işler

| Issue | PR | Çıktı |
|---|---|---|
| [#78 Fonksiyonel olmayan gereksinimler](https://github.com/Bertanokt/mini-udemy/issues/78) | [#156](https://github.com/Bertanokt/mini-udemy/pull/156) | `docs/non-functional-requirements.md`: 3.3.1–3.3.8 başlıklarında 19 NFR (ölçüt, doğrulama yöntemi, kaynak); `requirements.md` ve `docs/README.md`'den link |
| — | [#154](https://github.com/Bertanokt/mini-udemy/pull/154) review | OpenAPI sözleşmesini NFR-04 (401) ve NFR-08 (alan bazlı hata) ile karşılaştırdım, onayladım |
| — | [#153](https://github.com/Bertanokt/mini-udemy/pull/153) review | ER diyagramını NFR-09/10/11/12 ve FR'lerle karşılaştırdım; FR-38 ve FR-11 için iki soru sordum, onayladım |
| — | [#155](https://github.com/Bertanokt/mini-udemy/pull/155) review | Eray'ın katkı açıklamasındaki linkleri ve bilgileri kontrol ettim, onayladım |

## Kararlar ve nedenleri

- **Her NFR ölçülebilir bir ölçütle yazıldı.** "Hızlı olmalı" gibi bir cümle test edilemez; "size en fazla 50" ya da "360 px'te yatay kaydırma yok" test edilebilir.
- **Sınırlar proje tanım dokümanının §5'inden alındı** (video 200 MB, doküman 20 MB, kapak 2 MB). Sınırı aşan dosya 413, izin verilmeyen tür 415 döner.
- **Her NFR'ye doğrulama yöntemi yazıldı** (Test / İnceleme / Demo). Böylece test planında (#92) hangi NFR'nin otomatik test, hangisinin demo ile gösterileceği baştan belli.
- **README'de yalnız gereksinimler satırına link verdim.** Kullanım senaryoları satırı NFR'lerle ilgili değil.

## Karşılaştığım problemler

- NFR dosyası bana mesajla gönderilmişti ama indirmemiştim; içeriği mesajdan alarak ekledim.
- Claude'un tarayıcı panelinde kopyala-yapıştır çalışmadı; dosya içeriği editöre başka yolla girildi.
- #153 review'ında ER'de yayından kaldırma gerekçesi (FR-38) için bir alan bulamadım; Eray'a sordum.

## Planlanan

- Test planı ([#92](https://github.com/Bertanokt/mini-udemy/issues/92))
- Test raporu
- Haftalık ilerleme raporları

_Bu dosyanın taslağında ve GitHub'daki adımların bir kısmında Claude'dan yardım aldım._
