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

## Profiller

| Profil | Ne zaman | Not |
|---|---|---|
| `dev` | Yerel geliştirme (varsayılan) | SQL loglanır, varsayılan DB bilgileri `miniudemy` |
| `test` | Testler | DB gerektiren testler Testcontainers ile |
| `prod` | Demo | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ortamdan zorunlu |

## Çalışma şekli

Akış, branch adları, WIP limiti ve Definition of Done: [CONTRIBUTING.md](CONTRIBUTING.md).
Claude Code kuralları: [CLAUDE.md](CLAUDE.md); her issue için akış ve prompt şablonu: [docs/claude-code-workflow.md](docs/claude-code-workflow.md).

Veritabanı şeması yalnız Flyway ile değişir; migration dosyaları zaman damgalı adlandırılır: `V<yyyyMMddHHmm>__aciklama.sql` (ayrıntı: [backend/src/main/resources/db/migration/README.md](backend/src/main/resources/db/migration/README.md)).

Backlog GitHub Project board'unda ("Mini Udemy"). Issue'lar `scripts/issues.json` içinde tanımlı ve `scripts/create-issues.sh` ile oluşturuldu. Backlog değişiklikleri önce `issues.json`'a yazılır, sonra `scripts/create-issues.sh --sync` ve `scripts/setup-board.sh` çalıştırılır.
