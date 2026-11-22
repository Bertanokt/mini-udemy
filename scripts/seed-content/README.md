# Seed içeriği üreticisi (#67)

Demo verisinin kurs içeriğini ve dosyalarını üretir. Çıktılar repoda hazır durur; demo için çalıştırmak gerekmez. İçeriği değiştirmek isteyen önce buradaki kaynakları düzenler, sonra betiği çalıştırır.

| Dosya | İçerik |
|---|---|
| `icerik.mjs` | Kurslar, bölümler, dersler (video slaytlarının maddeleri, PDF notlarının metni ve kod örnekleri), sınav soruları, eğitmenler, kategori renkleri |
| `kullanici-verisi.sql` | Kayıtlar, ilerleme, sınav denemeleri, sertifika, değerlendirmeler, şikâyet ve bildirimler (demo senaryolarının beklediği durumlar) |
| `generate.mjs` | Üretici |
| `docx.py` | DOCX yazıcı (yalnız Python standart kütüphanesi) |

## Çalıştırmak

```bash
node scripts/seed-content/generate.mjs
```

Gerekenler: Node.js 22+, Google Chrome (başka bir yoldaysa `CHROME=/yol/chrome`), `ffmpeg` ve `ffprobe`, `python3`. Yeni npm bağımlılığı yok: görseller ve PDF'ler Chrome'un DevTools protokolüyle, Node'un kendi WebSocket'i üzerinden üretilir.

Ürettikleri (her çalıştırmada baştan):

- `backend/src/main/resources/seed/files/<storage_key>`: kapak görselleri (PNG, 1280×720), PDF ders notları (A4), video dersler (MP4, H.264, 720p, slayt tarzı, `-preset veryfast`), DOCX dokümanlar. Dosya adı depolama anahtarıdır (UUID); `DemoSeedRunner` açılışta bunları depolama dizinine kopyalar.
- `backend/src/main/resources/seed/demo-seed.sql`: kullanıcılar, dosyalar, kurs yapısı ve sınavlar `icerik.mjs`'ten üretilir, sonuna `kullanici-verisi.sql` eklenir. Bu dosyayı elle değiştirmeyin.

Kurallar (betik denetler): kapak ≤ 2 MB (kapak dosya kuralı), video ≤ 2 MB. Ders ve sınav adları kurs içinde, soru metinleri ve sınav adları bütün seed'de tekil olmalı; `kullanici-verisi.sql` satırları başlıkla eşler.

Demo senaryolarının dayandığı sayılar (README › Demo hesapları, `docs/demo.md`) değişirse `DemoSeedIT`, README ve `docs/demo.md` aynı PR'da güncellenir.
