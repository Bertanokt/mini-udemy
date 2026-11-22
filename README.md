# Mini Udemy

Yazılım Mimarisi dersi için online kurs platformu. Eğitmenler kurs → bölüm → ders oluşturur, öğrenciler kayıt olup dersleri takip eder ve çoktan seçmeli sınavları çözer, admin kursları onaylar.

| Katman | Teknoloji |
|---|---|
| Backend | Spring Boot 3.5 (Java 21), Spring Data JPA, Flyway |
| Veritabanı | PostgreSQL 17 |
| Frontend | Next.js 16 (TypeScript, App Router) |
| Çalıştırma | Docker Compose |
| CI | GitHub Actions |

## Özellikler

| Rol | Ne yapabilir |
|---|---|
| Ziyaretçi | Yayımlanmış kursları arar ve süzer (kategori, seviye), kurs ayrıntısını ve değerlendirmeleri görür, kayıt olur, sertifikayı doğrulama koduyla girişsiz doğrular (`/verify/{kod}`), parolasını sıfırlar |
| Kursiyer | Kursa kaydolur; ders oynatıcıda video izler (ileri sarma), PDF ve doküman açar; dersi tamamlandı işaretler (bölüm ve kurs ilerleme çubukları); kaldığı yerden devam eder; bölüm sınavlarını çözer (puan, geçti/kaldı, tekrar deneme, deneme geçmişi); zorunlu dersler ve sınavlar bitince sertifikasını görür ve yazdırır; kursa puan ve yorum verir |
| Eğitmen | Kurs oluşturur (kategori, seviye, öğrenme hedefleri, kapak); bölüm ve ders ekler, sıralar, içerik yükler (video, PDF, doküman) ve önizler; soru bankası ve bölüm sınavı kurar; kursu onaya gönderir ya da yayından kaldırır; kurs analitiğini görür (kayıt, ilerleme, tamamlanma, sınav başarısı, puan dağılımı, kursiyer listesi) |
| Yönetici | Kursları inceler, onaylar ya da gerekçeyle reddeder, yayından kaldırır; yorumları gizler ve geri açar; şikâyetleri sonuçlandırır; kullanıcıları (rol, pasifleştirme) ve kategorileri yönetir; platform raporunu görür |
| Herkes (giriş yapmış) | Bildirim zili (okunmamış sayısı, liste, okundu işaretleme), profil (ad soyad, kısa biyografi) |

