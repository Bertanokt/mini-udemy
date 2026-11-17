# ADR-007: Next.js'te nonce'suz Content-Security-Policy (`'unsafe-inline'`)

- **Durum:** Kabul edildi
- **Karar tarihi:** 17.11.2026 (#16 üzerinde çalışırken, CSP'nin tarayıcıda ölçülmesinden sonra)
- **Kayıt tarihi:** 17.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #16 (frontend auth ve token saklama), [ADR-002](0002-jwt-stateless-kimlik-dogrulama.md) (token `localStorage`'da; XSS önlemleri), [ADR-005](0005-ayri-frontend-rest-openapi.md) (ayrı frontend), güvenlik denetimi O5 (13.11.2026)

## Bağlam

ADR-002 token'ı frontend'de `localStorage`'da saklamaya karar verdi. Bu durumda bir XSS açığında sayfaya enjekte edilen betik token'ı okuyabilir. ADR-002 bunu üç önlemle küçültmeyi planladı: React'in kaçışlaması + `dangerouslySetInnerHTML` yasağı, kısa token süresi ve "betiklerin yalnız sitenin kendisinden yüklenmesine izin veren" temel bir CSP. ADR-002'nin beklentisi, bu CSP ile "enjekte edilen satır içi ya da dış betik çalışmaz" olmasıydı.

#16'da CSP uygulanırken iki kısıt çıktı:

- **Next.js App Router sayfaya kendi satır içi betiklerini gömer** (sunucu bileşenlerinin verisi, `self.__next_f.push(...)`). `script-src 'self'` bu betikleri de engeller.
- **Projede `cacheComponents` ve `partialPrefetching` açık:** sayfaların kabuğu build'de üretilir. Next.js'in kurulu sürümdeki CSP rehberine göre (`node_modules/next/dist/docs/01-app/02-guides/content-security-policy.md`) nonce kullanmak her sayfayı her istekte üretmeyi gerektirir ve kısmi ön-render (PPR) ile uyumsuzdur.

## Karar

CSP, Next.js rehberindeki **nonce'suz** biçimde uygulanır: `script-src 'self' 'unsafe-inline'`. XSS'e karşı asıl koruma **React'in kaçışlaması ve `react/no-danger` yasağıdır**. CSP, dış betiği ve dışarıya veri gönderilmesini sınırlayan ikinci bir katmandır.

`frontend/next.config.ts`'teki politika:

| Yönerge | Değer | Neden |
|---|---|---|
| `default-src` | `'self'` | Varsayılan: yalnız site |
| `script-src` | `'self' 'unsafe-inline'` (dev'de ayrıca `'unsafe-eval'`) | Next.js'in satır içi betikleri; dış kaynaktan betik yok |
| `style-src` | `'self' 'unsafe-inline'` | Next.js'in satır içi stilleri |
| `connect-src` | `'self'` + API origin'i | `fetch` yalnız site ve API'ye gidebilir |
| `img-src` | `'self' blob: data:` + API origin'i | Kapak görselleri API'den |
| `media-src` | `'self'` + API origin'i | Ders videoları API'den |
| `object-src` | `'none'` | Eklenti içeriği yok |
| `base-uri`, `form-action` | `'self'` | `<base>` ve form hedefi değiştirilemez |
| `frame-ancestors` | `'none'` | Site başka bir sayfaya gömülemez (clickjacking) |

API origin'i `NEXT_PUBLIC_API_URL`'den okunur. `upgrade-insecure-requests` bilerek yok: demo HTTP ile çalışıyor (`http://localhost:8080`); bu yönerge API isteklerini HTTPS'e çevirip kırardı.

## Değerlendirilen alternatifler

17.11.2026'da gerçek backend ve `next build` + `next start` ile, uygulamanın tarayıcısında denendi:

| Yol | Ölçüldü mü | Sonuç |
|---|---|---|
| `script-src 'self'` + Next.js'in deneysel SRI'si (`experimental.sri`) | Evet | **Uygulama çalışmıyor.** Tarayıcı Next.js'in 10 satır içi betiğini engelledi ("Executing inline script violates … `script-src 'self'`"); sayfa "Kurslar yükleniyor…" ekranında kaldı, istemci tarafı hiç çalışmadı. Ölçümden çıkarım: SRI yalnız dosya olarak yüklenen betiklere `integrity` ekliyor, satır içi betikleri kapsamıyor. |
| Nonce (proxy'de her istekte üretilen tek kullanımlık değer) | Hayır | Next.js rehberine göre her sayfa dinamik üretilmeli; statik optimizasyon kapanır ve kısmi ön-render ile uyumsuz. Projede `cacheComponents` / `partialPrefetching` açık ve katalog ekranı (#171) buna göre yazıldı. Freeze gününde (17.11) bu değişiklik kapsam dışı bırakıldı. |
| `script-src 'self' 'unsafe-inline'` (seçilen) | Evet | **Uygulama çalışıyor**: katalog ve backend durumu yüklendi, istemci tarafı çalıştı. Enjekte edilmiş gibi yapılan üç betik denendi: başka bir siteye `fetch` **engellendi** (`connect-src`), dış kaynaktan `<script src>` **engellendi** (`script-src-elem`), sayfaya eklenen satır içi `<script>` **çalıştı**. |

## Sonuçlar

**Olumlu:**

- Uygulama statik kabukla çalışmaya devam ediyor; `cacheComponents` ve katalogdaki yapı değişmedi.
- Enjekte edilen bir betik token'ı okusa bile `fetch`/XHR ile başka bir siteye gönderemiyor; dış kaynaktan betik yükleyemiyor; site başka bir sayfaya gömülemiyor.

**Olumsuz / ödünleşimler (kalan risk):**

- **Satır içi betik çalışabiliyor.** Bir XSS açığı olursa enjekte edilen satır içi betik `localStorage`'daki token'ı okuyabilir. `connect-src` dışarıya `fetch`'i engelliyor, ama sayfayı başka bir adrese yönlendirmek (`location = …`) gibi yollar CSP ile kapanmıyor. ADR-002'deki "enjekte edilen satır içi betik çalışmaz" beklentisi bu kurulumda sağlanamadı.
- Bu yüzden XSS'e karşı asıl korumanın yeri değişti: **CSP değil, XSS'in hiç oluşmaması.** React değerleri kaçışlayarak basıyor; `dangerouslySetInnerHTML` ESLint'te yasak. Kullanıcı içeriği (kurs açıklaması, yorum, ad soyad) her zaman metin olarak gösterilmeli.
- Token'ın süresi 1 saat (ADR-002): çalınan bir token'ın kullanılabileceği süre bununla sınırlı.

**Kararın kodda nasıl korunduğu:**

- `frontend/next.config.ts` › `headers()`: tüm sayfalarda `Content-Security-Policy` başlığı.
- `frontend/eslint.config.mjs` › `"react/no-danger": "error"`: `dangerouslySetInnerHTML` lint'te hata, CI'da build'i kırar (`npm run lint`).
- Nonce'a geçmek gerekirse (ör. kullanıcı içeriğinin HTML olarak gösterilmesi istenirse) yeni bir ADR yazılır. O ADR, `cacheComponents` / kısmi ön-render'dan vazgeçmenin bedelini de değerlendirir.
