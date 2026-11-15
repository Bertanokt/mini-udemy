# ADR-003: Flyway ile şema yönetimi

- **Durum:** Kabul edildi
- **Karar tarihi:** 15.10.2026 (repo iskeleti, `479fc03`: Flyway ve `ddl-auto=validate`). Zaman damgalı dosya adlandırması 22.10.2026'da, backlog'un yeniden planlanmasında getirildi (`f483d28`).
- **Kayıt tarihi:** 15.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #81, #7, #20, #29, #40, #47, #53 (ilk migration'lar), #172 (`ddl-auto=validate` ile uyumsuz kolonun yakalanması), [ADR-001](0001-moduler-monolit.md)

## Bağlam

Uygulamanın verisi PostgreSQL'de tutuluyor; şema 10 modülün entity'leriyle birlikte hafta hafta büyüyor (kullanıcılar, kurslar, kayıtlar, ilerleme, sınavlar …). Kısıtlar:

- **Paralel çalışma:** dört kişi aynı anda farklı modüllerin tablolarını ekliyor. Şema değişiklikleri çakışmadan birleşmeli ve her ortamda aynı sırayla uygulanmalı.
- **Tekrarlanabilir kurulum:** demo temiz bir makinede `docker compose up --build` ile boş veritabanından ayağa kalkmalı (NFR-16); şema elle kurulmamalı.
- **Dokümanla tutarlılık:** şema ER diyagramıyla aynı olmalı; raporda şemanın nasıl evrildiği gösterilebilmeli.
- **Ekip deneyimi:** ekipte veritabanı uzmanı yok; şemanın entity'lerden sessizce türetilip değişmesi hataları gizler.

## Karar

Şema **yalnız Flyway'in sürümlü SQL migration'larıyla** değişir; Hibernate şemayı değiştirmez, yalnız doğrular (`ddl-auto=validate`).

- **Ayar:** `backend/src/main/resources/application.yml` › `spring.jpa.hibernate.ddl-auto: validate`, `spring.flyway.enabled: true`, `spring.flyway.locations: classpath:db/migration`. Uygulama açılırken Flyway bekleyen migration'ları uygular, ardından Hibernate entity'leri tablolara karşı doğrular. Uyuşmazlık varsa uygulama açılmaz.
- **Dosya adı zaman damgalı:** `V<yyyyMMddHHmm>__<aciklama>.sql` (ör. `V202611131449__create_course_structure.sql`). Zaman damgası `date +%Y%m%d%H%M` ile üretilir; sıra numarası (V1, V2…) kullanılmaz (`backend/src/main/resources/db/migration/README.md`, `CLAUDE.md` › Veritabanı ve Flyway).
- **Merge edilmiş migration değişmez:** `main`'e girmiş bir migration dosyası bir daha düzenlenmez; düzeltme her zaman yeni bir migration ile yapılır.
- **Bir migration, bir mantıksal değişiklik:** bir tablo grubu ya da bir alan ekleme. Şema ER diyagramıyla aynı PR'da güncellenir.
- **dev profilinde sırasız uygulama:** `application-dev.yml` › `spring.flyway.out-of-order: true`. Geliştiricinin yerel veritabanına daha yeni tarihli bir migration uygulanmışken başkasının daha eski tarihli migration'ı merge edilirse o da uygulanır. CI ve demo boş veritabanından başladığı için orada sıra her zaman zaman damgasına göredir.

## Değerlendirilen alternatifler

### Hibernate'in şemayı kendisinin güncellemesi (`ddl-auto=update`)

Uygulama açılırken Hibernate entity'lere bakıp eksik tablo ve kolonları ekler.

- **Artıları:** migration dosyası yazmak gerekmez; başlangıçta çok hızlı.
- **Neden seçilmedi:** `update` kolon silmez, ad değiştirmeyi bilmez (eskisini bırakıp yenisini ekler), kısıtları (CHECK, benzersizlik, `ON DELETE CASCADE`) ve indeksleri eksik ya da farklı üretir. Şemanın hangi değişiklikle ne zaman geldiği hiçbir yerde kayıtlı olmaz; iki geliştiricinin yerel şeması sessizce ayrışır. Entity'deki bir yazım hatası hata vermek yerine yeni bir kolon açar. #172'de olduğu gibi entity ile şema arasındaki uyumsuzluğun açılışta yakalanması, ancak şemayı entity'den bağımsız yazıp `validate` ile karşılaştırınca mümkün.

