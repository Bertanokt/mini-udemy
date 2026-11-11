# Mimari karar kayıtları (ADR)

ADR (Architecture Decision Record), önemli bir mimari kararı alındığı bağlamla, değerlendirilen alternatiflerle ve sonuçlarıyla birlikte kayıt altına alan kısa bir dokümandır. Kodun *nasıl* yazıldığını kod gösterir; *neden* öyle yazıldığını ADR'ler gösterir.

## Ne zaman ADR yazılır

- Mimariyi, modül sınırlarını ya da modüller arası iletişimi etkileyen bir karar alındığında
- Bir tasarım deseni bilinçli olarak seçildiğinde (desen issue'ları ADR ister)
- Teknoloji, kütüphane ya da altyapı seçiminde birden fazla makul seçenek varken
- Mevcut bir ADR'deki karar değiştiğinde (yeni ADR yazılır, eskisi "Yerine geçildi" olur)

ADR, kararın alındığı iş ile **aynı PR'da** yazılır.

## Kurallar

- **Dosya adı:** `NNNN-kisa-ad.md`. `NNNN` dört haneli sıra numarasıdır (`0001`, `0002`…); kısa ad küçük harf ve tire ile yazılır. Numara, ADR `main`'e girdiği sırayla verilir; aynı numarayı iki PR alırsa sonra merge edilen numarasını kaydırır.
- **Şablon:** [`0000-sablon.md`](0000-sablon.md).
- **Tarihler:** iki ayrı alan var. *Karar tarihi* kararın gerçekten alındığı gün, *kayıt tarihi* ADR'nin yazıldığı gündür. Karar ADR'den önce alındıysa iki tarih farklıdır; tarih **geriye dönük yazılmaz**.
- **Değişmezlik:** kabul edilmiş bir ADR'nin kararı sonradan değiştirilmez. Karar değişirse yeni bir ADR yazılır, eskisinin durumu "Yerine geçildi: ADR-NNNN" olur. Yazım hatası ve bağlantı düzeltmesi serbesttir.

## Durumlar

| Durum | Anlamı |
|---|---|
| Önerildi | Karar PR'da tartışılıyor, henüz kabul edilmedi |
| Kabul edildi | Karar geçerli; kod bu karara uyar |
| Yerine geçildi: ADR-NNNN | Karar yeni bir ADR ile değiştirildi |
| Reddedildi | Önerilen karar kabul edilmedi (gerekçesiyle saklanır) |

## Kayıtlar

| No | Başlık | Durum | Issue |
|---|---|---|---|
| [ADR-001](0001-moduler-monolit.md) | Katmanlı modüler monolit ve modül bazlı paketleme | Kabul edildi | #80 |

### Planlanan

| No | Konu | Issue |
|---|---|---|
| ADR-002 | JWT ile stateless kimlik doğrulama | #81 |
| ADR-003 | Flyway ile şema yönetimi | #81 |
| ADR-004 | Yerel dosya depolama (Docker volume, yetki kontrollü servis) | #84 |
| ADR-005 | Ayrı frontend, REST ve önce OpenAPI sözleşmesi | #84 |
| sonraki numaralar | Desen kararları: DTO+Mapper (#9), State (#85), Factory Method (#110), Observer (#42), Strategy (#52) | ilgili issue'lar |

Genel mimari kurallar: [`docs/architecture.md`](../architecture.md).
