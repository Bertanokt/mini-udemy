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

- **Rol = kişi.** Her issue'nun tek bir rol etiketi var (`rol:mimar`, `rol:alan`, `rol:veri`, `rol:kalite`); o rolü üstlenen üye o issue'yu yapar. Frontend issue'ları ilgili backend işinin rolündedir.
- Issue'lar atanmış olarak açılmaz; üye **"Bu Hafta" kolonundan kendi rol etiketli işini** seçer. Rolünün işi bitmişse ya da takıldıysa planlamada başka bir rolün işini devralabilir (etiketi değiştirerek).
- **İşe başlarken iki adım zorunlu:** issue'yu alan **kendini assignee yapar** ve board'da **kartı "In Progress"e taşır**. Assignee'si olmayan bir issue'da kimse çalışmıyor sayılır; aynı işi iki kişinin yapmasını bu önler.
  ```bash
  gh issue edit <no> --add-assignee @me
  ```
  Kartı board'da (Mini Udemy project) sürükleyerek "In Progress"e taşı. İşi bırakırsan assignee'yi kaldır ve kartı "Bu Hafta"ya geri al.
- **Önce P1 (milestone bazında):** o haftanın milestone'undaki P1'ler bitmeden o haftanın P2'si alınmaz.
- **P2 kesme kuralı:** Hafta 4 sprint planlamasında (11.11) Hafta 3'ün P1'leri bitmemişse Hafta 4'ün P2'leri "not planned" olarak kapatılır, kaydırılmaz.
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
- **Aynı modülde birbirine bağlı küçük issue'lar tek PR'da kapatılabilir:** `Closes #a #b #c` (her biri ayrı `Closes` ile de yazılabilir: `Closes #a, closes #b`). Branch adı ilk issue'nun numarasını taşır. Farklı modüllere dokunan işler ayrı PR olur.
- PR açıklamasındaki **"Ne / Neden" bölümünü PR sahibi kendi cümleleriyle yazar** (Claude Code ya da başka bir araç bu bölümü yazmaz). Sunumda kodunu anlatabilmen buna bağlı.
- Branch'i güncel tutmak için `main`'i rebase ya da merge et; PR "Squash and merge" ile birleştirilir.

## Definition of Done

Bir issue ancak şunların hepsi sağlandığında "Done"dır:

- [ ] PR açık ve issue'ya bağlı (`Closes #<no>`)
- [ ] En az 1 review onayı (modül sorumlusundan)
- [ ] CI yeşil (backend build + test, frontend lint + build)
- [ ] Issue'daki kabul kriterlerinin tamamı işaretli
- [ ] Kod değiştiyse ilgili diyagram (ER, sınıf, sekans, bileşen) ve OpenAPI aynı PR'da güncellendi
- [ ] Yeni mimari karar varsa ADR yazıldı

## Roller

Dersin önerdiği rol dağılımı. **Her üye bir rol üstlenir ve rol etiketli issue'ların sahibidir** (rol = kişi). Rol etiketleri kod issue'larını da kapsar; herkes kod yazar. Backlog kişi başı 33–38 issue olacak şekilde dengelendi.

| Rol | Temel sorumluluk | Beklenen çıktı | Kişi | Etiket |
|---|---|---|---|---|
| Yazılım Mimarı | Mimari yaklaşım, bileşen sınırları, mimari kararlar | Mimari diyagramlar (C4, bileşen, dağıtım), ADR'ler | Bertan (@Bertanokt) | `rol:mimar` |
| Alan Tasarımcısı | Alan modeli, sınıflar, nesne yönelimli tasarım, tasarım desenleri | Sınıf ve sekans diyagramları, desen/SOLID açıklamaları | Sedat (@sedatisener) | `rol:alan` |
| Veri ve Entegrasyon Sorumlusu | Veritabanı, API tasarımı, dış servis entegrasyonları | ER diyagramı, OpenAPI sözleşmesi | Eray (@eraygame113-beep) | `rol:veri` |
| Kalite ve Süreç Sorumlusu | Testler, kod kalitesi, Git iş akışı, proje takibi | Test planı ve raporu, CI çıktıları, haftalık ilerleme raporları | Veronika (@vliashenko6) | `rol:kalite` |

