# Katkı — Eray (Veri ve Entegrasyon Sorumlusu)

GitHub: [@eraygame113-beep](https://github.com/eraygame113-beep)

## Yaptığım işler

| Issue | PR | Çıktı |
|---|---|---|
| [#2 ER diyagramı (tüm modüller)](https://github.com/Bertanokt/mini-udemy/issues/2) | [#153](https://github.com/Bertanokt/mini-udemy/pull/153) | `docs/diagrams/er.puml` + `er.svg` / `er.png`: 10 modül, 20 tablo, PK/FK/unique/CHECK kısıtları |
| [#3 API sözleşmesi: auth, profil, parola yenileme](https://github.com/Bertanokt/mini-udemy/issues/3) | [#154](https://github.com/Bertanokt/mini-udemy/pull/154) | `docs/api/openapi.yaml`: register, login, parola yenileme (request/confirm), `GET/PUT /api/me` |
| [#7 Flyway migration: users](https://github.com/Bertanokt/mini-udemy/issues/7) | [#162](https://github.com/Bertanokt/mini-udemy/pull/162) | İlk migration: `users` tablosu; e-posta büyük/küçük harf duyarsız tekil (`lower(email)`), rol CHECK kısıtı |
| [#20 Flyway migration: kurs yapısı](https://github.com/Bertanokt/mini-udemy/issues/20) | [#163](https://github.com/Bertanokt/mini-udemy/pull/163) | `categories`, `courses`, `sections`, `lessons`; seviye/durum/içerik türü CHECK, bölüm ve derslerde CASCADE |
| [#4](https://github.com/Bertanokt/mini-udemy/issues/4), [#5](https://github.com/Bertanokt/mini-udemy/issues/5), [#108](https://github.com/Bertanokt/mini-udemy/issues/108) API sözleşmesinin kalanı | [#165](https://github.com/Bertanokt/mini-udemy/pull/165) | Tüm modüllerin uçları (57 adres, 73 işlem); her uçta rol (`x-roles`) |

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

## Karşılaştığım problemler

- **İlk taslakta ilerleme ve sertifika tabloları `user_id`'ye bağlıydı.** Issue #40 ve #129 ile karşılaştırınca uyuşmadığını gördüm ve `enrollment_id`'ye çevirdim.
- **`/api/me` mi `/api/users/me` mi?** `CLAUDE.md` kendi kaynakları `/api/me/...` altında istiyor, issue #3, #14 ve #116 ise `/api/users/me` istiyor. Önce issue'ya uydum ve PR #154'te ekibe sordum. Backlog'daki diğer kullanıcı uçlarının hepsi `/api/me/...` altında olduğu için tutarlılık adına `/api/me`'ye geçtik.
- **#7'nin issue tanımında `bio` kolonu yoktu** (ER diyagramında vardı): profil ucu (#116) kullandığı için migration'a ER'e göre ekledim (#162).
- **Uzunluklar ER ile API'de farklıydı** (`full_name` 150/100, `email` 255/254): API'dekine eşitledim (#162).
- **Bilgisayarımda Java kurulu değil:** PlantUML diyagramını ve OpenAPI doğrulamasını Docker imajlarıyla çalıştırdım (`plantuml/plantuml`, `redocly/cli`).
- **Diyagramda üç modülün rengi birbirine çok yakındı,** tablolar karışıyordu. Renkleri ve tablo aralıklarını değiştirip yeniden render ettim.
