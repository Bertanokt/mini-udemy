# ADR-002: JWT ile stateless kimlik doğrulama

- **Durum:** Kabul edildi
- **Karar tarihi:** 15.10.2026 (repo iskeleti ve backlog, `479fc03`: "Stateless security config, JWT üretme/doğrulama servisi"). Uygulama ayrıntıları 14.11.2026'da #10'da (PR #179), herkese açık uçlardaki 401 davranışı ve token'ın saklanacağı yer 15.11.2026'da kararlaştırıldı.
- **Kayıt tarihi:** 15.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #81, #10 (Spring Security + JWT, PR #179), #13 (login), #16 (frontend auth ve token saklama), #115 (parola yenileme), [ADR-001](0001-moduler-monolit.md), [ADR-005](0005-ayri-frontend-rest-openapi.md)

## Bağlam

Kullanıcı kayıt olur, giriş yapar ve rolüne göre (öğrenci, eğitmen, yönetici) farklı uçlara erişir (proje dokümanı modül A; NFR-04, NFR-05). Kısıtlar:

- **Ayrı frontend:** Next.js uygulaması (`localhost:3000`) backend'in REST API'sini (`localhost:8080`) çağırır ([ADR-005](0005-ayri-frontend-rest-openapi.md)). Kimlik bilgisi her istekte API'ye taşınmalı.
- **Kurulum:** sistem temiz makinede `docker compose up --build` ile ayağa kalkmalı (NFR-16); ücretli ya da hesap gerektiren dış servis yok.
- **Ekip ve süre:** 4 kişi, yaklaşık 5 hafta; güvenlik altyapısı diğer modüllerin önünde (kritik yol #10 → #13 → #14 → …).
- **Yetki her istekte doğru olmalı:** yönetici bir kullanıcıyı pasifleştirdiğinde ya da rolünü değiştirdiğinde bu hemen geçerli olmalı (güvenlik denetimi O3, 13.11.2026).

## Karar

Kimlik doğrulama **sunucuda oturum tutmadan, HS256 ile imzalanmış ve süresi 1 saat olan bir JWT ile** yapılır. Token `Authorization: Bearer` başlığında taşınır, frontend'de `localStorage`'da saklanır.

- **Üretim (#10, #13):** `POST /api/auth/login` yanıtında `accessToken`, `tokenType: Bearer`, `expiresIn: 3600` döner (`docs/api/openapi.yaml`). Token'da `sub` kullanıcı id'si, `role` rol claim'idir. Üretimi `JwtService.generateToken` yapar. Yenileme (refresh) token'ı yok; süre dolunca kullanıcı yeniden giriş yapar.
- **Doğrulama:** Spring Security'nin oauth2-resource-server modülü (Nimbus). `BearerTokenAuthenticationFilter` token'ı başlıktan okur; `JwtDecoder` imzayı (HS256) ve süreyi (`exp`, 30 sn saat farkı toleransıyla) denetler (`JwtConfig`).
- **Kimlik ve rol veritabanından:** imza ve süre geçerliyse `JwtAccountAuthenticationConverter` kullanıcının güncel durumunu `AccountLookup` üzerinden sorar. Kullanıcı silinmişse 401, pasifse 403 döner. Yetki token'daki `role` claim'inden değil, veritabanındaki rolden kurulur. `AccountLookup` arayüzü `common`'dadır, uygulaması `auth` modülündedir; `common` hiçbir modüle bağımlı olmaz ([ADR-001](0001-moduler-monolit.md)).
- **Anahtar:** `application.yml`'da `app.jwt.secret: ${JWT_SECRET}`, yedek değeri yok. Anahtar tanımlı değilse ya da 32 bayttan kısaysa uygulama açılmaz (`JwtProperties`, güvenlik denetimi O2). Yalnız dev ve test profillerinde "dev-only" / "test-only" etiketli sabit bir anahtar var.
- **Oturum ve CSRF yok:** `SessionCreationPolicy.STATELESS`; CSRF koruması kapalı, çünkü token çerezde değil başlıkta taşınıyor ve tarayıcı onu başka bir siteden gelen isteğe kendiliğinden eklemiyor (`SecurityConfig`).
- **Herkese açık uçlar:** sözleşmede güvenliği boş ya da isteğe bağlı olan 12 uç (`SecurityConfig.PUBLIC_GET`, `PUBLIC_POST`). İstekte geçersiz ya da süresi dolmuş bir token varsa bu uçlar da 401 döner. İstemci 401 alınca token'ı siler ve isteği token'sız tekrarlar (sözleşme PR #182; istemci tarafı #16).
- **Token'ın frontend'de saklanması (#16):** `localStorage`. XSS riski ve buna karşı alınan önlemler aşağıda ("Token'ın saklanacağı yer").

### Token'ın saklanacağı yer

`localStorage`'daki değeri sayfada çalışan her JavaScript okuyabilir. Sitede bir XSS açığı olursa (örneğin bir kurs açıklamasına ya da yoruma gömülmüş betik), saldırgan token'ı çalıp süresi dolana kadar kullanıcının yerine API'yi çağırabilir. Bu risk bilerek kabul edildi ve şu önlemlerle küçültülüyor:

- **İçerik metin olarak gösterilir:** React değerleri varsayılan olarak kaçışlayarak (escape) basar. `dangerouslySetInnerHTML` kullanılmaz; yasak #16'da ESLint kuralıyla (`react/no-danger`) zorunlu hale gelir.
- **Temel CSP:** #16'da `next.config.ts`'e, betiklerin yalnız sitenin kendisinden yüklenmesine izin veren bir `Content-Security-Policy` başlığı eklenir. Böylece enjekte edilen satır içi ya da dış betik çalışmaz.
- **Kısa süre:** token en fazla 1 saat geçerli; çalınan bir token'ın kullanılabileceği süre bununla sınırlı.
- **Çıkışta ve 401'de silinir:** istemci çıkışta ve 401 aldığında token'ı `localStorage`'dan siler.

Değerlendirilen diğer iki seçenek ve neden seçilmedikleri:

- **Yalnız bellekte (JavaScript değişkeni ya da React state):** XSS'e karşı `localStorage`'dan biraz daha dayanıklı; betik token'ı kalıcı bir yerden okuyamaz, ama sayfa açıkken yine de istek atabilir. **Seçilmedi**, çünkü sayfa yenilendiğinde ya da yeni sekme açıldığında token kaybolur ve kullanıcı yeniden giriş yapmak zorunda kalır. Bunu sessizce telafi etmenin yolu bir yenileme token'ı olur; o token'ın da güvenli saklanması için çerez gerekir. Yani bu seçenek tek başına yetmez, aşağıdaki çerez seçeneğinin işini de gerektirir. Demo akışlarında (kurs oynatıcı, sınav) her yenilemede girişe dönmek kabul edilemez.
- **httpOnly çerez + CSRF koruması:** token'ı JavaScript hiç okuyamaz, XSS ile çalınamaz (betik sayfa açıkken kullanıcı adına istek atabilir ama token'ı dışarı taşıyamaz). **Seçilmedi**, çünkü kararı ve hazır kodu birkaç yerden değiştirir:
  - Tarayıcı çerezi her isteğe kendiliğinden eklediği için CSRF koruması geri açılmalı ve frontend her değiştiren istekte bir CSRF token'ı göndermeli (`SecurityConfig`'te şu an kapalı).
  - Frontend ve backend farklı origin'de olduğu için CORS'ta `allowCredentials(true)` gerekir (`WebConfig`), frontend'deki her istek `credentials: "include"` ile atılır.
  - Spring'in token'ı başlık yerine çerezden okuması için özel bir `BearerTokenResolver` gerekir.
  - Sözleşme değişir: login yanıtı token'ı gövdede değil `Set-Cookie` ile verir; çerezi silecek bir çıkış ucu eklenir.

  Bu iş feature freeze'den (17.11.2026) önce #10, #13 ve #16'yı yeniden açmak demek. Yerel bir demo için ek güvenlik kazancı bu maliyeti karşılamıyor. Gerekirse yeni bir ADR ile bu karar değiştirilebilir.

## Değerlendirilen alternatifler

### Sunucu oturumu + oturum çerezi (Spring Session / `JSESSIONID`)

Giriş yapınca sunucu bir oturum açar, tarayıcıya oturum çerezi verir; yetki her istekte sunucudaki oturumdan okunur.

- **Artıları:** oturum sunucuda tek hamlede sonlandırılabilir (çıkış, pasifleştirme anında geçerli); çerez httpOnly olduğu için XSS ile çalınamaz.
- **Neden seçilmedi:** ayrı origin'deki frontend için CORS'ta kimlik bilgisiyle istek (`allowCredentials`) ve CSRF koruması gerekir; yukarıdaki httpOnly çerez seçeneğinin bütün işi burada da var. Sunucu oturum durumu tutar; birden fazla backend örneğinde ortak oturum deposu (ör. Redis) gerekir. Pasifleştirmenin anında geçerli olması ihtiyacı, JWT ile her istekte veritabanına bakılarak zaten karşılandı (O3).

### Harici kimlik sağlayıcısı (Keycloak, Google / GitHub ile OAuth2 / OIDC)

Kimlik doğrulamayı ayrı bir sağlayıcı yapar; uygulama yalnız onun verdiği token'ı doğrular.

- **Artıları:** parola saklama, parola sıfırlama, çok faktörlü doğrulama ve token iptali sağlayıcıda hazır.
- **Neden seçilmedi:** Google/GitHub dış hesap ve internet bağlantısı gerektirir, temiz makinede tek komutla demo kuralına uymaz (NFR-16). Keycloak yerel çalışır ama Compose'a ağır bir servis, realm ve istemci ayarı ekler. Proje dokümanı kayıt, giriş ve parola yenilemeyi uygulamanın kendi işlevi olarak istiyor (modül A; #12, #13, #115). Rol ve aktiflik bilgisi yine uygulamanın veritabanında kalacağı için iki kaynak senkron tutulmak zorunda kalırdı.

### JWT, ama kendi yazdığımız filtre ve kütüphane (ör. jjwt + `OncePerRequestFilter`)

Token'ı ayrı bir JWT kütüphanesiyle üretip, başlığı okuyan ve doğrulayan filtreyi kendimiz yazmak.

- **Artıları:** yaygın örneklerin çoğu bu şekilde; davranış tümüyle bizim kodumuzda görünür.
- **Neden seçilmedi:** imza ve süre denetimi, başlık ayrıştırma gibi güvenliğe kritik kodu Spring Security hazır ve test edilmiş olarak veriyor (oauth2-resource-server, Nimbus). Ek bir JWT kütüphanesine gerek kalmıyor. Kendi filtremizde bir hata (örneğin süre denetiminin atlanması) doğrudan bir güvenlik açığı olur.

## Sonuçlar

**Olumlu:**

- Sunucu oturum durumu tutmuyor; backend yeniden başlasa da kullanıcılar oturumda kalıyor. Yatay ölçeklemede ortak oturum deposu gerekmiyor.
- Pasifleştirme ve rol değişikliği bir sonraki istekte geçerli: rol ve aktiflik token'dan değil veritabanından okunuyor (O3).
- Anahtar unutulursa ya da zayıfsa uygulama açılmıyor; "herkesin token üretebildiği" bir sistem sessizce çalışmaz (O2).
- Filtrede oluşan 401/403 de diğer hatalarla aynı `ErrorResponse` gövdesinde dönüyor (`JsonSecurityErrorHandler`).

**Olumsuz / ödünleşimler:**

- **Token iptal edilemiyor:** sunucuda bir iptal listesi yok. Çıkış yapan kullanıcının token'ı süresi dolana kadar (en fazla 1 saat) geçerli kalır. Pasifleştirme ve rol değişikliği her istekte veritabanından kontrol edildiği için bu durumlar etkilenmiyor; etkilenen durum çalınmış bir token.
- **Parola yenilenince eski token'lar düşmüyor (#115):** parolası sıfırlanan kullanıcının daha önce verilmiş token'ları 1 saate kadar çalışır. Kapatmak için kullanıcıya "token sürümü" ya da "parola değişme zamanı" alanı eklenip converter'da `iat` ile karşılaştırılabilir; bu iş #115'in kapsamında değerlendirilecek.
- **Kısa süre, yenileme yok:** kullanıcı 1 saatte bir yeniden giriş yapar. Demo için kabul edildi; yenileme token'ı, saklanacağı yer (çerez) sorununu yeniden açar.
- **Her istekte bir veritabanı sorgusu:** `AccountLookup` her doğrulanmış istekte kullanıcıyı okur. Tek sunuculu, birkaç kullanıcılı demo için ihmal edilebilir.
- **XSS riski:** token `localStorage`'da; bir XSS açığı token'ın çalınmasına yol açar. Önlemler yukarıda; CSP ve ESLint yasağı #16 ile gelir.
- **Herkese açık uçta geçersiz token 401 döner:** istemcinin bu durumu ele alması gerekir (token'ı sil, token'sız tekrarla).

**Kararın kodda nasıl korunduğu:**

Aşağıdaki sınıf ve testler #10 ile gelir (PR #179; bu ADR yazıldığında henüz `main`'de değil).

- `backend/src/main/java/com/miniudemy/common/security/SecurityConfig.java`: stateless, CSRF kapalı, herkese açık uç listesi, rol kuralları.
- `backend/src/main/java/com/miniudemy/common/security/JwtConfig.java`, `JwtProperties.java`, `JwtService.java`, `JwtAccountAuthenticationConverter.java`.
- `backend/src/main/resources/application.yml` › `app.jwt.secret: ${JWT_SECRET}`, `app.jwt.expiration: 1h`.
- `SecurityConfigTest`: token'sız korumalı uç 401; bozuk, süresi dolmuş, başka anahtarla imzalanmış token 401; silinmiş kullanıcı 401; pasif kullanıcı 403; rol veritabanından (`roleComesFromDatabase_notFromToken`); herkese açık POST CSRF'e takılmıyor.
- `JwtPropertiesTest`: anahtar yoksa, boşsa, ortam değişkeni çözülmemişse ya da 32 bayttan kısaysa uygulama açılmıyor.
- `JwtServiceTest`: süresi dolmuş, değiştirilmiş ve başka anahtarla imzalanmış token reddediliyor.
- `docs/api/openapi.yaml`: login yanıtı ve herkese açık uçlardaki 401 (PR #182).
- Token saklama önlemleri #16'da gelir: ESLint `react/no-danger` kuralı ve `next.config.ts`'teki CSP başlığı.
