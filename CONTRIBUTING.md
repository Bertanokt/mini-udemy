# Çalışma kuralları

## Akış

İşler GitHub Project board'unda ("Mini Udemy") şu kolonlardan geçer:

**Backlog → Bu Hafta → In Progress → Review → Done**

| Kolon | Anlamı | Kim taşır |
|---|---|---|
| Backlog | Henüz planlanmamış işler | – |
| Bu Hafta | Pazartesi planlamasında bu sprint'e alınanlar | Ekip, planlamada |
| In Progress | Biri issue'yu kendine atadı ve çalışıyor | İşi alan kişi |
| Review | PR açıldı, onay bekliyor | PR'ı açan kişi |
| Done | PR merge edildi, issue kapandı | Otomatik / PR'ı açan kişi |

## Sprint

- Sprint 1 hafta. Her hafta bir milestone'a karşılık gelir:

  | Milestone | Bitiş |
  |---|---|
  | Hafta 1: Analiz, tasarım, iskelet, auth | 27.10.2026 |
  | Hafta 2: Kurs yönetimi + kayıt | 03.11.2026 |
  | Hafta 3: Ders izleme, ilerleme, sınav oluşturma | 10.11.2026 |
  | Hafta 4: Sınav çözme/puanlama, admin, entegrasyon | 17.11.2026 |
  | Hafta 5: Bug fix, test, seed data, rapor, demo | 24.11.2026 |
  | Sunum: Rapor, sunum ve demo hazırlığı | 01.12.2026 |

- Proje 21.10.2026 Çarşamba başlar. **Sunum ve rapor teslimi 02.12.2026** (rapor hocanın Word şablonunda, .docx).
- **Her pazartesi planlama:** o haftanın milestone'undaki issue'lar "Bu Hafta"ya çekilir, geçen haftadan kalanlar konuşulur.
- Hafta 4 sonu (17.11) **feature freeze**: sonrasında yeni özellik yok, yalnız bug fix, test, seed data, rapor, sunum ve demo.

## İş seçme

- Issue'lar kimseye atanmış olarak açılmaz; herkes işi **"Bu Hafta" kolonundan kendisi seçer**, issue'yu kendine atar ve "In Progress"e taşır.
- **Önce P1 (milestone bazında):** o haftanın milestone'undaki P1'ler bitmeden o haftanın P2'si alınmaz. P3 / `nice-to-have` işler yalnız MVP bittikten sonra.
- **WIP limiti:** kişi başı aynı anda en fazla **2** issue (In Progress + Review toplamı).
- Bir işe başlamadan önce "Blocked by #X" satırına bak; bağımlı olduğu issue kapanmadan başlama.
- Ekip dışından gelen bir engel varsa (hoca onayı, erişim, karar bekleniyor) issue'ya elle `blocked` etiketi ekle ve nedenini yorum olarak yaz. Bağımlılık zaten issue metninde yazdığı için `blocked` etiketi bunun için kullanılmaz.
- 1 günden büyük çıkan bir işi böl: yeni issue aç, ilkine "Blocked by" ile bağla.

## Branch ve commit

- `main`'e doğrudan push yok; her şey PR ile gelir.
- Branch adı:
  - özellik: `feature/#<issue-no>-kisa-ad` → `feature/#12-auth-register`
  - hata: `fix/#<issue-no>-kisa-ad` → `fix/#71-login-redirect`
- Commit mesajında ve PR açıklamasında `Closes #<no>` bulunur; merge edildiğinde issue otomatik kapanır.
- Branch'i güncel tutmak için `main`'i rebase ya da merge et; PR "Squash and merge" ile birleştirilir.

## Definition of Done

Bir issue ancak şunların hepsi sağlandığında "Done"dır:

- [ ] PR açık ve issue'ya bağlı (`Closes #<no>`)
- [ ] En az 1 review onayı (modül sorumlusundan)
- [ ] CI yeşil (backend build + test, frontend lint + build)
- [ ] Issue'daki kabul kriterlerinin tamamı işaretli
- [ ] Gerekiyorsa docs / diyagram / OpenAPI sözleşmesi güncel

## Roller

Dersin önerdiği rol dağılımı. Herkes kod yazar; rol yalnız o alanın koordinasyonundan ve çıktılarından kimin sorumlu olduğunu belirtir. Gerekirse roller değiştirilebilir.

