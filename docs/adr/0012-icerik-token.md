# ADR-012: Ders içeriği için kısa ömürlü, derse özel token

- **Durum:** Kabul edildi
- **Karar tarihi:** 19.11.2026 (#45; Bertan'ın kararı)
- **Kayıt tarihi:** 19.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #45 (ders oynatıcı), #113 (yetki kontrollü içerik servisi), [ADR-002](0002-jwt-stateless-kimlik-dogrulama.md), [ADR-004](0004-yerel-dosya-depolama.md), [ADR-007](0007-csp-nonce-suz.md)

## Bağlam

Ders içeriği `GET /api/lessons/{id}/content` ile, her istekte yetki kontrolüyle sunuluyor (ADR-004, #113). Kimlik `Authorization: Bearer` başlığındaki JWT'den geliyor (ADR-002); token tarayıcıda localStorage'da.

Ders oynatıcı (#45) videoyu `<video>` etiketiyle oynatmalı, PDF'i ve dokümanı bir bağlantıyla açtırmalı ya da indirtmeli. Tarayıcı `<video>` ve `<a href>` isteklerine **`Authorization` başlığı ekleyemiyor**; adresi doğrudan verirsek istek 401 alır. Video ileri sarılabilmeli (HTTP `Range`, 206) ve 200 MB'a kadar dosyalar belleğe alınmadan akmalı (NFR-02).

## Karar

İçerik adresi, **5 dakika geçerli, kullanıcıya ve derse özel bir token**'ı `?token=` parametresiyle taşır.

- **Token alma:** `POST /api/lessons/{id}/content-token` (Bearer ile) `{token, expiresAt}` döner.
  - Kural içeriği açmakla aynı: kursa kayıtlı kursiyer, kursun eğitmeni ya da yönetici; diğerleri 403, içerik yoksa 404.
  - Token almak "ders açıldı" sayılmaz (#119'un `LessonOpened` olayı yalnız içerik isteğinde yayınlanır).
- **Token kullanma:** `GET /api/lessons/{id}/content?token=…`.
  - Güvenlik filtresi yalnız bu isteği kimliksiz geçirir: `GET`, yol `/api/lessons/{sayı}/content`, `token` parametresi var (`SecurityConfig.isLessonContentWithToken`). Başka hiçbir uç ya da yöntem token parametresiyle açılmaz.
  - Token'ı ve yetkiyi içerik servisi doğrular: imza, süre ve ders tutmalı (yoksa 401); hesap veritabanında olmalı (yoksa 401) ve aktif olmalı (değilse 403); rol veritabanından okunur; kayıt, sahiplik ve rol kontrolü Bearer'lı istekteki gibi **her istekte** yeniden yapılır.
- **Token'ın yapısı:** HS256 imzalı bir JWT; içinde `sub` (kullanıcı id), `lesson` (ders id), `typ=content`, `iat` ve `exp` var (`common/security/ContentTokens`).
- **Ayrı anahtar:** imza anahtarı ana JWT anahtarından HMAC-SHA256 ile türetilmiş ayrı bir anahtar (`mini-udemy/content-token/v1` etiketiyle). Bu yüzden:
  - içerik token'ı API'nin Bearer doğrulayıcısında imzadan geçmez; 5 dakikalık bir video token'ıyla API kullanılamaz;
  - API token'ı da içerik token'ı yerine geçmez.
- **Süre dolunca:** video oynatma 5 dakikadan uzun sürerse, tarayıcının sonraki Range isteği 401 alır. Oynatıcı yeni token alıp adresi yeniler ve kaldığı saniyeden devam eder (#45).

## Değerlendirilen alternatifler

### Dosyayı `fetch` ile alıp Blob adresi (`URL.createObjectURL`) vermek

İstek `Authorization` başlığıyla `fetch` ile atılır, gelen dosya tarayıcıda bir Blob adresine dönüştürülür.

- **Artıları:** sözleşme ve güvenlik ayarı değişmez.
- **Neden seçilmedi:** dosyanın tamamı belleğe iner. 200 MB'lık bir video bitmeden oynamaz, `Range` ve akış kullanılmaz (NFR-02). PDF ve doküman (≤ 20 MB) için kabul edilebilirdi; ama video için olmaz, iki ayrı yol da gereksiz karmaşıklık getirirdi.

### Ana JWT'yi adreste taşımak (`?access_token=`)

Spring Security'nin Bearer çözücüsü adresteki `access_token` parametresini de kabul edecek şekilde açılır.

- **Neden seçilmedi:** bütün API'ye 1 saat yetki veren token adreste, tarayıcı geçmişinde ve sunucu loglarında görünür. Bizim token'ımız yalnız bir dersin içeriği için ve 5 dakika geçerli; sızarsa etkisi o ders ve o süreyle sınırlı.

### Çerezle (HttpOnly cookie) kimlik doğrulama

Girişte bir oturum çerezi de verilir; tarayıcı medya isteklerinde çerezi kendiliğinden gönderir.

- **Neden seçilmedi:**
  - ADR-002'deki stateless Bearer modelini değiştirir; iki kimlik yolu bütün API'de birlikte yaşar.
  - Çerezle gelen isteklerde CSRF koruması gerekir.
  - Frontend ve API ayrı origin'lerde (ADR-005); çerez `SameSite` ve CORS ayarları da gerekir.
  - Bu tek ihtiyaç için ağır.

### Süreli imzalı adres (presigned URL)

ADR-004'te nesne depolama için reddedilen yol.

- **Neden seçilmedi:** imzalı adres süresi dolana kadar her isteği kabul eder, yetki yeniden sorulmaz. ADR-004 bu yüzden reddetmişti. Bu karardaki token yalnız kimliği taşır; kayıt, rol ve hesap aktifliği her istekte veritabanından yeniden denetlenir. Yani ADR-004'ün "her istekte yetki kontrolü" kuralı korunuyor.

## Sonuçlar

**Olumlu:**

- Video tarayıcının kendi oynatıcısıyla, `Range` ve ileri sarmayla akıyor; PDF tarayıcının PDF görüntüleyicisinde yeni sekmede açılıyor, doküman iniyor; ek bellek yok.
- PDF'i sayfaya `<iframe>` ile gömmek bu kararın dışında: frontend CSP'sinde `frame-src` yok (`default-src 'self'`, ADR-007) ve backend yanıtları Spring Security'nin varsayılan `X-Frame-Options: DENY` başlığını taşıyor. Gömmek istenirse ikisi birlikte, ayrı bir kararla değişmeli.
- Yetki her istekte yeniden sorulduğu için kaydı silinen ya da pasifleştirilen kullanıcının elindeki token en geç bir sonraki istekte işe yaramaz.
- Token sızarsa etkisi bir ders ve en fazla 5 dakikayla sınırlı; API'ye erişim vermez.

**Olumsuz / ödünleşimler:**

- **Adreste token:** token tarayıcı geçmişinde ve sunucu erişim loglarında görünebilir. Süresinin kısa olması ve ders ile kullanıcıyla sınırlı olması bu riski azaltıyor, ortadan kaldırmıyor.
- **Uzun videolar:** 5 dakikadan uzun oynatmada oynatıcı token'ı yenilemek zorunda (#45). Yenileme sırasında bir an bekleme olabilir.
- **İkinci bir kimlik yolu:** `SecurityConfig`'te bir istisna kuralı var. Kural tek bir yol ve yöntemle sınırlı, ayrıca testle korunuyor.

**Kararın kodda nasıl korunduğu:**

- `ContentTokensTest`: token dersine ve süresine bağlı; API token'ı içerik token'ı yerine, içerik token'ı API token'ı yerine geçmiyor; bozuk token reddediliyor.
- `ContentDeliveryServiceTest`: geçersiz ya da süresi dolmuş token 401, silinmiş hesap 401, pasif hesap 403, kaydı olmayan kullanıcı 403; rol veritabanından; token verilmesi içeriği açmakla aynı kurala bağlı.
- `ContentControllerTest`: token parametresi yalnız `GET /api/lessons/{id}/content`'i açıyor (başka yol ve `POST` 401); token'lı istekte Range 206.
- #45 PR'ında her kural bilerek bozulduğunda ilgili testin kırıldığı listeleniyor.
