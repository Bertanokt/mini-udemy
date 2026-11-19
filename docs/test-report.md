# Test raporu (#99)

> **TASLAK (19.11.2026).** Bug bash (#68) bulguları ve JaCoCo coverage sayıları eklenince tamamlanacak. Sayılar aşağıdaki CI çalışmasından alınmıştır.

Test planına ([test-plan.md](test-plan.md)) göre gerçekleşen sonuçlar. FR → issue → test izlenebilirliği [traceability.md](traceability.md)'de.

**Kaynak:** `main`, commit `ee2012b`, CI çalışması [44549756895](https://github.com/Bertanokt/mini-udemy/actions/runs/44549756895) (19.11.2026, başarılı).

## 1. Özet

| | Sayı | Sonuç |
|---|---|---|
| Birim testleri (surefire, `*Test`) | 893 | 0 hata |
| Mimari testler (ArchUnit, `common.architecture`) | 13 | 0 ihlal |
| Entegrasyon testleri (failsafe, `*IT`, gerçek PostgreSQL 17) | 147 | 0 hata |
| **Toplam** | **1053** | **CI yeşil** |
| Frontend | lint + build | yeşil (FE test aracı yok, proje kuralı) |

Test sayısının gelişimi: 10.11'de 1, 17.11 sonunda 404, 19.11'de 1053 (ilerleme raporları).

### Modül bazında birim + mimari testler

| Modül | Test |
|---|---|
| course | 359 |
| quiz | 167 |
| content | 86 |
| common (güvenlik, hata yönetimi, ArchUnit) | 73 |
| auth | 45 |
| progress | 42 |
| review | 37 |
| admin | 32 |
| enrollment | 30 |
| notification | 21 |
| certificate | 14 |

## 2. Coverage (JaCoCo, #89)

Hedef (test-plan › 7.4): servis katmanında (`com.miniudemy.*.service`) satır coverage'ı en az %70.

| Kapsam | Satır coverage | Hedef |
|---|---|---|
| Servis katmanı | _doldurulacak_ | ≥ %70 |
| Genel | _doldurulacak_ | bilgi amaçlı |

Rapor her CI çalışmasında `jacoco-report` artifact'ı olarak üretiliyor; sayılar son `main` çalışmasının artifact'ından alınacak.

## 3. Senaryo 1–5 entegrasyon testleri

| Senaryo | Test | Sonuç | Not |
|---|---|---|---|
| 1. Kurs yayımlama | `CoursePublishingScenarioIT` (3), `ContentUploadIT` (11), `CourseEditDeleteIT` (3), `CourseUnpublishIT` (2) | ✓ | Gerçek yükleme ucuyla; gerekçeli red, yetkisiz düzenleme, yayından kaldırma |
| 2. Kayıt ve öğrenme | `EnrollmentScenarioIT` (2) | ✓ | Arama → kayıt → içerik (Range 206) → tamamlama → ilerleme |
| 3. Kaldığı yerden devam | `ResumeScenarioIT` (2) | ✓ | Son açılan ders, ilk tamamlanmamış ders, eğitmen açınca değişmez |
| 4. Sınav ve sertifika | `QuizCertificateScenarioIT` (2), `CertificateScenarioIT` (2), `NotificationScenarioIT` (1) | ✓ (kısmi) | Sertifika görüntüleme ve herkese açık doğrulama ucu henüz yok (#130) |
| 5. Eğitmen analitiği | `InstructorAnalyticsIT` (1), `InstructorAnalyticsScenarioIT` (1), `PlatformReportIT` (2) | ✓ | |
| Seed verisi | `DemoSeedIT` (6) | ✓ | Demo hesapları README'deki parolayla girer |

## 4. Bölüm 6 kritik test durumları

| Durum | Beklenen | Test | Sonuç |
|---|---|---|---|
| Kursa kayıt olmadan özel derse erişim | 403 | `Bolum6CriticalCasesIT.case1_…` + birim `ContentDeliveryServiceTest` | ✓ |
| Mükerrer kayıt | 409, tek satır | `case2_…` + `EnrollmentServiceTest` | ✓ |
| Yetkisiz kurs düzenleme | 403 | `case3_…` + `CourseServiceTest`, `SectionServiceTest`, `LessonServiceTest` | ✓ |
| Yanlış ilerleme hesabı | 15/20 → %75, 0 → %0 | `case4_…` + `ProgressServiceTest` | ✓ |
| Dosya türü / boyutu | 415 / 413 | `case5_…` + `LocalDiskStorageServiceTest`, `ContentUploadIT` | ✓ |
| Rol × uç erişim matrisi | girişsiz 401, yanlış rol 403, sözleşme = kod | `AccessMatrixIT` (102 kontrol) | ✓ |

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

## 6. Bug bash bulguları (#68)

_Bug bash yapılınca doldurulacak:_ bulunan hata, öncelik (P1/P2), issue, durum.

## 7. Başarı ölçütleri (test-plan › 7)

| Ölçüt | Durum |
|---|---|
| 1. `main`'de CI yeşil | ✓ |
| 2. Tüm P1 FR'lerin otomatik testi ya da yazılı manuel kontrolü | izlenebilirlik matrisinde (#98) güncellenecek |
| 3. Bölüm 6 testleri mevcut ve yeşil, `@Disabled` yok | ✓ |
| 4. Servis katmanında satır coverage ≥ %70 | _coverage eklenince_ |
| 5. ArchUnit ihlali yok, kurallar gevşetilmemiş | ✓ (13 mimari test) |
| 6. Bug bash'teki P1 hataların hepsi kapalı | _bug bash sonrası_ |
| 7. Demo provasında senaryo 1–5 hatasız | _#106_ |
