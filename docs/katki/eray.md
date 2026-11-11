# Katkı — Eray (Veri ve Entegrasyon Sorumlusu)

GitHub: [@eraygame113-beep](https://github.com/eraygame113-beep)

## Yaptığım işler

| Issue | PR | Çıktı |
|---|---|---|
| [#2 ER diyagramı (tüm modüller)](https://github.com/Bertanokt/mini-udemy/issues/2) | [#153](https://github.com/Bertanokt/mini-udemy/pull/153) | `docs/diagrams/er.puml` + `er.svg` / `er.png`: 10 modül, 20 tablo, PK/FK/unique/CHECK kısıtları |
| [#3 API sözleşmesi: auth, profil, parola yenileme](https://github.com/Bertanokt/mini-udemy/issues/3) | [#154](https://github.com/Bertanokt/mini-udemy/pull/154) | `docs/api/openapi.yaml`: register, login, parola yenileme (request/confirm), `GET/PUT /api/me` |

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

## Karşılaştığım problemler

- **İlk taslakta ilerleme ve sertifika tabloları `user_id`'ye bağlıydı.** Issue #40 ve #129 ile karşılaştırınca uyuşmadığını gördüm ve `enrollment_id`'ye çevirdim.
- **`/api/me` mi `/api/users/me` mi?** `CLAUDE.md` kendi kaynakları `/api/me/...` altında istiyor, issue #3, #14 ve #116 ise `/api/users/me` istiyor. Önce issue'ya uydum ve PR #154'te ekibe sordum. Backlog'daki diğer kullanıcı uçlarının hepsi `/api/me/...` altında olduğu için tutarlılık adına `/api/me`'ye geçtik.
- **`users` tablosunda `bio` kolonu yok:** Profil ucu (#116) `bio` alanını kullanıyor, ama #7'deki migration tanımında bu alan yok. #7'de eklenmesi gerekiyor.
- **Bilgisayarımda Java kurulu değil:** PlantUML diyagramını ve OpenAPI doğrulamasını Docker imajlarıyla çalıştırdım (`plantuml/plantuml`, `redocly/cli`).
- **Diyagramda üç modülün rengi birbirine çok yakındı,** tablolar karışıyordu. Renkleri ve tablo aralıklarını değiştirip yeniden render ettim.
