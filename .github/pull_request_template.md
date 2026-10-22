Closes #
<!-- Aynı modülde birbirine bağlı birden fazla issue: Closes #a #b #c -->

## Ne / Neden
<!-- Bu bölümü PR sahibi KENDİ cümleleriyle yazar (Claude Code ya da başka bir araç yazmaz).
     Ne değişti, neden bu şekilde yapıldı, hangi seçeneği neden seçmedin? 3–6 cümle yeterli. -->

## Kabul kriteri → kanıt
| Kabul kriteri | Kanıt (test adı / manuel kontrol) |
|---|---|
|  |  |

## Nasıl test edildi
<!-- Çalıştırdığın komutlar ve sonuçları (./mvnw verify, npm run lint && npm run build), denediğin akış. Ekran değişikliği varsa ekran görüntüsü. -->

## Güncellenen dokümanlar
<!-- OpenAPI, ER, sınıf/sekans/bileşen diyagramı, ADR. Gerekmediyse nedenini yaz. Yeni kütüphane eklendiyse adı ve neden. -->

## Definition of Done
- [ ] Branch adı kurala uygun (`feature/#<no>-kisa-ad` / `fix/#<no>-kisa-ad`)
- [ ] Commit ve PR açıklamasında `Closes #<no>` var
- [ ] Issue'daki tüm kabul kriterleri karşılandı ve yukarıdaki tabloda kanıtı var
- [ ] CI yeşil (backend build + test, frontend lint + build)
- [ ] En az 1 onay alındı (modül sorumlusundan)
- [ ] Kod değiştiyse ilgili diyagram ve OpenAPI aynı PR'da güncellendi; yeni mimari karar varsa ADR yazıldı
- [ ] Yeni migration zaman damgalı (`V<yyyyMMddHHmm>__aciklama.sql`); merge edilmiş migration'lar değiştirilmedi
- [ ] "Ne / Neden" bölümünü kendim yazdım
