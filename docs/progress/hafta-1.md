# İlerleme raporu — Hafta 1 (21.10–27.10.2026)

**Milestone:** Hafta 1: Analiz, tasarım, iskelet, auth (bitiş 27.10.2026)
**Rapor tarihi:** 14.11.2026
**Veri tarihi:** 14.11.2026 14:38 (İstanbul saati). Kaynak: GitHub API (milestone, issue, PR, CI çalışmaları) ve `git log origin/main`.

Bu rapor hafta bittikten sonra yazıldı. Hafta sonundaki durum (27.10) ile bugünkü durum (14.11) ayrı sütunlarda verilmiştir; geriye dönük tarih atılmamıştır. Board sütun geçmişi kayıtlı olmadığından planlanan iş listesi milestone'dan alınmıştır.

## 1. Özet

| | 27.10.2026 (hafta sonu) | 14.11.2026 (bugün) |
|---|---|---|
| Planlanan issue (milestone) | 27 | 27 |
| Tamamlanan (kapanan) issue | 0 | 13 |
| Devreden (açık) issue | 27 | 14 |
| Merge edilen PR | 0 | 18 (11–13.11 arası) |
| `main`'deki test metodu (`@Test`) | 1 | 23 |
| `main`'de son CI durumu | yeşil | yeşil |

## 2. Tamamlanan issue'lar

Hafta içinde (21–27.10) kapanan issue yok. Aşağıdakiler hafta bittikten sonra kapandı:

| Issue | Başlık | Rol | Kapanış | PR |
|---|---|---|---|---|
| #1 | Use-case diyagramı | alan | 11.11.2026 | #152 |
| #2 | ER diyagramı | veri | 11.11.2026 | #153 |
| #3 | API sözleşmesi: auth, profil, parola yenileme | veri | 11.11.2026 | #154 |
| #6 | Mimari kurallar | mimar | 11.11.2026 | #148 |
| #77 | Fonksiyonel gereksinimler | alan | 11.11.2026 | #151 |
| #78 | Fonksiyonel olmayan gereksinimler | kalite | 11.11.2026 | #156 |
| #80 | ADR altyapısı + ADR-001 | mimar | 11.11.2026 | #149 |
| #4 | API sözleşmesi: kategori, kurs, bölüm, ders, içerik, katalog | veri | 13.11.2026 | #165 |
| #5 | API sözleşmesi: kayıt, ilerleme, sınav, sertifika | veri | 13.11.2026 | #165 |
| #108 | API sözleşmesi: yorum, bildirim, eğitmen ve yönetici paneli | veri | 13.11.2026 | #165 |
| #7 | Flyway migration: users | veri | 13.11.2026 | #162 |
| #20 | Flyway migration: categories, courses, sections, lessons | veri | 13.11.2026 | #163 |
| #11 | Global exception handler + standart hata gövdesi | mimar | 13.11.2026 | #159, #160, #161 |

## 3. Devreden issue'lar (14.11.2026 itibarıyla açık)

| Issue | Başlık | Rol | Öncelik |
|---|---|---|---|
| #8 | User entity + UserRepository | alan | P1 |
| #9 | Desen: DTO + Mapper altyapısı | alan | P1 |
| #21 | Category / Course / Section / Lesson entity + repository | alan | P1 |
| #79 | Kullanım senaryoları | alan | P1 |
| #10 | Spring Security + JWT altyapısı | mimar | P1 |
| #12 | POST /api/auth/register | mimar | P1 |
| #13 | POST /api/auth/login | mimar | P1 |
| #14 | GET /api/me + rol bazlı yetkilendirme | mimar | P1 |
| #88 | UI taslak çizimleri | mimar | P2 |
| #26 | Katalog + sayfalama; kurs detayı | veri | P1 |
| #109 | Kategori listesi API'si + başlangıç kategorileri | veri | P1 |
| #15 | AuthService unit testleri | kalite | P1 |
| #83 | İlerleme raporu – Hafta 1 (bu rapor) | kalite | P1 |
| #82 | Gantt şeması, görev dağılımı ve risk analizi | kalite | P2 |

## 4. Kişi bazında merge edilen PR'lar

| Kişi | 21–27.10 arası | 14.11.2026'ya kadar toplam |
|---|---|---|
| Bertan | 0 | 7 (#148, #149, #150, #158, #159, #160, #161) |
| Eray | 0 | 7 (#153, #154, #155, #162, #163, #165, #166) |
| Sedat | 0 | 2 (#151, #152) |
| Veronika | 0 | 2 (#156, #157) |

Hafta içinde `main`'e PR'sız 4 commit girdi (Bertan; 22–23.10): backlog'un proje tanım dokümanına göre yeniden planlanması, Claude Code kuralları, ekip kurulum script'i ve `CONTRIBUTING.md` rol tablosu.

## 5. CI ve testler

- Hafta içinde `main`'de 3 CI çalışması oldu (22.10 ×1, 23.10 ×2), hepsi yeşil.
- 27.10'da `main`'de 1 test metodu vardı (`HealthControllerTest`). 14.11'de 23 test metodu var (`GlobalExceptionHandlerTest` 22, `HealthControllerTest` 1); hepsi `common` modülünde. Auth modülüne ait test henüz yok (#15, auth uçlarını bekliyor).

## 6. Karşılaşılan sorunlar

- Hafta 1 milestone'unda hafta içinde hiçbir issue kapanmadı; ilk PR'lar 11.11'de açıldı. Doküman ve tasarım işleri 11–13.11'de toplu halde tamamlandı.
- 11.11'de merge edilen ilk 4 PR (#148, #149, #151, #152) review onayı olmadan birleştirildi (katkı raporu, #150). "En az 1 onay" kuralı yerine "CI yeşil + Kalite ve Süreç sorumlusunun hızlı kontrolü" kuralı getiriliyor (#84).

## 7. Gelecek haftanın riski

- Auth zinciri (#8 → #10 → #12/#13 → #14) açık; kurs, kayıt ve test işlerinin çoğu bu zincire bağlı. Feature freeze 17.11.2026.
- Coverage raporu (#89) henüz `main`'de yok; test planı (#92) ve servis testleri auth ve kurs kodunu bekliyor.