Mimari: modüler monolit (10 modül, sınırlar ArchUnit testleriyle CI'da); State, Strategy, Factory Method, Observer ve DTO + Mapper desenleri. Ayrıntı: [docs/architecture.md](docs/architecture.md), [docs/solid.md](docs/solid.md), diyagramlar [docs/README.md](docs/README.md)'de.

**Demo:** beş senaryo adım adım ve her adımda gösterilen mimari nokta: [docs/demo.md](docs/demo.md). Hesaplar aşağıda (› Demo hesapları).

### Ekran görüntüleri

⟨24.11 kod dondurmasından sonra `docs/screenshots/` altına eklenecek (#72): katalog, kurs detayı, oynatıcı, sınav sonucu, sertifika, eğitmen analitiği, yönetici kurs onayı, bildirim zili.⟩

## Klasör yapısı

```
backend/    Spring Boot uygulaması (modül bazlı paketler: auth, course, content, enrollment, progress, quiz, certificate, review, notification, admin, common)
frontend/   Next.js uygulaması (src/lib/api: API client katmanı, src/components/layout: ortak layout)
docs/       Diyagramlar, OpenAPI sözleşmesi, mimari, test planı ve raporu, demo senaryosu (dizin: docs/README.md)
rapor/      Proje raporu bölümleri (Word şablonunda birleştirilecek)
scripts/    GitHub kurulum script'leri (etiket, milestone, issue, board)
```

## Hızlı başlangıç (her şey Docker'da)

Gereken: Docker Desktop.

```bash
docker compose up --build
```

İlk build temiz makinede uzun sürer (~22 dk; Maven ve npm bağımlılıkları iner), sonraki build'ler önbellekle saniyeler içinde biter. Temiz kurulumda beş senaryo 21.11.2026'da baştan sona denendi (#70).

| Servis | Adres |
|---|---|
| Frontend | http://localhost:3000 |
| Backend health | http://localhost:8080/api/health |
| Actuator | http://localhost:8080/actuator/health |
| PostgreSQL | `localhost:5432`, db/kullanıcı/şifre: `miniudemy` |

Durdurmak: `docker compose down`. Veritabanını da silmek: `docker compose down -v`.

### Demo hesapları (seed verisi, #67)

`dev` profilinde (docker compose ve yerel geliştirme) uygulama **boş** bir veritabanıyla açılırsa örnek veri otomatik yüklenir (`DemoSeedRunner`, `backend/src/main/resources/seed/`). Veritabanında kullanıcı varsa hiçbir şeye dokunulmaz; baştan yüklemek için `docker compose down -v`. `prod` ve `test` profillerinde yüklenmez.

Bütün hesapların parolası: **`Demo1234!`** (yalnız yerel demo için).

| Rol | E-posta | Ne görülür |
|---|---|---|
| Yönetici | `admin@miniudemy.dev` | Onay kuyruğunda "SQL ve Veritabanları" (İncelemede); şikâyetlerde Selin'in açık şikâyeti |
| Eğitmen | `mehmet.kaya@miniudemy.dev` | "Sıfırdan Python" ve "Pandas ile Veri Analizine Giriş" (Yayımlandı), "SQL ve Veritabanları" (İncelemede), "Python 2 ile Programlama (eski)" (Yayından kaldırıldı) |
| Eğitmen | `zeynep.ak@miniudemy.dev` | "Java ile Nesne Yönelimli Programlama", "Arayüz Tasarımının Temelleri", "Git ile Sürüm Kontrolü" (Yayımlandı), "Web Geliştirmeye Giriş" (Taslak, red gerekçesiyle) |
| Eğitmen | `ece.yildiz@miniudemy.dev` | "İş İngilizcesi: E-posta ve Toplantılar", "Scrum ile Çevik Proje Yönetimi" (Yayımlandı) |
| Kursiyer | `ayse.yilmaz@miniudemy.dev` | Python %100 + sertifika (`K7Q2-M9XA-4T`), Java %0, Scrum %33,33 |
| Kursiyer | `can.demir@miniudemy.dev` | Python %66,67 (giriş sınavını geçti), Pandas %44,44 (sınavı geçti) |
| Kursiyer | `elif.sahin@miniudemy.dev` | Python %50 (giriş sınavından kaldı), Arayüz Tasarımı %22,22 |
| Kursiyer | `burak.celik@miniudemy.dev` | Python %16,67, Git %55,56 (sınavından kaldı) |
| Kursiyer | `deniz.arslan@miniudemy.dev` | Python %0 (yeni kayıt) |
| Kursiyer | `selin.koc@miniudemy.dev` | Java %50, Pandas %33,33, İş İngilizcesi %0; Python'a kayıtlı değil |

**Parolamı unuttum (yalnız `dev`):** e-posta gönderilmez (#115). "Parolamı unuttum" sayfasından istenen bağlantı backend loguna yazılır: `docker compose logs backend | grep "Parola yenileme bağlantısı"`. Bağlantı 30 dakika geçerli ve tek kullanımlık. `prod` profilinde bağlantı hiçbir yere yazılmaz.

**İçerik:** 10 kurs (7 yayımlanmış; birer tane incelemede, reddedilmiş taslak ve yayından kaldırılmış), 5 kategori. Her kursta 3–4 bölüm, bölüm başına 3–4 ders, en az iki video ders (720p, 35 sn), PDF ders notları, bir doküman (DOCX) ve en az bir zorunlu bölüm sınavı (4–5 soru). Toplam 94 ders, 11 sınav, 10 değerlendirme, 1 açık şikâyet. "Sıfırdan Python" 4 bölüm ve 12 derstir, 3 değerlendirmesi var (ortalama 4,3). Seed 28 bildirim de yazar (kayıt → eğitmene, onay/red → eğitmene, sınav sonucu ve sertifika → kursiyere); son 7 günün 11 bildirimi okunmamış ve üst menüdeki zilde görünür.

Kapak görselleri, video dersler, PDF ve DOCX dosyaları ile `demo-seed.sql` [`scripts/seed-content/`](scripts/seed-content/README.md) ile üretilir (Chrome, ffmpeg, python3); dosyalar repoda hazır olduğu için demo için betiği çalıştırmak gerekmez.

## Geliştirme ortamı (hot reload)

Gereken: Java 21, Node.js 22+, Docker.

1. Yalnız veritabanını başlat:
   ```bash
   docker compose up -d postgres
   ```
2. Backend (varsayılan profil `dev`):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
3. Frontend:
   ```bash
   cd frontend
   cp .env.example .env.local
   npm install
   npm run dev
   ```

### Testler ve kontroller (CI'ın çalıştırdıkları)

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm run build
```

### Coverage raporu (JaCoCo)

`./mvnw verify` testlerden sonra JaCoCo coverage raporunu üretir:

- **Yerelde:** `backend/target/site/jacoco/index.html` dosyasını tarayıcıda aç.
- **CI'da:** GitHub'da PR'ın ya da commit'in **Checks** sekmesinden CI çalışmasını aç (ya da **Actions** › ilgili çalışma). Sayfanın altındaki **Artifacts** bölümünden `jacoco-report` dosyasını indir, zip'i aç ve `index.html`'i tarayıcıda aç.

## Profiller

| Profil | Ne zaman | Not |
|---|---|---|
| `dev` | Yerel geliştirme (varsayılan) | SQL loglanır, varsayılan DB bilgileri `miniudemy`; JWT anahtarı `application-dev.yml`'daki "dev-only" değer (yalnız yerel) |
| `test` | Testler | DB gerektiren testler Testcontainers ile |
| `prod` | Demo | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ve `JWT_SECRET` (en az 32 bayt, örn. `openssl rand -base64 48`) ortamdan zorunlu; `JWT_SECRET` yoksa ya da kısaysa uygulama açılmaz |

## Çalışma şekli

Akış, branch adları, WIP limiti ve Definition of Done: [CONTRIBUTING.md](CONTRIBUTING.md).
Claude Code kuralları: [CLAUDE.md](CLAUDE.md); her issue için akış ve prompt şablonu: [docs/claude-code-workflow.md](docs/claude-code-workflow.md).

Veritabanı şeması yalnız Flyway ile değişir; migration dosyaları zaman damgalı adlandırılır: `V<yyyyMMddHHmm>__aciklama.sql` (ayrıntı: [backend/src/main/resources/db/migration/README.md](backend/src/main/resources/db/migration/README.md)).

Backlog GitHub Project board'unda ("Mini Udemy"). Issue'lar `scripts/issues.json` içinde tanımlı ve `scripts/create-issues.sh` ile oluşturuldu. Backlog değişiklikleri önce `issues.json`'a yazılır, sonra `scripts/create-issues.sh --sync` ve `scripts/setup-board.sh` çalıştırılır.
