# Katkı — Eray (Veri ve Entegrasyon Sorumlusu)

GitHub: [@eraygame113-beep](https://github.com/eraygame113-beep)

Son güncelleme: 23.11.2026

## Yaptığım işler

| Issue | PR | Çıktı |
|---|---|---|
| [#2 ER diyagramı (tüm modüller)](https://github.com/Bertanokt/mini-udemy/issues/2) | [#153](https://github.com/Bertanokt/mini-udemy/pull/153) | `docs/diagrams/er.puml` + `er.svg` / `er.png`: 10 modül, 20 tablo, PK/FK/unique/CHECK kısıtları |
| [#3 API sözleşmesi: auth, profil, parola yenileme](https://github.com/Bertanokt/mini-udemy/issues/3) | [#154](https://github.com/Bertanokt/mini-udemy/pull/154) | `docs/api/openapi.yaml`: register, login, parola yenileme (request/confirm), `GET/PUT /api/me` |
| [#7 Flyway migration: users](https://github.com/Bertanokt/mini-udemy/issues/7) | [#162](https://github.com/Bertanokt/mini-udemy/pull/162) | İlk migration: `users` tablosu; e-posta büyük/küçük harf duyarsız tekil (`lower(email)`), rol CHECK kısıtı |
| [#20 Flyway migration: kurs yapısı](https://github.com/Bertanokt/mini-udemy/issues/20) | [#163](https://github.com/Bertanokt/mini-udemy/pull/163) | `categories`, `courses`, `sections`, `lessons`; seviye/durum/içerik türü CHECK, bölüm ve derslerde CASCADE |
| [#4](https://github.com/Bertanokt/mini-udemy/issues/4), [#5](https://github.com/Bertanokt/mini-udemy/issues/5), [#108](https://github.com/Bertanokt/mini-udemy/issues/108) API sözleşmesinin kalanı | [#165](https://github.com/Bertanokt/mini-udemy/pull/165) | Tüm modüllerin uçları (57 adres, 73 işlem); her uçta rol (`x-roles`) |

**23.11 itibarıyla eklenenler**

| Alan | Issue | PR | Çıktı |
|---|---|---|---|
| Veritabanı (Flyway) | [#29](https://github.com/Bertanokt/mini-udemy/issues/29) | [#167](https://github.com/Bertanokt/mini-udemy/pull/167) | `enrollments`: kişi + kurs tekil |
| Veritabanı (Flyway) | [#40](https://github.com/Bertanokt/mini-udemy/issues/40) | [#168](https://github.com/Bertanokt/mini-udemy/pull/168) | `lesson_progress`, `course_progress` |
| Veritabanı (Flyway) | [#47](https://github.com/Bertanokt/mini-udemy/issues/47) | [#169](https://github.com/Bertanokt/mini-udemy/pull/169) | soru bankası ve sınav tabloları |
| Veritabanı (Flyway) | [#53](https://github.com/Bertanokt/mini-udemy/issues/53) | [#170](https://github.com/Bertanokt/mini-udemy/pull/170) | `quiz_attempts`, `attempt_answers` |
| Kayıt, katalog, yorum | [#30](https://github.com/Bertanokt/mini-udemy/issues/30) | [#209](https://github.com/Bertanokt/mini-udemy/pull/209) | kursa kayıt ve kayıtlı kurslarım; mükerrer kayıt engeli |
| Kayıt, katalog, yorum | [#26](https://github.com/Bertanokt/mini-udemy/issues/26) | [#207](https://github.com/Bertanokt/mini-udemy/pull/207) | katalog: arama, filtre, sayfalama, sıralama; kurs detayı |
| Kayıt, katalog, yorum | [#143](https://github.com/Bertanokt/mini-udemy/issues/143) | [#216](https://github.com/Bertanokt/mini-udemy/pull/216) | katalogda ortalama puan, değerlendirme sayısı, `minRating` filtresi |
| Kayıt, katalog, yorum | [#126](https://github.com/Bertanokt/mini-udemy/issues/126) | [#215](https://github.com/Bertanokt/mini-udemy/pull/215) | puan ve yorum: kayıtlı kursiyer, tek değerlendirme |
| Soru bankası ve sınav | [#121](https://github.com/Bertanokt/mini-udemy/issues/121) | [#204](https://github.com/Bertanokt/mini-udemy/pull/204) | soru bankası CRUD (çoktan seçmeli) |
| Soru bankası ve sınav | [#48](https://github.com/Bertanokt/mini-udemy/issues/48) | [#210](https://github.com/Bertanokt/mini-udemy/pull/210) | bölüm sonu sınavı: bankadan soru seçimi, başarı eşiği |
| Soru bankası ve sınav | [#49](https://github.com/Bertanokt/mini-udemy/issues/49) | [#213](https://github.com/Bertanokt/mini-udemy/pull/213) | sınav görüntüleme: öğrenci görünümünde doğru cevap yok |
| Soru bankası ve sınav | [#52](https://github.com/Bertanokt/mini-udemy/issues/52) | [#223](https://github.com/Bertanokt/mini-udemy/pull/223) | Strategy deseni: `ScoringStrategy` ve uygulamaları |
| Soru bankası ve sınav | [#54](https://github.com/Bertanokt/mini-udemy/issues/54) | [#225](https://github.com/Bertanokt/mini-udemy/pull/225) | sınav çözme, puanlama, başarı eşiği, `QuizCompleted` olayı |
| Soru bankası ve sınav | [#55](https://github.com/Bertanokt/mini-udemy/issues/55) | [#221](https://github.com/Bertanokt/mini-udemy/pull/221) | deneme geçmişi `GET /api/me/quizzes/{id}/attempts` |
| Desenler ve ilerleme | [#42](https://github.com/Bertanokt/mini-udemy/issues/42) | [#224](https://github.com/Bertanokt/mini-udemy/pull/224) | Observer: domain olayları, AFTER_COMMIT teslimi, ArchUnit kuralı, ADR-009 |
| Desenler ve ilerleme | [#43](https://github.com/Bertanokt/mini-udemy/issues/43) | [#226](https://github.com/Bertanokt/mini-udemy/pull/226) | bölüm ve kurs ilerleme yüzdesi, `ProgressListener` |
| Kurs içeriği | [#25](https://github.com/Bertanokt/mini-udemy/issues/25) | [#233](https://github.com/Bertanokt/mini-udemy/pull/233) | ders CRUD, sıralama, zorunlu ders işareti; `LessonPublished` olayı |
| Kurs içeriği | [#112](https://github.com/Bertanokt/mini-udemy/issues/112) | [#252](https://github.com/Bertanokt/mini-udemy/pull/252) | ders içeriği ve kapak görseli yükleme; bağı kopan dosya diskten silinir |
| Kurs içeriği | [#28](https://github.com/Bertanokt/mini-udemy/issues/28) | [#220](https://github.com/Bertanokt/mini-udemy/pull/220) | `GET /api/instructor/courses`: eğitmenin kursları |
| Eğitmen analitiği | [#133](https://github.com/Bertanokt/mini-udemy/issues/133) | [#253](https://github.com/Bertanokt/mini-udemy/pull/253) | kurs istatistikleri (kayıt, ilerleme, tamamlama, sınav başarısı) ve kursiyer listesi (#134) |
| Eğitmen analitiği | [#257](https://github.com/Bertanokt/mini-udemy/issues/257) | [#343](https://github.com/Bertanokt/mini-udemy/pull/343) | ilerleme dağılımı bütün kurstan backend'de (inceleme bekliyor) |
| Yönetim | [#60](https://github.com/Bertanokt/mini-udemy/issues/60) | [#211](https://github.com/Bertanokt/mini-udemy/pull/211) | admin: kullanıcı listesi ve hesap durumu |
| Yönetim | [#118](https://github.com/Bertanokt/mini-udemy/issues/118) | [#214](https://github.com/Bertanokt/mini-udemy/pull/214) | admin: kategori yönetimi |
| Ön yüz | [#35](https://github.com/Bertanokt/mini-udemy/issues/35) | [#171](https://github.com/Bertanokt/mini-udemy/pull/171) | kurs kataloğu ve filtreler |
| Ön yüz | [#36](https://github.com/Bertanokt/mini-udemy/issues/36) | [#208](https://github.com/Bertanokt/mini-udemy/pull/208) | kurs detay ve kayıt ol |
| Ön yüz | [#37](https://github.com/Bertanokt/mini-udemy/issues/37) | [#212](https://github.com/Bertanokt/mini-udemy/pull/212) | kayıtlı kurslar paneli |
| Ön yüz | [#38](https://github.com/Bertanokt/mini-udemy/issues/38) | [#219](https://github.com/Bertanokt/mini-udemy/pull/219) | eğitmen paneli: kurslarım, durum, onaya gönder |
| Ön yüz | [#122](https://github.com/Bertanokt/mini-udemy/issues/122) | [#205](https://github.com/Bertanokt/mini-udemy/pull/205) | soru bankası |
| Ön yüz | [#127](https://github.com/Bertanokt/mini-udemy/issues/127) | [#217](https://github.com/Bertanokt/mini-udemy/pull/217) | yorum formu ve değerlendirmeler |
| Ön yüz | [#63](https://github.com/Bertanokt/mini-udemy/issues/63) | [#218](https://github.com/Bertanokt/mini-udemy/pull/218) | admin kullanıcı yönetimi |
| Ön yüz | [#141](https://github.com/Bertanokt/mini-udemy/issues/141) | [#222](https://github.com/Bertanokt/mini-udemy/pull/222) | sınav sonuç geçmişi |
| Ön yüz | [#57](https://github.com/Bertanokt/mini-udemy/issues/57) | [#244](https://github.com/Bertanokt/mini-udemy/pull/244) | sınav çözme ekranı |
| Ön yüz | [#135](https://github.com/Bertanokt/mini-udemy/issues/135) | [#254](https://github.com/Bertanokt/mini-udemy/pull/254) | eğitmen analitik paneli |
| Ön yüz | [#305](https://github.com/Bertanokt/mini-udemy/issues/305) | [#342](https://github.com/Bertanokt/mini-udemy/pull/342) | katalogda puana göre filtre (inceleme bekliyor) |

## Kararlar ve nedenleri

**ER diyagramı**
- **İlerleme ve sertifika kayda (enrollment) bağlı, kullanıcıya değil.** İlerleme bir kişinin bir kurstaki durumudur; kişi + kurs çifti zaten bir kayıttır. Böylece kayıtsız kursta ilerleme ya da sertifika olamaz.
- **Mükerrer kayıt veritabanında da engelli:** `enrollments` UK(student_id, course_id). Service kontrolünü kaçıran eşzamanlı iki istek bile ikinci kaydı oluşturamaz. Aynı mantıkla bir kayda tek sertifika (`certificates.enrollment_id` UK), bir kursa kişi başı tek yorum.
- **Sınav ↔ soru çoka-çok ilişkisi ara tabloyla:** `quiz_questions`, bileşik PK. Aynı soru birden fazla sınavda kullanılabiliyor (soru bankası).
- **Kurs durumu tek kolonda** (`status` + `rejection_reason`). Durum geçişlerini kodda State deseni yönetecek; veritabanı yalnız mevcut durumu saklıyor.
- **Modüller renkle ayrıldı.** Modüller arası FK veritabanında bütünlük için var, ama kodda modüller birbirine yalnız id ile bağlanıyor (modüler monolit kuralı).

**API sözleşmesi**
- **Önce sözleşme (contract-first):** Frontend ve backend aynı anda çalışabilsin, kimse kimseyi beklemesin.
- **İstek ve yanıt şemaları ayrı** (DTO): Entity ve parola hash'i hiçbir yanıtta yok.
- **Tek hata formatı** (`ErrorResponse`): Bütün 400/401/403/404/409 yanıtları aynı gövdeyi döner, frontend hatayı tek yerde işler.
- **Kayıtlı e-postalar belli olmuyor:** Parola yenileme isteği e-posta kayıtlı olsun olmasın aynı 202 yanıtını döner; girişte yanlış e-posta ile yanlış parola aynı 401'i verir.
- **Kayıtta ADMIN rolü seçilemiyor:** Herkes kendini yönetici yapamasın diye.
- **Adres kuralı:** liste üst kaynağın altında, tek kayıt kendi id'siyle (`/api/courses/{id}/sections` ama `/api/sections/{id}`). Issue'lardaki `/api/lessons/{id}/complete` gibi adreslerle tutarlı.
- **Öğrenci ve eğitmen sınav görünümü ayrı adreslerde ve ayrı şemalarda:** öğrenci şemasında doğru cevap alanı hiç yok; tek adreste role göre farklı yanıt vermek test etmeyi zorlaştırırdı.
- **Yayımlanmamış kurs başkasına 404:** 403 dönseydi kursun var olduğu anlaşılırdı.

**Migration'lar**
- **E-posta tekilliği büyük/küçük harfe duyarsız** (`CREATE UNIQUE INDEX ... (lower(email))`): Bertan'ın review'unda `ali@x.com` ile `ALI@x.com`'un iki hesap açabildiği çıktı; migration merge edilmeden düzelttim.
- **Red ve yayından kaldırma gerekçesi ayrı kolonlarda** (`rejection_reason`, `unpublish_reason`): Veronika'nın FR-38 sorusu. Tek kolonda tutulsaydı yeniden onaya gönderimde hangisinin silineceği belirsizdi.
- **Dosya kolonlarının FK'si sonra:** `cover_file_id` ve `content_file_id` var, ama `stored_files` tablosu #111'de geleceği için FK'leri o migration'a bırakıldı.

**Observer ve olaylar (#42, #54, #43, #25, #112)**
- **Olaylar işlem tamamlanınca yayınlanıyor** (`AFTER_COMMIT`): Kayıt ya da sınav geri alınırsa ilerleme, bildirim gibi yan işler hiç çalışmıyor. Ayrıntı ADR-009'da.
- **Sınav puanlaması Strategy'de:** `scoringType` hangi stratejinin seçileceğini belirliyor; yeni puanlama kuralı yeni bir sınıf demek, serviste `if/switch` yok.
- **Ders ekleme izni kurs durumuna sorulur:** Taslak, Yayımlandı ve Yayından kaldırıldı durumunda ders eklenebilir, İncelemede 409 döner. Kural serviste değil durum nesnesinde (State deseni). Yayımlanmış kursa ders eklenince `LessonPublished` yayınlanıyor.
- **Dosya silme olayla ve commit'ten sonra:** Dersin içeriği değişince eski dosyanın silinmesi `ContentFileDetached` olayını dinleyen bir sınıfta. Silme başarısız olursa yalnız log yazılıyor ve diskte artık dosya kalabiliyor; bilinen bir ödünleşim.
- **Kapak görseli:** jpg, png, webp ve en çok 2 MB; tür dosyanın başındaki baytlardan anlaşılıyor, uzantıya güvenilmiyor.

**Eğitmen analitiği (#133, #134, #135, #257)**
- **Hesap kuralları açık yazıldı:** Ortalama ilerleme = ilerleme yüzdelerinin toplamı / kayıtlı sayısı (ilerleme satırı olmayan kayıt %0). Tamamlama oranı = %100 olanlar / kayıtlı. Sınav başarısı = geçen deneme / bütün deneme. Payda sıfırsa sonuç 0, iki ondalık.
- **Veri `course_progress` önbelleğinden okunuyor:** Her istekte bütün ders kayıtlarını taramıyor.
- **Modüller yalnız arayüzle konuşuyor:** `course` modülü sayıları kendi tanımladığı arayüzler üzerinden alıyor, `enrollment`/`progress`/`quiz` bunları uyguluyor. İstatistik DTO'ları `course/dto` altında (ArchUnit kuralı).
- **İlerleme dağılımı başta ekranda hesaplanıyordu** (#254), yalnız görünen sayfayı kapsıyordu. #257'de tek sorguyla backend'e taşıdım; 5 kovanın toplamı kayıt sayısına eşit.

**Ön yüz**
- **Sınav çözme ekranı (#57):** Tek cevaplı soru için radyo, çok cevaplı için onay kutusu seçimi `scoringType`'tan geliyor. Boş bırakılan soru olursa uyarı çıkıyor ve kişi geri dönebiliyor ya da yine de gönderebiliyor.
- **Katalog filtresi (#305):** En az puan seçimi adreste tutuluyor (`minRating`), sayfa yenilenince ya da bağlantı paylaşılınca korunuyor.

## Karşılaştığım problemler

- **İlk taslakta ilerleme ve sertifika tabloları `user_id`'ye bağlıydı.** Issue #40 ve #129 ile karşılaştırınca uyuşmadığını gördüm ve `enrollment_id`'ye çevirdim.
- **`/api/me` mi `/api/users/me` mi?** `CLAUDE.md` kendi kaynakları `/api/me/...` altında istiyor, issue #3, #14 ve #116 ise `/api/users/me` istiyor. Önce issue'ya uydum ve PR #154'te ekibe sordum. Backlog'daki diğer kullanıcı uçlarının hepsi `/api/me/...` altında olduğu için tutarlılık adına `/api/me`'ye geçtik.
- **#7'nin issue tanımında `bio` kolonu yoktu** (ER diyagramında vardı): profil ucu (#116) kullandığı için migration'a ER'e göre ekledim (#162).
- **Uzunluklar ER ile API'de farklıydı** (`full_name` 150/100, `email` 255/254): API'dekine eşitledim (#162).
- **Bilgisayarımda Java kurulu değil:** PlantUML diyagramını ve OpenAPI doğrulamasını Docker imajlarıyla çalıştırdım (`plantuml/plantuml`, `redocly/cli`).
- **Diyagramda üç modülün rengi birbirine çok yakındı,** tablolar karışıyordu. Renkleri ve tablo aralıklarını değiştirip yeniden render ettim.
- **Paralel merge'lerde çakışma:** Bertan squash merge yapınca açık dallarım çakıştı (çoğunlukla `architecture.md` ve ADR tablosu). Her seferinde ana dala göre yeniden dizip iki tarafın satırlarını korudum ve tam testi yeniden çalıştırdım.
- **Entegrasyon testleri Docker istiyor:** Testcontainers'lı testler Docker içinde `docker.sock` bağlanmadan çalışmıyor. İlk sayımlarım yalnız birim testlerini kapsadığı için bir IT hatasını kaçırmıştım; sonra hem `surefire` hem `failsafe` sonuçlarını sayıp IT'leri soket bağlayarak çalıştırdım.
- **Eksik `}` (#254):** `globals.css`'te bir medya sorgusunun kapanış parantezi yoktu, sonraki kurallar bloğun içine düşüp uygulanmıyordu; ekledim.
- **Bilinen eksik:** Tek cevaplı bir sınavda doğru seçenek sayısı 1'den farklı kaydedilirse sınav çözerken 500 dönüyor. Henüz düzeltilmedi.
