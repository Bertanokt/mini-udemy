# Test raporu (#99)

> **TASLAK (21.11.2026; ilk sürüm 19.11).** Bug bash (#68) bulguları ve demo provası (#106) eklenince tamamlanacak. Test sayıları aşağıdaki CI çalışmasından alınmıştır; coverage 19.11 ölçümüdür ve 01.12'den önce yeniden ölçülecek.

Test planına ([test-plan.md](test-plan.md)) göre gerçekleşen sonuçlar. FR → issue → test izlenebilirliği [traceability.md](traceability.md)'de.

**Kaynak:** `main`, commit `a27bcf7`, CI çalışması [44937251944](https://github.com/Bertanokt/mini-udemy/actions/runs/44937251944) (21.11.2026, başarılı). Coverage: commit `ee2012b`, CI çalışması [44549756895](https://github.com/Bertanokt/mini-udemy/actions/runs/44549756895) (19.11.2026).

## 1. Özet

| | Sayı | Sonuç |
|---|---|---|
| Birim testleri (surefire, `*Test`) | 993 | 0 hata |
| Mimari testler (ArchUnit, `common.architecture`) | 13 | 0 ihlal |
| Entegrasyon testleri (failsafe, `*IT`, gerçek PostgreSQL 17) | 182 | 0 hata |
| **Toplam** | **1188** | **CI yeşil** |
| Frontend | lint + build | yeşil (FE test aracı yok, proje kuralı) |

Test sayısının gelişimi: 10.11'de 1, 17.11 sonunda 404, 19.11'de 1053, 21.11'de 1188 (ilerleme raporları). 19.11'den sonraki artışın kaynağı: sertifika görüntüleme ve doğrulama (#130), bildirim API'si (#125), parola yenileme ve profil (#115, #116), yorum moderasyonu (#142), şikâyetler (#144), istemcinin kestiği bağlantı (#259) ve bileşen diyagramı testi (#91).

### Modül bazında birim + mimari testler

| Modül | Test |
|---|---|
| course | 359 |
| quiz | 167 |
| content | 86 |
| common (güvenlik, hata yönetimi, ArchUnit, bileşen diyagramı) | 81 |
| auth | 71 |
| admin | 58 |
| review | 45 |
| progress | 42 |
| notification | 39 |
| enrollment | 32 |
| certificate | 26 |
| **Toplam** | **1006** |

## 2. Coverage (JaCoCo, #89)

Hedef (test-plan › 7.4): servis katmanında (`com.miniudemy.*.service`) satır coverage'ı en az %70. Aşağıdaki değerler **19.11** ölçümüdür; o günden sonra eklenen servisler (`CertificateQueryService`, `PasswordResetService`, `ReviewModerationService`, `ComplaintService`, bildirim okuma) kendi birim ve entegrasyon testleriyle geldi. ⟨01.12'den önce son CI'ın `jacoco-report`'u ile yeniden ölçülecek.⟩

| Kapsam | Satır coverage | Dal (branch) coverage | Hedef |
|---|---|---|---|
| **Servis katmanı** (`*.service`) | **%98,4** (1347 satırın 1326'sı) | %93,9 | ≥ %70 ✓ |
| Genel | %96,1 (2952 satırın 2836'sı) | %80,7 | bilgi amaçlı |

Modül bazında servis katmanı: admin, auth, certificate, enrollment, notification, progress, review %100; quiz %99,2; course %98,9; content %93,1. Servis katmanında %90'ın altında kalan tek sınıf `LocalDiskStorageService` (%85,1; kapsanmayan 10 satır çoğunlukla disk G/Ç hata dalları).

Kaynak: aynı CI çalışmasının `jacoco-report` artifact'ı (`jacoco.csv`). JaCoCo ajanı birim ve entegrasyon testlerinde aynı `jacoco.exec`'e yazar; rapor `verify` aşamasında, entegrasyon testlerinden sonra üretilir.

## 3. Senaryo 1–5 entegrasyon testleri

| Senaryo | Test | Sonuç | Not |
|---|---|---|---|
| 1. Kurs yayımlama | `CoursePublishingScenarioIT` (3), `ContentUploadIT` (11), `CourseEditDeleteIT` (3), `CourseUnpublishIT` (2) | ✓ | Gerçek yükleme ucuyla; gerekçeli red, yetkisiz düzenleme, yayından kaldırma |
| 2. Kayıt ve öğrenme | `EnrollmentScenarioIT` (2) | ✓ | Arama → kayıt → içerik (Range 206) → tamamlama → ilerleme |
| 3. Kaldığı yerden devam | `ResumeScenarioIT` (2) | ✓ | Son açılan ders, ilk tamamlanmamış ders, eğitmen açınca değişmez |
| 4. Sınav ve sertifika | `QuizCertificateScenarioIT` (2), `CertificateScenarioIT` (3), `NotificationScenarioIT` (1), `NotificationApiIT` (2) | ✓ | Senaryo testi (#138) kursiyerin akışını sertifikadan sonra da sürdürüyor: Sertifikalarım, girişsiz doğrulama, bildirim listesi ve okunmamış sayısı |
| 5. Eğitmen analitiği | `InstructorAnalyticsIT` (1), `InstructorAnalyticsScenarioIT` (1), `PlatformReportIT` (2) | ✓ | |
| Seed verisi | `DemoSeedIT` (7) | ✓ | Demo hesapları README'deki parolayla girer; örnek bildirimler uygulamanın metinleriyle aynı |
| Diğer uçtan uca | `PasswordResetIT` (4), `ProfileApiIT` (1), `ReviewModerationIT` (2), `ComplaintIT` (4), `CourseLearningOutcomesIT` (2) | ✓ | Parola yenileme (tek kullanım, süre, varlık sızmaması), profil, yorum gizleme, şikâyet |

## 4. Bölüm 6 kritik test durumları

| Durum | Beklenen | Test | Sonuç |
|---|---|---|---|
| Kursa kayıt olmadan özel derse erişim | 403 | `Bolum6CriticalCasesIT.case1_…` + birim `ContentDeliveryServiceTest` | ✓ |
| Mükerrer kayıt | 409, tek satır | `case2_…` + `EnrollmentServiceTest` | ✓ |
| Yetkisiz kurs düzenleme | 403 | `case3_…` + `CourseServiceTest`, `SectionServiceTest`, `LessonServiceTest` | ✓ |
| Yanlış ilerleme hesabı | 15/20 → %75, 0 → %0 | `case4_…` + `ProgressServiceTest` | ✓ |
| Dosya türü / boyutu | 415 / 413 | `case5_…` + `LocalDiskStorageServiceTest`, `ContentUploadIT` | ✓ |
| Rol × uç erişim matrisi | girişsiz 401, yanlış rol 403, sözleşme = kod | `AccessMatrixIT` (122 kontrol) | ✓ |

Bu testler `@Disabled` değildir ve silinmez (CLAUDE.md).

## 5. Kırmızı kontrol (testlerin doğru sebeple kırıldığı)

Her test issue'sunda kod bilerek bozuldu, ilgili testlerin CI'da kırıldığı görüldü, sonra geri alındı. Ayrıntılar issue/PR yorumlarında.

| Issue | Bozma | Yakalanan | Bulunan test boşluğu |
|---|---|---|---|
| #24 Bölüm CRUD | 3 | 3 | – |
| #65 Senaryo 1–3 | 3 + 2 + 3 + 2 | hepsi | Senaryo 1: ders sırasının veritabanına yazılmadığını yalnız yeniden okuyan test yakaladı |
| #32 Kayıt | 3 | 3 | – |
| #128 Değerlendirme | 4 | 4 | – |
| #56 Puanlama | 5 | 5 | **Servisin strateji seçimini hiçbir test denetlemiyordu**; 2 test eklendi |
| #44 İlerleme | 5 | 5 | %0 ve %100 uç durumları eksikti; 2 test eklendi |
| #114 İçerik/depolama | 4 | 4 | – |
| #132 Sertifika | 5 | 5 | – |
| #61 Admin/durum geçişleri | 6 | 6 | – |
| #31 Kurs/State | 6 | 6 | – |
| #139 Bölüm 6 + matris | 7 | 7 | – |
| #67 Seed | 4 | 4 | – |
| #145 Platform raporu | 6 | 5 → 6 | Tamamlanma sayımını test verisi denetlemiyordu; %50'lik kayıt eklendi |
| #138 Senaryo 4–5 | 3 | 3 | – |
| #137 Bildirim | 5 | 4 | 1 eşdeğer bozma (`distinct()`, veritabanı kısıtı zaten engelliyor) |
| #137 Teslim kuralı (PR #285) | 2 | 2 | **Dinleyici `@EventListener` yapılınca mevcut 1054 testin hepsi yeşil kalıyordu**; `NotificationDeliveryTest` eklendi |
| #138 Senaryo 4 sertifika + bildirim (PR #312) | 2 | 2 | – |

Diğer üyelerin PR'larında da aynı yöntem kullanıldı ve PR açıklamalarına yazıldı; örnekler: #303 bildirim API'si (5 bozma), #322 parola yenileme (6 bozma), #315 "hepsini okundu" kapsamı (review'da bulunan açık), #326 istemcinin kestiği bağlantı.

## 6. Bug bash bulguları (#68)

_Bug bash yapılınca doldurulacak:_ bulunan hata, öncelik (P1/P2), issue, durum.

## 7. Başarı ölçütleri (test-plan › 7)

| Ölçüt | Durum |
|---|---|
| 1. `main`'de CI yeşil | ✓ |
| 2. Tüm P1 FR'lerin otomatik testi ya da yazılı manuel kontrolü | ✓ Matriste (#98) FR 43/44 otomatik testli; tek istisna FR-29 (akreditasyon ibaresi, ön yüzde sabit metin), manuel kontrolü bug bash'te (#68) |
| 3. Bölüm 6 testleri mevcut ve yeşil, `@Disabled` yok | ✓ |
| 4. Servis katmanında satır coverage ≥ %70 | ✓ %98,4 (19.11; ⟨01.12'de yeniden⟩) |
| 5. ArchUnit ihlali yok, kurallar gevşetilmemiş | ✓ (13 mimari test; bileşen diyagramı da CI'da kodla karşılaştırılıyor, `ComponentDiagramTest`) |
| 6. Bug bash'teki P1 hataların hepsi kapalı | _bug bash sonrası_ |
| 7. Demo provasında senaryo 1–5 hatasız | 21.11'de temiz kurulumda beş senaryo ekranda yüründü (#70, Bertan); o gün eksik çıkan ekranlar aynı gün tamamlandı. ⟨Demo provası #106⟩ |