## Bireysel katkı

Not, grup notuna bireysel katkı katsayısı (0,5–1,1) uygulanarak verilir. Katsayı Git geçmişine, geliştirilen modüllere ve sunumdaki teknik sorulara verilen cevaplara göre belirlenir. Bu yüzden:

- **Herkes her hafta kendi adına en az bir PR merge ettirir.**
- **Herkes en az bir modülü backend ve frontend tarafıyla baştan sona geliştirir** (frontend issue'ları ilgili backend işinin rolünde olduğu için bu kendiliğinden sağlanır).
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
| Kullanıcı ve yetkilendirme | `mod:auth` | |
| Kurs, kategori, katalog, eğitmen paneli | `mod:course` | |
| Ders içerikleri ve dosyalar | `mod:content` | |
| Kursa kayıt | `mod:enrollment` | |
| İzleme ve ilerleme | `mod:progress` | |
| Sınav | `mod:quiz` | |
| Sertifika | `mod:certificate` | |
| Puanlama ve yorumlar | `mod:review` | |
| Bildirimler | `mod:notification` | |
| Yönetici paneli | `mod:admin` | |
| Docs / DevOps | `docs`, `devops` | |

## Mimari kurallar (özet)

- Backend modül bazlı paketlenir: `com.miniudemy.<modül>.{controller,service,repository,dto,mapper,domain}`. Modüller: auth, course, content, enrollment, progress, quiz, certificate, review, notification, admin (+ common).
- Bir modül başka modülün yalnız service arayüzünü ya da domain olaylarını kullanır; başka modülün repository'sine ve entity'sine erişmez.
- Controller yalnız DTO alır/döner; entity (domain) API'ye çıkmaz. Dönüşüm mapper'da yapılır.
- İş kuralı service katmanında; controller ve repository'de iş kuralı olmaz.
- Şema değişikliği yalnız Flyway migration'ı ile yapılır; dosya adı zaman damgalı: `V<yyyyMMddHHmm>__aciklama.sql` (`backend/src/main/resources/db/migration/README.md`).
- Bilinçli kullanılan desenler: kurs yayın durumlarında State, kayıt/ders/onay/red/sınav/sertifika olaylarında Observer (Spring events), sınav puanlamada Strategy, ders içerik türlerinde Factory Method, katman sınırlarında DTO + Mapper. Ayrıntı: `docs/architecture.md`.
- Önemli her mimari karar `docs/adr/` altında bir ADR olarak yazılır (bağlam, karar, alternatifler, sonuçlar).
- Katman ve modül kuralları ArchUnit testleriyle CI'da kontrol edilir.
- Geliştirme Claude Code ile yapılıyorsa kurallar `CLAUDE.md`'de, her issue için kullanılacak akış ve prompt şablonu `docs/claude-code-workflow.md`'de.

## Etiketler

| Etiket | Kullanım |
|---|---|
| `backend`, `frontend`, `docs`, `devops` | Alan |
| `P1`, `P2`, `P3` | Öncelik. P1: proje dokümanındaki 5 senaryo, Bölüm 6 test durumları ve tasarım çıktıları; P2: senaryo dışı ayrıntılar. Aynı milestone'da P1'ler bitmeden P2 alınmaz |
| `rol:mimar`, `rol:alan`, `rol:veri`, `rol:kalite` | Issue'nun sahibi olan rol (= kişi) |
| `nice-to-have` | MVP dışı |
| `bug` | Hata |
| `blocked` | Ekip dışı bir engel var (elle) |
| `design-pattern` | Ders için önemli desen uygulaması |
| `mod:auth`, `mod:course`, `mod:content`, `mod:enrollment`, `mod:progress`, `mod:quiz`, `mod:certificate`, `mod:review`, `mod:notification`, `mod:admin` | Modül (review sorumlusu buna göre) |