| Rol | Temel sorumluluk | Beklenen çıktı | Kişi |
|---|---|---|---|
| Yazılım Mimarı | Mimari yaklaşım, bileşen sınırları, mimari kararlar | Mimari diyagramlar (C4, bileşen, dağıtım), ADR'ler | |
| Alan Tasarımcısı | Alan modeli, sınıflar, nesne yönelimli tasarım, tasarım desenleri | Sınıf ve sekans diyagramları, desen/SOLID açıklamaları | |
| Veri ve Entegrasyon Sorumlusu | Veritabanı, API tasarımı, dış servis entegrasyonları | ER diyagramı, OpenAPI sözleşmesi | |
| Kalite ve Süreç Sorumlusu | Testler, kod kalitesi, Git iş akışı, proje takibi | Test planı ve raporu, CI çıktıları, haftalık ilerleme raporları | |

## Bireysel katkı

Not, grup notuna bireysel katkı katsayısı (0,5–1,1) uygulanarak verilir. Katsayı Git geçmişine, geliştirilen modüllere ve sunumdaki teknik sorulara verilen cevaplara göre belirlenir. Bu yüzden:

- **Herkes her hafta kendi adına en az bir PR merge ettirir.**
- **Herkes en az bir modülü backend ve frontend tarafıyla baştan sona geliştirir** (bkz. modül sorumluları).
- Review'lar modüller arasında dönüşümlü yapılır; herkes kendi yazmadığı modülleri de sunumda anlatabilecek kadar bilir.
- Commit'ler kendi GitHub hesabınla ve kendi e-postanla atılır (`git config user.email` GitHub hesabındakiyle aynı olmalı; yoksa katkı profilinde görünmez).
- PR'lar squash merge ile birleşir; merge commit'inin author'ı PR'ı açan kişidir, katkı izlenebilir kalır. Birlikte çalıştığınız bir PR'da diğer kişiyi `Co-authored-by:` satırıyla ekleyin.

## Review

- Her modülün bir sorumlusu var; o modülün PR'larını o review eder. Birden çok modüle dokunan PR'da ilgili sorumluların her biri bakar.
- Review 24 saat içinde yapılır. Sorumlu müsait değilse başka bir ekip üyesi onaylayabilir.
- Kendi PR'ını onaylayamazsın.

### Modül sorumluları

| Modül | Etiket | Sorumlu |
|---|---|---|
| Auth | `mod:auth` | |
| Kurs | `mod:course` | |
| Kayıt / ilerleme | `mod:enrollment` | |
| Sınav | `mod:quiz` | |
| Admin | `mod:admin` | |
| Docs / DevOps | `docs`, `devops` | |

## Mimari kurallar (özet)

- Backend modül bazlı paketlenir: `com.miniudemy.<modül>.{controller,service,repository,dto,mapper,domain}`.
- Controller yalnız DTO alır/döner; entity (domain) API'ye çıkmaz. Dönüşüm mapper'da yapılır.
- İş kuralı service katmanında; controller ve repository'de iş kuralı olmaz.
- Şema değişikliği yalnız Flyway migration'ı ile yapılır (`backend/src/main/resources/db/migration/README.md`).
- Bilinçli kullanılan desenler: puanlamada Strategy, ilerleme güncellemede Observer (Spring events), kurs yaşam döngüsünde State, DTO + Mapper. Ayrıntı: `docs/architecture.md`.
- Önemli her mimari karar `docs/adr/` altında bir ADR olarak yazılır (bağlam, karar, alternatifler, sonuçlar).
- Katman ve modül kuralları ArchUnit testleriyle CI'da kontrol edilir.

## Etiketler

| Etiket | Kullanım |
|---|---|
| `backend`, `frontend`, `docs`, `devops` | Alan |
| `P1`, `P2`, `P3` | Öncelik (aynı milestone'da P1'ler bitmeden P2 alınmaz) |
| `nice-to-have` | MVP dışı |
| `bug` | Hata |
| `blocked` | Ekip dışı bir engel var (elle) |
| `design-pattern` | Ders için önemli desen uygulaması |
| `mod:auth`, `mod:course`, `mod:enrollment`, `mod:quiz`, `mod:admin` | Modül (review sorumlusu buna göre) |
