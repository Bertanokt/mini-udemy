# Bireysel teknik katkı — Bertan (@Bertanokt), Yazılım Mimarı

<!-- TASLAK: "✍️" ile başlayan satırları kendi cümlelerinle yaz ve sil. Toplam en fazla 1 sayfa. -->

## Geliştirdiğim modüller ve yaptığım işler

- **Repo iskeleti ve CI** — Spring Boot backend (10 modül + common için `controller/service/repository/dto/mapper/domain` paketleri, `GET /api/health`), Next.js frontend (layout, API client), Docker Compose, GitHub Actions (backend build + test, frontend lint + build): [`479fc03`](https://github.com/Bertanokt/mini-udemy/commit/479fc0362aafa99260e881d1d9c8adfcd20727ff)
- **Backlog ve görev panosu** — 147 issue, 6 milestone, rol ve modül etiketleri; `scripts/issues.json` + `create-issues.sh` / `setup-board.sh`: [`5eada73`](https://github.com/Bertanokt/mini-udemy/commit/5eada732421f2f9cd5d3785164a989f33a2b4dab), [`f483d28`](https://github.com/Bertanokt/mini-udemy/commit/f483d2854a1bd96790c615c2b408543f0fa7e014)
- **Çalışma kuralları** — `CONTRIBUTING.md` (akış, Definition of Done, roller, review), `CLAUDE.md`: [`cc49918`](https://github.com/Bertanokt/mini-udemy/commit/cc4991844a78254ce02e298f914660bce24d299c), [`9c849ba`](https://github.com/Bertanokt/mini-udemy/commit/9c849ba391fe6ce9e6cdb632d8742d910fb5b90a)
- **Mimari kurallar** — `docs/architecture.md`: 10 modül, katmanlar, modüller arası iletişim, 5 tasarım deseni: [#148](https://github.com/Bertanokt/mini-udemy/pull/148) (Closes #6)
- **ADR altyapısı ve ADR-001 (modüler monolit)** — `docs/adr/`: [#149](https://github.com/Bertanokt/mini-udemy/pull/149) (Closes #80)
- **Bireysel katkı raporu** — `docs/katki/README.md`: [#150](https://github.com/Bertanokt/mini-udemy/pull/150)
- **Global hata yönetimi** — `common/exception`: tüm hatalar için tek `ErrorResponse` gövdesi, service katmanının istisna sınıfları (400/403/404/409/413/415), 17 senaryolu WebMvcTest: [#159](https://github.com/Bertanokt/mini-udemy/pull/159) (Closes #11)
- **Hata yönetimi düzeltmesi: alan sırası ve null mesaj** — alan hataları geldiği sırayla dönüyor, mesajı null olan alan 500'e yol açmıyor: [#160](https://github.com/Bertanokt/mini-udemy/pull/160)
- **Hata yönetimi düzeltmesi: Spring'in hata kodları** — `ResponseStatusException` ve 406 kendi koduyla dönüyor, hata gövdesi her zaman JSON: [#161](https://github.com/Bertanokt/mini-udemy/pull/161)

## Aldığım mimari kararlar ve nedenleri

- **Modüler monolit (ADR-001):**
  ✍️ Neden mikroservis ya da klasik katmanlı monolit değil? (ADR-001'deki alternatifler)
- **Modüller arası iletişim yalnız service arayüzü ve domain olaylarıyla:**
  ✍️ Bir modül başka modülün repository'sine neden erişmiyor? Bunun faydası ne?
- **Paketleme modül bazlı (`com.miniudemy.<modül>.*`):**
  ✍️ Katmanlara göre değil de modüllere göre paketlemenin nedeni
- **Profil ucu `/api/me`:**
  ✍️ Neden `/api/users/me` değil (#154)
- ✍️ (varsa) Flyway, zaman damgalı migration adları, backlog'un script ile tutulması gibi başka bir karar

## Karşılaştığım teknik problemler

- ✍️ Örnek: takvimin gerisinde kalmak ve 11.11'de PR'ların review beklenmeden merge edilmesi; bundan sonra ne değişecek
- ✍️ Örnek: ekip üyelerinin repo erişimi (süresi dolan davet), commit'lerin doğru hesaba yazılması
- ✍️ Kendi karşılaştığın teknik bir sorun ve nasıl çözdüğün

## Araç kullanımı

✍️ Commit'lerimde `Co-Authored-By: Claude` satırı var: Claude Code'u neyi hazırlamak için kullandığını ve kararların / son kontrolün kimde olduğunu bir cümleyle yaz.
