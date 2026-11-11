# ADR-001: Katmanlı modüler monolit ve modül bazlı paketleme

- **Durum:** Kabul edildi
- **Karar tarihi:** 18.10.2026 (proje planlaması)
- **Kayıt tarihi:** 11.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #80, #6 (`docs/architecture.md`), #86 (ArchUnit)

## Bağlam

Mini Udemy; kurs, içerik, kayıt, ilerleme, sınav, sertifika, puanlama, bildirim ve yönetim alanlarını (proje dokümanı, modüller A–L) kapsayan bir web platformu. Kısıtlar:

- **Ekip ve süre:** 4 kişi, yaklaşık 5 haftalık geliştirme; sunum 02.12.2026.
- **Proje dokümanı (§6):** "Katmanlı, modüler monolit mimari önerilir. Kurs, içerik, kayıt, ilerleme, sınav, sertifika ve bildirim alanları ayrı modüller halinde düzenlenecektir."
- **Değerlendirme:** notun önemli bir kısmı mimari kararların gerekçelendirilmesine ve UML diyagramlarının kodla tutarlılığına dayanıyor; modül sınırlarının kodda açıkça görünmesi gerekiyor.
- **Teknoloji:** Spring Boot 3 (Java 21), PostgreSQL, Docker Compose; ayrı Next.js frontend.

İki soruya karar vermek gerekiyordu: sistemi kaç dağıtım birimine böleceğiz, ve kodu paketlere nasıl ayıracağız.

## Karar

Backend **tek bir dağıtım birimi olan katmanlı modüler monolit** olarak geliştirilir ve kod **önce modüle, sonra katmana** göre paketlenir: `com.miniudemy.<modül>.<katman>`.

- 10 modül: `auth`, `course`, `content`, `enrollment`, `progress`, `quiz`, `certificate`, `review`, `notification`, `admin`; ortak altyapı `common`.
- Her modülün içi aynı katmanlara ayrılır: `controller`, `service`, `repository`, `dto`, `mapper`, `domain`.
- Modüller birbirleriyle yalnız `service` arayüzleri ve domain olayları üzerinden konuşur.
- Tek bir PostgreSQL veritabanı kullanılır.

Kuralların ayrıntısı: [`docs/architecture.md`](../architecture.md).

## Değerlendirilen alternatifler

### Mikroservisler

Her alan (kurs, kayıt, sınav…) ayrı servis, ayrı veritabanı, servisler arası REST ya da mesajlaşma.

- **Artıları:** bağımsız dağıtım ve ölçekleme, servis başına teknoloji seçimi, güçlü sınırlar.
- **Neden seçilmedi:** 4 kişilik ekip ve kısa süre için dağıtım, servisler arası iletişim ve dağıtık veri tutarlılığı (örn. kayıt ile ilerleme arasındaki tutarlılık) fayda getirmeden yük getiriyor. Bu ölçekte ayrı ölçekleme ihtiyacı yok. Proje dokümanı da monoliti öneriyor.

### Klasik katmanlı monolit (katmanlar en üstte paketleme)

`controller/`, `service/`, `repository/` en üst paketler; tüm alanların sınıfları bu paketlerin içinde.

- **Artıları:** yaygın ve basit, başlangıçta hızlı.
- **Neden seçilmedi:** bir modülün kodu tüm projeye dağılıyor ve modül sınırları görünmez oluyor. Hangi sınıfın hangi alana ait olduğu ve modüller arası bağımlılıklar paket yapısından okunamıyor; bu da sınırların test ile korunmasını zorlaştırıyor.

## Sonuçlar

**Olumlu:**

- Tek dağıtım ve tek veritabanı: kurulum (`docker compose up`), test ve demo basit; modüller arası çağrılar sıradan metot çağrısı ve tek transaction içinde tutarlı.
- Modül sınırları paket yapısında görünüyor; her modül kendi içinde katmanlı.
- Modüller arası bağımlılıklar belgelenmiş ve testle korunabiliyor; bileşen diyagramı paket yapısıyla birebir eşleşebiliyor.
- Sınırlar net tutulduğu için ileride bir modülü ayrı servise ayırmak mümkün.

**Olumsuz / ödünleşimler:**

- Tek veritabanında modüller arası yabancı anahtarlar oluşacak; ileride bir modülü ayırmak bu yüzden maliyetli olabilir. JPA ilişkileri yalnız aynı modül içinde kurularak bu bağ sınırlı tutuluyor.
- Tüm modüller birlikte dağıtılır ve ölçeklenir; bir modüldeki hata tüm uygulamayı etkileyebilir.
- Modül sınırları dil düzeyinde zorunlu değil (aynı derleme birimi); disiplin gerektiriyor.

**Kararın kodda nasıl korunduğu:**

- Katman ve modül bağımlılık kuralları ArchUnit testleriyle CI'da kontrol edilir (#86); ihlal build'i kırar.
- `docs/architecture.md` izin verilen modül bağımlılıklarını listeler; yeni bir bağımlılık önce o dokümanın güncellenmesini gerektirir.
- PR review'larında modül sınırına uyum kontrol edilir.
