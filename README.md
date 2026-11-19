# Mini Udemy

Yazılım Mimarisi dersi için online kurs platformu. Eğitmenler kurs → bölüm → ders oluşturur, öğrenciler kayıt olup dersleri takip eder ve çoktan seçmeli sınavları çözer, admin kursları onaylar.

| Katman | Teknoloji |
|---|---|
| Backend | Spring Boot 3.5 (Java 21), Spring Data JPA, Flyway |
| Veritabanı | PostgreSQL 17 |
| Frontend | Next.js 16 (TypeScript, App Router) |
| Çalıştırma | Docker Compose |
| CI | GitHub Actions |

## Klasör yapısı

```
backend/    Spring Boot uygulaması (modül bazlı paketler: auth, course, content, enrollment, progress, quiz, certificate, review, notification, admin, common)
frontend/   Next.js uygulaması (src/lib/api: API client katmanı, src/components/layout: ortak layout)
docs/       Diyagramlar, OpenAPI sözleşmesi, mimari rapor
scripts/    GitHub kurulum script'leri (etiket, milestone, issue, board)
```

## Hızlı başlangıç (her şey Docker'da)

Gereken: Docker Desktop.

```bash
docker compose up --build
```

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
| Yönetici | `admin@miniudemy.dev` | Onay kuyruğunda "SQL ve Veritabanları" (İncelemede) |
| Eğitmen | `mehmet.kaya@miniudemy.dev` | "Sıfırdan Python" (Yayımlandı), "SQL ve Veritabanları" (İncelemede), "Python 2 ile Programlama (eski)" (Yayından kaldırıldı) |
| Eğitmen | `zeynep.ak@miniudemy.dev` | "Java ile Nesne Yönelimli Programlama" (Yayımlandı), "Web Geliştirmeye Giriş" (Taslak, red gerekçesiyle) |
| Kursiyer | `ayse.yilmaz@miniudemy.dev` | Python %100 + sertifika (`K7Q2-M9XA-4T`), Java %0 |
| Kursiyer | `can.demir@miniudemy.dev` | Python %66,67, giriş sınavını geçti |
| Kursiyer | `elif.sahin@miniudemy.dev` | Python %50, giriş sınavından kaldı |
| Kursiyer | `burak.celik@miniudemy.dev` | Python %16,67 |
| Kursiyer | `deniz.arslan@miniudemy.dev` | Python %0 (yeni kayıt) |
| Kursiyer | `selin.koc@miniudemy.dev` | Java %50, Python'a kayıtlı değil |

"Sıfırdan Python": 3 bölüm, 6 ders (3 PDF, 1 doküman, 2 video), 4 soruluk soru bankası, 2 zorunlu bölüm sınavı, 3 değerlendirme. Video derslerinin dosyası yok; demoda eğitmen hesabıyla yükleme ucundan (MP4/WebM) eklenir. Bildirimler, bildirim modülü (#124) gelince eklenecek.

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
