# ADR-006: DTO + Mapper deseni, MapStruct ile ve eşlenmemiş alan derleme hatası

- **Durum:** Kabul edildi
- **Karar tarihi:** 14.11.2026 (#9 üzerinde çalışmaya başlarken; yaklaşım seçimi #9'un kabul kriteriydi)
- **Kayıt tarihi:** 14.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #9, #8 (`User` entity), [ADR-001](0001-moduler-monolit.md), [ADR-005](0005-ayri-frontend-rest-openapi.md), [`docs/architecture.md`](../architecture.md) › Tasarım desenleri

## Bağlam

Her modül JPA entity'leriyle çalışır, API ise `docs/api/openapi.yaml`'daki şemaları döner (ADR-005). İkisini aynı sınıf yapmak üç soruna yol açar:

- **Sızıntı:** entity'nin her alanı JSON'a çıkar. `users` tablosunda `password_hash` var; bir entity'yi doğrudan döndürmek parola özetini istemciye gönderir (NFR-04).
- **Kilitlenme:** veritabanı şemasındaki her değişiklik API sözleşmesini de değiştirir; sözleşme ile şema ayrı evrilemez.
- **Serileştirme hataları:** lazy JPA ilişkileri (ör. kurs → bölümler) transaction dışında JSON'a çevrilirken hata verir ya da gereksiz sorgu üretir.

Bu yüzden mimari kurallar baştan "entity API'ye çıkmaz; controller yalnız DTO alır ve döner, dönüşüm yalnız mapper'da yapılır" diyor (`docs/architecture.md`, `CLAUDE.md`). #9'da cevaplanması gereken soru, dönüşümün **nasıl** yazılacağı: 10 modülde onlarca DTO olacak ve sözleşme (#165 ile 57 adres, 73 işlem) büyümeye devam ediyor.

## Karar

DTO'lar Java `record` olarak `<modül>/dto` paketinde, mapper'lar `<modül>/mapper` paketinde **MapStruct** arayüzleri olarak yazılır; dönüşüm kodunu MapStruct derleme sırasında üretir.

- **Ortak ayar:** tüm mapper'lar `@Mapper(config = MapStructConfig.class)` kullanır (`common/mapper/MapStructConfig`):
  - `componentModel = "spring"`: mapper bir Spring bean'idir, service'e constructor ile verilir.
  - **`unmappedTargetPolicy = ReportingPolicy.ERROR`**: DTO'daki bir alanın kaynağı bulunamazsa **derleme durur**. Bir alan bilerek boş bırakılacaksa `@Mapping(target = "...", ignore = true)` ile açıkça yazılır.
- **İlk örnek:** `auth/dto/UserResponse` (`id`, `email`, `fullName`, `role`, `bio`, `createdAt`; OpenAPI › `UserResponse` ile aynı) ve `auth/mapper/UserMapper.toResponse(User)`. `passwordHash`, `active` ve `updatedAt` DTO'da yok.
- **Sürüm:** MapStruct 1.6.3 (`mapstruct` bağımlılığı ve `maven-compiler-plugin`'de `mapstruct-processor`), `backend/pom.xml` › `mapstruct.version`.

## Değerlendirilen alternatifler

### Elle yazılmış mapper

Her modülde dönüşümü yapan sıradan bir sınıf (`new UserResponse(user.getId(), user.getEmail(), ...)`).

- **Artıları:** ek bağımlılık ve derleme adımı yok; kod açık, okuması ve hata ayıklaması kolay; karmaşık dönüşümlerde tam kontrol.
- **Neden seçilmedi:** onlarca DTO için tekrar eden kod yazılır ve testi gerekir. En önemlisi, DTO'ya yeni bir alan eklendiğinde mapper'ı güncellemeyi unutmak **sessiz bir hata** üretir: record'larda derleyici yakalar, ama sınıf tabanlı ya da `null` geçilen alanlarda istemciye boş değer gider. MapStruct'ın ERROR politikası bu hatayı derlemede yakalıyor. Elle yazmanın kontrol avantajı MapStruct'ta da var: karmaşık bir alan için mapper arayüzüne `default` metot ya da `@Mapping(expression = ...)` yazılabilir.

### ModelMapper (çalışma anında yansıma ile eşleme)

- **Artıları:** yapılandırma az; alan adlarından kendiliğinden eşler.
- **Neden seçilmedi:** eşleme çalışma anında yapılır; yanlış ya da eksik eşleme ancak o kod çalıştığında (çoğu zaman bir testte ya da kullanıcıda) görülür. Yansıma kullandığı için daha yavaştır ve record'larla (constructor ile kurulan değişmez tipler) çalışması ek ayar ister.

### Entity'yi doğrudan döndürmek (`@JsonIgnore` ile alan gizleyerek)

- **Artıları:** en az kod.
- **Neden seçilmedi:** bağlamdaki üç sorunun hepsi kalır. Gizlenecek alan her entity'de ayrı ayrı hatırlanmalı; unutulan bir `@JsonIgnore` parola özetini sızdırır. Mimari kuralı ("entity API'ye çıkmaz") doğrudan ihlal eder.

## Sonuçlar

**Olumlu:**

- Sözleşmeye eklenen ama mapper'da karşılığı olmayan alan **derlenmez**; hata CI'da, kullanıcıya ulaşmadan yakalanır.
- Dönüşüm kodu üretildiği için tekrar eden ve test edilmesi gereken elle kod azalır; mapper testleri sözleşmedeki alanların doğru aktarıldığına odaklanır.
- Üretilen kod düz Java'dır (`target/generated-sources/annotations`); yansıma yok, hata ayıklamada okunabilir.
- Kural tek yerde (`MapStructConfig`); bir modül farklı davranamaz.

**Olumsuz / ödünleşimler:**

- Yeni bir bağımlılık ve derleme adımı (annotation processor). IDE'de üretilen sınıfların görünmesi için projenin bir kez derlenmesi gerekir.
- Ekip MapStruct'ın anotasyonlarını (`@Mapping`, `ignore`, `expression`) öğrenmek zorunda; karmaşık dönüşümler elle yazılmış koda göre daha az doğrudan okunur.
- ERROR politikası, kaynakta olmayan bir alan için her seferinde açık bir karar (eşle ya da `ignore`) ister; bu bilinçli bir sürtünme.

**Kararın kodda nasıl korunduğu:**

- `MapStructConfig`'deki `unmappedTargetPolicy = ERROR`: eşlenmemiş hedef alan derlemeyi, dolayısıyla CI'ı kırar. (#9'da denendi: `UserResponse`'a kaynağı olmayan bir alan eklenince derleme `Unmapped target property: "phone"` hatasıyla durdu.)
- `UserMapperTest`: alanların doğru aktarıldığını ve `UserResponse`'ta yalnız sözleşmedeki alanların, parola alanı olmadan, bulunduğunu doğrular.
- `CLAUDE.md` › Tasarım desenleri: yeni mapper'lar `@Mapper(config = MapStructConfig.class)` ile yazılır; dönüşüm controller ya da service'te elle yazılmaz.
- Katman kuralları (mapper yalnız `domain` ve `dto`'yu kullanır) ArchUnit ile korunacak (#86).
