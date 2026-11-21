# Kullanılan kütüphaneler, araçlar ve dış kaynaklar

Proje raporu için zorunlu liste (#100). Sürümler ve lisanslar elle yazılmadı, ölçüldü (20.11.2026, `main`):
- **Backend sürümleri:** `./mvnw dependency:list -DexcludeTransitive=true` (Maven'ın çözdüğü doğrudan bağımlılıklar).
- **Backend lisansları:** her paketin kendi POM'undaki `<licenses>` alanı.
- **Frontend:** `package.json` ve kurulu paketin `node_modules/<paket>/package.json`'u (kurulu sürüm ve `license`).

Bir bağımlılık eklenince ya da sürümü değişince bu sayfa aynı PR'da güncellenir (CLAUDE.md › Kod değişirse doküman da değişir). Dolaylı (transitive) bağımlılıklar listelenmez.

## 1. Backend (Java 21, Maven, `backend/pom.xml`)

Sürümlerin çoğu Spring Boot ebeveyninden (`spring-boot-starter-parent` 3.5.16) gelir; `pom.xml`'de ayrıca sabitlenenler MapStruct (1.6.3), ArchUnit (1.4.1) ve JaCoCo (0.8.13).

| Bağımlılık | Sürüm | Kapsam | Lisans | Ne için |
|---|---|---|---|---|
| `spring-boot-starter-web` | 3.5.16 | derleme | Apache-2.0 | REST uçları (Spring MVC), JSON (Jackson), gömülü Tomcat |
| `spring-boot-starter-validation` | 3.5.16 | derleme | Apache-2.0 | İstek DTO'larında Bean Validation (`@Valid`, alan bazlı 400) |
| `spring-boot-starter-data-jpa` | 3.5.16 | derleme | Apache-2.0 | Entity ve repository'ler (Hibernate, Spring Data JPA) |
| `spring-boot-starter-security` | 3.5.16 | derleme | Apache-2.0 | Kimlik doğrulama ve yetki filtresi, BCrypt parola özeti |
| `spring-boot-starter-oauth2-resource-server` | 3.5.16 | derleme | Apache-2.0 | JWT (HS256) üretme ve doğrulama (Nimbus) (ADR-002, ADR-012) |
| `spring-boot-starter-actuator` | 3.5.16 | derleme | Apache-2.0 | `/actuator/health` (Docker sağlık kontrolü) |
| `flyway-core`, `flyway-database-postgresql` | 11.7.2 | derleme | Apache-2.0 | Şema migration'ları (ADR-003) |
| `postgresql` (JDBC sürücüsü) | 42.7.11 | çalışma | BSD-2-Clause | PostgreSQL bağlantısı |
| `mapstruct` (+ derleyici eklentisi `mapstruct-processor`) | 1.6.3 | derleme | Apache-2.0 | Entity ↔ DTO dönüşümü, derleme zamanında üretilir (ADR-006) |
| `spring-boot-starter-test` | 3.5.16 | test | Apache-2.0 | JUnit 5, Mockito, AssertJ, MockMvc |
| `spring-security-test` | 6.5.11 | test | Apache-2.0 | Güvenlik ayarıyla controller testleri |
| `spring-boot-testcontainers`, `testcontainers:postgresql` | 3.5.16, 1.21.4 | test | Apache-2.0, MIT | Entegrasyon testleri gerçek PostgreSQL 17 konteynerine karşı (`*IT`) |
| `archunit-junit5` | 1.4.1 | test | Apache-2.0 | Mimari kural testleri: katmanlar, modül bağımlılıkları (#86) |

**Derleme eklentileri:**

| Eklenti | Sürüm | Lisans | Ne için |
|---|---|---|---|
| `spring-boot-maven-plugin` | 3.5.16 | Apache-2.0 | Çalıştırılabilir jar, `spring-boot:run` |
| `maven-compiler-plugin` | 3.14.1 | Apache-2.0 | Java 21 derleme, MapStruct işlemcisi |
| `maven-surefire-plugin` / `maven-failsafe-plugin` | 3.5.6 | Apache-2.0 | Birim testleri (`*Test`) / entegrasyon testleri (`*IT`) |
| `jacoco-maven-plugin` | 0.8.13 | EPL-2.0 | Test kapsamı raporu |

## 2. Frontend (Next.js, `frontend/package.json`)

Çalışma zamanında yalnız Next.js ve React kullanılır; başka bir UI, durum yönetimi ya da HTTP kütüphanesi yoktur (API çağrıları `fetch`/`XMLHttpRequest` ile `src/lib/api/*`'de).

| Paket | Kurulu sürüm (`package.json`) | Tür | Lisans | Ne için |
|---|---|---|---|---|
| `next` | 16.4.6 (`16.4.6`) | çalışma | MIT | Uygulama çatısı: sayfalar (App Router), derleme, CSP başlıkları |
| `react`, `react-dom` | 19.3.0 (`19.3.0`) | çalışma | MIT | Arayüz bileşenleri |
| `typescript` | 5.9.3 (`^5`) | geliştirme | Apache-2.0 | Tip denetimi |
| `@types/node`, `@types/react`, `@types/react-dom` | 20.19.43, 19.3.0, 19.3.0 | geliştirme | MIT | TypeScript tip tanımları |
| `eslint`, `eslint-config-next` | 9.39.5, 16.4.6 | geliştirme | MIT | Lint kuralları (CI'da `npm run lint`) |

## 3. Araçlar ve çalışma ortamı

| Araç | Sürüm | Nerede | Ne için |
|---|---|---|---|
| Docker, Docker Compose | — | `docker-compose.yml`, `*/Dockerfile` | Uygulamanın tamamını tek komutla çalıştırma (NFR-16) |
| PostgreSQL imajı | `postgres:17-alpine` | `docker-compose.yml`, Testcontainers | Veritabanı |
| Java imajları | `eclipse-temurin:21-jdk` (derleme), `eclipse-temurin:21-jre` (çalışma) | `backend/Dockerfile` | Backend imajı |
| Node imajı | `node:22-alpine` | `frontend/Dockerfile` | Frontend imajı (Next.js standalone) |
| GitHub Actions | `actions/checkout@v4`, `actions/setup-java@v4` (Java 21), `actions/setup-node@v4` (Node 22), `actions/upload-artifact@v4` | `.github/workflows/ci.yml` | Her PR'da backend `./mvnw verify` ve frontend `npm run lint && npm run build` |
| GitHub Projects, Issues, PR | — | Mini Udemy board | Planlama, iş takibi, kod incelemesi |
| PlantUML | 1.2026.9 (smetana düzeni, Graphviz gerekmez) | `docs/diagrams/*.puml` | C4, ER, sınıf, sekans, bileşen, dağıtım ve use-case diyagramları; C4 için PlantUML'in standart kütüphanesindeki C4-PlantUML (`!include <C4/...>`) |
| Mermaid | GitHub'ın yerleşik çizimi | `docs/architecture.md`, `docs/diagrams/gantt.mmd` | Modül bağımlılıkları özeti, Gantt planı |
| OpenAPI | 3.0.3 | `docs/api/openapi.yaml` | API sözleşmesi |

## 4. Faydalanılan dokümanlar ve kaynaklar

- **Proje tanım dokümanı** (ders sorumlusu): kapsamın tek kaynağı; modüller A–L, senaryo 1–5, bölüm 5–7 (`CLAUDE.md`).
- **Çatıların resmî dokümantasyonu:**
  - Spring Boot, Spring Security, Spring Data JPA referansları;
  - Flyway, MapStruct, Testcontainers, ArchUnit kullanım kılavuzları;
  - Next.js dokümantasyonu: kurulu sürümle gelen `node_modules/next/dist/docs/` (`frontend/AGENTS.md`) ve CSP rehberi (`frontend/next.config.ts`, ADR-007).
- **OpenAPI 3.0 belirtimi:** sözleşmenin biçimi.
- **C4 modeli:** sistem bağlamı ve container diyagramları (#87).
- **Mimari karar kaydı (ADR) yaklaşımı:** şablon `docs/adr/0000-sablon.md` (bağlam, karar, değerlendirilen alternatifler, sonuçlar).

## 5. Yapay zekâ araçları

Ekip, kod ve doküman yazımında **Claude Code**'u (Anthropic) kullandı.

- **Nasıl kullanıldı:** her issue aynı akışla ilerledi: issue'yu oku → plan → üyenin onayı → uygula → test → PR (`docs/claude-code-workflow.md`). Kod yazılmadan önce plan üyeye onaylatıldı. Hoca her üyeye kendi kodu hakkında soru soracağı için planı okuyup onaylayan ve testleri yorumlayan üyenin kendisi.
- **Kurallar:** repo kökündeki `CLAUDE.md`. Mimari, katmanlar, desenler, hata biçimi, test beklentisi, "kod değişirse doküman da değişir" tablosu orada; Claude Code bu dosyayı her oturumda okur. Mimari kurallar ayrıca ArchUnit testleriyle CI'da denetlenir; kuralı gevşeterek testi geçirmek yasak.
- **Neler yaptırıldı:**
  - backend ve frontend kodu, birim ve entegrasyon testleri;
  - OpenAPI, ADR ve diyagram güncellemeleri;
  - PR açıklamalarının "Ne / Neden" bölümü (her teknik iddia kodla karşılaştırılarak);
  - kod incelemesi (review).
- **İnsan denetimi:**
  - her PR CI'dan (derleme, tüm testler, ArchUnit, lint) geçer ve Kalite ve Süreç sorumlusunun hızlı kontrolünden sonra merge edilir (CONTRIBUTING.md › Definition of Done);
  - testlerin doğru sebeple kırıldığı, kod geçici bozularak gösterilir (CLAUDE.md › Test beklentisi).
- **İzlenebilirlik:** commit mesajındaki `Co-Authored-By: Claude …` satırı, commit'in Claude Code'la birlikte yazıldığını gösterir. Ölçüm (20.11.2026, `main`): 139 commit'in 61'inde bu satır var. Üye başına ayrıntı: `docs/katki/README.md`.
- **Sınırlar:** gizli bilgiler (parola, token, JWT anahtarı) repoya konmaz, ortam değişkeninden okunur. Yeni bağımlılık sormadan eklenmez; test, ArchUnit ya da lint kuralı gevşetilerek "yeşil" yapılmaz (CLAUDE.md › Yapma).
