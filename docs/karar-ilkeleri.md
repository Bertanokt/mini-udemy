# Karar ilkeleri: Bertan'ın Claude oturumu

> **Kapsam:** bu doküman yalnız Bertan'ın (Yazılım Mimarı, @Bertanokt) Claude Code oturumu için geçerlidir. Diğer ekip üyelerinin oturumlarında uygulanmaz; ekibin akışı `CONTRIBUTING.md` ve `docs/claude-code-workflow.md`'de olduğu gibi kalır. Buradaki maddeler o iki dokümanı değiştirmez, yalnız Bertan'ın Claude'una hangi kararları kendisinin verip hangilerinde sorup bekleyeceğini söyler.

## Kendin karar ver, bana yalnız bildir

- **Merge:** CI yeşil, main'le birleşik tam derleme (`./mvnw clean verify`) yeşil ve ArchUnit geçiyorsa kendi PR'larımı ve kontrol ettiğin ekip PR'larını merge et. **Ek şart:** PR'da Kalite ve Süreç sorumlusunun "Kontrol edildi ✓" yorumu da olmalı (`CONTRIBUTING.md` › DoD: "CI yeşil + Kalite ve Süreç sorumlusunun hızlı kontrolü"; risk R6). Bu yorum yoksa merge etme, bana bildir.
- **Doküman çakışmaları:** ekip PR'larında yalnız doküman ya da javadoc çakışması varsa iki tarafı koruyarak çöz (merge commit, force push yok) ve PR'a not bırak.
- **Atıf satırı:** PR açıklamasındaki Claude atıf satırını sil ve devam et.
- **Plan seçenekleri:** planda önerdiğin seçenek proje dokümanı, sözleşme ve ADR'lerle uyumluysa ve geri alınabilirse önerinle ilerle; gerekçeyi PR'a yaz.
- **Takip işleri:** küçük takip işleri için issue aç; doğru rolün sahibine ata, sahipsizse bana.
- **Sıralama ölçütü:**
  1. önce başkasını bloke eden iş;
  2. sonra demoda görünen iş (5 senaryo);
  3. sonra doküman.

## Bana sor ve bekle

- **Yeni bağımlılık:** kütüphane ya da araç.
- **Sözleşme ve ADR:** sözleşmeyi (OpenAPI) ya da kabul edilmiş bir ADR'yi değiştiren karar; güvenlik ödünleşimi.
- **Kapsam ve devir:** kapsam kesme ya da başka rolden iş devri (R10).
- **Kod çakışması:** kod çakışması olan ekip PR'ı.
- **Geri alınamayan işlem:** silme, force push, geçmişi yeniden yazma.
- **Hocanın dokümanı:** hocanın dokümanıyla çelişen bir durum.
