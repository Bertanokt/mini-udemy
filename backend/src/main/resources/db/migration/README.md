# Flyway migration'ları

## İsimlendirme

```
V<yyyyMMddHHmm>__<kisa_aciklama>.sql
```

- Sürüm, dosyayı oluşturduğun anın zaman damgasıdır (yerel saat, dakikaya kadar). Örnek: `V202610231415__create_users.sql`.
- Zaman damgasını elle yazma, üret: `date +%Y%m%d%H%M`
- Açıklama snake_case, İngilizce ve ne yaptığını söyler: `create_courses`, `add_cover_to_courses`, `create_quiz_attempts`.
- `V` büyük harf, sürümden sonra **iki** alt çizgi.

Neden sıra numarası (V1, V2…) değil: dört kişi paralel çalışırken iki PR aynı numarayı alabiliyor ve sonra merge edilen PR kırılıyordu. Zaman damgası çakışmayı pratikte ortadan kaldırır.

## Kurallar

- Merge edilmiş bir migration dosyası **bir daha değiştirilmez**; düzeltme yeni bir migration ile yapılır. (Flyway checksum'ı değişen dosyada uygulamayı başlatmaz.)
- Bir migration tek bir mantıksal değişiklik içerir (bir tablo grubu ya da bir alan ekleme).
- Şema ER diyagramıyla (`docs/diagrams/er.*`) aynı PR'da güncellenir.
- `dev` profilinde `spring.flyway.out-of-order: true` açıktır: senin DB'nde daha yeni tarihli bir migration uygulanmışken başkasının daha eski tarihli migration'ı merge edilirse o da uygulanır. CI ve demo temiz veritabanından başladığı için orada sıra her zaman zaman damgasına göredir.
- Yerel DB'n bozulursa: `docker compose down -v && docker compose up -d postgres` (tüm yerel veri silinir).
