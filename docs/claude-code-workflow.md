# Claude Code ile issue akışı

Ekipteki herkes her issue için aynı akışı izler: **issue'yu oku → plan → onay → uygula → test → PR**. Claude Code repo kökündeki `CLAUDE.md`'yi kendiliğinden okur; aşağıdaki şablon onun üstüne issue'ya özgü adımları ekler.

Amaç yalnız kodu yazdırmak değil: sunumda hoca her üyeye kendi kodu hakkında teknik soru soracak ve bireysel katkı katsayısını buna göre verecek. Planı okuyup onaylayan, testleri yorumlayan ve PR'ın "Ne / Neden" bölümünü yazan **sensin**.

## Başlamadan önce (bir kez)

- `gh auth status`: aktif hesap **kendi** GitHub hesabın olmalı.
- `git config user.name` / `git config user.email`: GitHub hesabındaki e-postayla aynı olmalı, yoksa katkın profilinde ve Git geçmişinde görünmez.
- `main` güncel: `git switch main && git pull`.

## Standart prompt şablonu

Board'da "Bu Hafta" kolonundan kendi rol etiketli issue'nu al, kendine ata, "In Progress"e taşı. Sonra Claude Code'a şunu yapıştır (`<no>` yerine issue numarası):

```text
#<no> numaralı issue üzerinde çalışacağız. Aşağıdaki adımları sırayla izle; her "DUR" noktasında benim cevabımı bekle.

1. OKU
   - `gh issue view <no>` ile issue'yu oku. Gövdedeki "Blocked by" issue'larının kapalı olduğunu `gh issue view` ile kontrol et; açık olan varsa söyle ve DUR.
   - CLAUDE.md'yi ve issue'nun dokunduğu modülün mevcut kodunu oku. İlgili OpenAPI bölümüne ve diyagramlara bak.
   - Proje tanım dokümanındaki hangi maddeyi (A–L, senaryo 1–5, bölüm 5–7) karşıladığını yaz.

2. PLAN — kod yazma.
   - Kabul kriterlerini madde madde listele; belirsiz olanları soru olarak yaz.
   - Değişecek/eklenecek dosyalar (paket ve katmanıyla), yeni sınıflar, varsa Flyway migration adı (V<yyyyMMddHHmm>__...).
   - Kullanılan ya da etkilenen desen (State, Observer, Strategy, Factory Method, DTO+Mapper) ve nasıl uygulanacağı.
   - Yazılacak testlerin listesi: her kabul kriteri hangi testle doğrulanacak.
   - Güncellenecek dokümanlar: OpenAPI, ER, sınıf/sekans/bileşen diyagramı, ADR (CLAUDE.md'deki tabloya göre). Gerekmiyorsa nedenini yaz.
   - Kapsam dışı kalan ya da yeni issue gerektiren bir şey varsa belirt.
   DUR: planı onaylamamı ya da düzeltmemi bekle.

3. UYGULA
   - Branch aç: git switch -c 'feature/#<no>-kisa-ad'
   - Onaylanan planı uygula; plandan sapman gerekirse önce söyle.
   - Dokümanları aynı branch'te güncelle.

4. TEST
   - Testleri yaz. Her yeni testin doğru sebeple kırmızıya döndüğünü bir kez göster (kodu geçici bozarak), sonra geri al.
   - Backend değiştiyse `cd backend && ./mvnw verify`, frontend değiştiyse `cd frontend && npm run lint && npm run build` çalıştır ve çıktının özetini göster.
   - Kabul kriteri → kanıt (test adı ya da manuel kontrol adımı) tablosunu çıkar. Karşılanmayan kriter varsa açıkça yaz.
   DUR: sonuçları incelememi bekle.

5. PR
   - Commit mesajı: kısa özet + `Closes #<no>`.
   - .github/pull_request_template.md'ye göre PR açıklaması taslağı hazırla. "Ne / Neden" bölümünü BOŞ bırak; onu ben kendi cümlelerimle yazacağım.
   DUR: "Ne / Neden" metnimi ve push onayımı bekle.
   - Onaydan sonra push et ve `gh pr create` ile PR'ı aç; PR linkini ver.
```

## Review için prompt

Bir arkadaşının PR'ını incelerken:

```text
#<pr-no> numaralı PR'ı review edeceğim. `gh pr view <pr-no>` ve `gh pr diff <pr-no>` ile oku; bağlı issue'nun kabul kriterlerini de oku.
Şunları kontrol edip madde madde raporla, yorum yazma ya da onaylama işini bana bırak:
- Her kabul kriteri karşılanmış ve testle doğrulanmış mı?
- CLAUDE.md kurallarına uyuyor mu (katmanlar, modüller arası erişim, DTO/Mapper, hata formatı, isimlendirme, Flyway adı)?
- Desen kullanımı doğru mu, deseni atlatan if/switch var mı?
- OpenAPI ve diyagramlar kodla tutarlı mı?
- Eksik test, güvenlik açığı (yetki kontrolü, dosya erişimi), gereksiz değişiklik var mı?
```

## Kendi kodunu anlamak için prompt

Merge'den sonra ve sunumdan önce, kendi yazdığın her issue için:

```text
#<no> numaralı issue'da yazdığımız kodu bana sunumda anlatabileceğim şekilde açıkla: hangi sınıf ne yapıyor, istek hangi katmanlardan geçiyor, hangi desen neden kullanıldı, hangi testler neyi garanti ediyor.
Sonra bana hocanın sorabileceği 5 teknik soru sor ve cevaplarımı değerlendir.
```

## Notlar

- Birbirine bağlı küçük issue'ları aynı modülde tek PR'da kapatabilirsin (`Closes #a #b #c`); şablonda `<no>` yerine hepsini yaz.
- Claude Code bir kuralı çiğnemeyi önerirse (test kapatmak, ArchUnit kuralını gevşetmek, merge edilmiş migration'ı değiştirmek) reddet; CLAUDE.md'deki "Yapma" listesine bak.
- Oturum uzadıysa ya da başka bir issue'ya geçiyorsan yeni bir Claude Code oturumu aç; bir oturum = bir issue.
