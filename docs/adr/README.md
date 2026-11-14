# Mimari karar kayıtları (ADR)

ADR (Architecture Decision Record), önemli bir mimari kararı alındığı bağlamla, değerlendirilen alternatiflerle ve sonuçlarıyla birlikte kayıt altına alan kısa bir dokümandır. Kodun *nasıl* yazıldığını kod gösterir; *neden* öyle yazıldığını ADR'ler gösterir.

## Ne zaman ADR yazılır

- Mimariyi, modül sınırlarını ya da modüller arası iletişimi etkileyen bir karar alındığında
- Bir tasarım deseni bilinçli olarak seçildiğinde (desen issue'ları ADR ister)
- Teknoloji, kütüphane ya da altyapı seçiminde birden fazla makul seçenek varken
- Mevcut bir ADR'deki karar değiştiğinde (yeni ADR yazılır, eskisi "Yerine geçildi" olur)

ADR, kararın alındığı iş ile **aynı PR'da** yazılır.

## Kurallar

- **Dosya adı:** `NNNN-kisa-ad.md`. `NNNN` dört haneli numaradır (`0001`, `0002`…); kısa ad küçük harf ve tire ile yazılır.
- **Numaralar planlamada ayrılır:** planlanan bir ADR, aşağıdaki "Planlanan" tablosunda ayrılmış numarasını kullanır; ADR'nin `main`'e giriş sırası belirleyici değildir (örn. ADR-004, ADR-002'den önce merge edilebilir). Planda olmayan yeni bir ADR, kullanılmamış ve ayrılmamış ilk numarayı alır ve aynı PR'da tabloya eklenir. Aynı numara iki PR'da kullanılırsa sonra merge edilen numarasını kaydırır.
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
| [ADR-004](0004-yerel-dosya-depolama.md) | Yerel dosya depolama (Docker volume) ve yetki kontrollü servis | Kabul edildi | #84 |
| [ADR-005](0005-ayri-frontend-rest-openapi.md) | Ayrı frontend, REST API ve önce OpenAPI sözleşmesi | Kabul edildi | #84 |

### Planlanan

| No | Konu | Issue |
|---|---|---|
| ADR-002 | JWT ile stateless kimlik doğrulama | #81 |
| ADR-003 | Flyway ile şema yönetimi | #81 |
| sonraki numaralar | Desen kararları: DTO+Mapper (#9), State (#85), Factory Method (#110), Observer (#42), Strategy (#52) | ilgili issue'lar |

Genel mimari kurallar: [`docs/architecture.md`](../architecture.md).