### Liquibase

Sürümlü şema değişikliklerini XML/YAML/SQL "changeset"leriyle yöneten, Flyway'e benzer bir araç.

- **Artıları:** veritabanından bağımsız changeset biçimi; otomatik geri alma (rollback) desteği; `preConditions` ile koşullu değişiklik.
- **Neden seçilmedi:** tek veritabanımız PostgreSQL olduğu için veritabanından bağımsızlığın faydası yok; changeset biçimi ekibin zaten bildiği SQL'in üstüne öğrenilecek bir katman ekliyor. Geri alma, düzeltmeyi yeni migration ile yapma kuralımızla gereksiz. Flyway düz SQL dosyalarıyla çalışıyor, Spring Boot'ta yalnız bağımlılık ve iki satır ayarla açılıyor. SQL dosyaları ER diyagramıyla doğrudan karşılaştırılabiliyor.

### Flyway, ama sıra numaralı sürümler (V1, V2, V3…)

Flyway'in varsayılan ve en yaygın adlandırması.

- **Artıları:** okunması kolay, sıra açıkça görünür.
- **Neden seçilmedi:** dört kişi paralel çalışırken iki PR aynı sıradaki numarayı alır; sonra merge edilen PR'ın migration'ı çakışır ya da uygulanmaz ve numara elle kaydırılmak zorunda kalır. Zaman damgası bu çakışmayı pratikte ortadan kaldırıyor. Bedeli, dev'de `out-of-order` ayarı.

## Sonuçlar

**Olumlu:**

- Şemanın tüm geçmişi repoda, sırayla okunabilir SQL dosyaları olarak duruyor (`backend/src/main/resources/db/migration/`; bu ADR yazıldığında 6 migration). Boş bir veritabanı tek açılışta güncel şemaya geliyor.
- Entity ile şema arasındaki uyumsuzluk uygulama açılırken yakalanıyor, çalışma anında değil (#172'deki uyumsuz kolon bu şekilde görüldü).
- CHECK, benzersizlik ve yabancı anahtar kısıtları SQL'de açıkça yazılı; veri bütünlüğü yalnız uygulama koduna bırakılmıyor.
- Paralel PR'lar migration numarası yüzünden çakışmıyor.

**Olumsuz / ödünleşimler:**

- Her şema değişikliği hem migration hem entity yazmayı gerektiriyor; ikisi elle senkron tutuluyor (`validate` farkı yakalıyor).
- Merge edilmiş bir migration'daki hata yerinde düzeltilemiyor; düzeltme migration'ı geçmişte kalıcı olarak görünür.
- Yerel veritabanı bozulursa (ör. henüz merge edilmemiş bir migration denendiyse) `docker compose down -v` ile sıfırlamak gerekiyor; yerel veri silinir.
- dev'deki `out-of-order` nedeniyle yerel veritabanında migration'lar CI'dakinden farklı sırada uygulanmış olabilir. Birbirine bağımlı migration'lar zaman damgası sırasına güvenmeli.

**Kararın kodda nasıl korunduğu:**

- `application.yml` › `ddl-auto: validate`: entity ile şema uyuşmazsa uygulama açılmaz.
- Flyway her açılışta veritabanında kayıtlı checksum ile dosyanın checksum'ını karşılaştırır. Merge edilmiş bir migration değiştirilirse daha önce uygulandığı her veritabanında (dev, demo) uygulama açılmaz (`db/migration/README.md` › Kurallar).
- `CLAUDE.md` › Veritabanı ve Flyway: zaman damgalı ad ve "merge edilmiş migration'ı asla değiştirme" kuralı Claude Code oturumlarına her seferinde yükleniyor. PR şablonunun DoD'sinde "Yeni migration zaman damgalı; merge edilmiş migration'lar değiştirilmedi" maddesi var.
- **Bilinen boşluk:** CI şu an gerçek bir PostgreSQL'e karşı Flyway çalıştırmıyor; testler veritabanı olmadan çalışıyor (`.github/workflows/ci.yml`'da veritabanı servisi yok). Migration ve `validate` hatası CI'da değil, ilk yerel ya da Docker açılışında görülüyor. Entegrasyon testleri (#65, #138) gerçek veritabanıyla çalışınca bu boşluk kapanır.
