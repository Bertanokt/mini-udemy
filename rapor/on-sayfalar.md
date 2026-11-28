<!--
Raporun ön sayfaları (#69). Word dosyası scripts/rapor/rapor.py ile bu dosyadan, bölüm dosyalarından ve kaynaklar.md'den üretilir.
- Sıra ve kapak düzeni hocanın şablonundaki gibidir: Kapak → Önsöz → Summary → Özet → İçindekiler → Şekil listesi → Kısaltmalar.
- "::: {custom-style=...}" blokları kapak sayfasıdır. Satır sonundaki "\" aynı paragrafta alt satıra geçer.
  "Kapak Uye" bloğunda her üye iki satırdır (AD SOYAD, öğrenci no); betik bunları şablondaki gibi iki sütuna dizer.
  Şablon verilirse (--sablon) kapağın başına şablondaki logo konur.
- KAPAK SONU, İÇİNDEKİLER ve ŞEKİL LİSTESİ yorum satırları betiğin işaretleridir; silmeyin.
- ⟨…⟩ yer tutucuları Word'de sarıyla işaretlenir; betik listesini yazar.
-->

::: {custom-style="Kapak Kurum"}
DOĞUŞ ÜNİVERSİTESİ
:::

::: {custom-style="Kapak Birim"}
MÜHENDİSLİK ve DOĞA BİLİMLERİ FAKÜLTESİ\
YAZILIM MÜHENDİSLİĞİ
:::

::: {custom-style="Kapak Baslik"}
MİNİ UDEMY\
ÇEVRİM İÇİ EĞİTİM PLATFORMU
:::

::: {custom-style="Kapak Ders"}
YM 341 YAZILIM TASARIMI VE MİMARİSİ DERSİ PROJESİ
:::

::: {custom-style="Kapak Etiket"}
HAZIRLAYAN
:::

::: {custom-style="Kapak Uye"}
BARAN BERTAN ÖKTEM

202403011018

SEDAT ⟨SOYADI⟩

⟨öğrenci no⟩

VERONİKA ⟨SOYADI⟩

⟨öğrenci no⟩

ERAY YUSUF YARTUNÇ

⟨öğrenci no⟩
:::

::: {custom-style="Kapak Danisman"}
DANIŞMAN\
Dr. Öğr. Üyesi Ertuğrul Kıraç
:::

::: {custom-style="Kapak Tarih"}
İSTANBUL, ARALIK 2026
:::

<!-- KAPAK SONU -->

# ÖNSÖZ

Bu rapor, Yazılım Tasarımı ve Mimarisi dersinin dönem projesi olarak geliştirilen Mini Udemy çevrim içi eğitim platformunu anlatır. Proje 21 Ekim – 2 Aralık 2026 arasında dört kişilik bir ekiple yapıldı. Ekipte her üyenin bir sorumluluk alanı vardı: yazılım mimarisi ve ekip koordinasyonu, alan tasarımı, kalite ve süreç, veri ve entegrasyon.

Projede amacımız yalnız çalışan bir ürün çıkarmak değildi. Gereksinimlerden kullanım senaryolarına, oradan mimariye, tasarım desenlerine ve koda kadar izlenebilen, diyagramları ve API sözleşmesi kodla tutarlı bir sistem kurmak istedik. Bu yüzden mimari kararları kayıt altına aldık, kuralların bir kısmını testlerle denetledik ve her değişikliği kod incelemesinden geçirdik.

Kod ve doküman yazımında yapay zekâ destekli bir kodlama aracı (Claude Code) kullandık. Aracın nasıl kullanıldığı ve bunun getirdiği riskler 8.4'te tartışılmıştır.

Proje boyunca yol gösteren ve geri bildirim veren ders sorumlumuz Dr. Öğr. Üyesi Ertuğrul Kıraç'a teşekkür ederiz.

Grup 12 — İstanbul, Aralık 2026

# SUMMARY

This report describes the design and implementation of Mini Udemy, an online learning platform developed for the Software Design and Architecture course. On the platform, instructors prepare courses made of videos, PDFs and documents and submit them for administrator approval. Learners enroll in published courses free of charge, follow the lessons, take end-of-section quizzes and receive a certificate with a verification code once they meet the requirements. Administrators review courses before publication and manage users, categories, reviews and complaints. The scope covers the 12 modules and 5 core scenarios of the project definition, broken down into 44 functional and 19 non-functional requirements and 34 use cases.

The system consists of a Next.js / React user interface, a Spring Boot REST API and a PostgreSQL database, and runs with a single Docker Compose command. The backend is a layered, modular monolith of 10 business modules; module boundaries are checked in continuous integration with ArchUnit tests. The State, Observer, Strategy, Factory Method and DTO + Mapper patterns were applied where there was a concrete need. Twelve architectural decisions were recorded as ADRs, and the API is defined by an OpenAPI contract.

According to the test report, the project has 1194 automated tests, and line coverage of the service layer is 98.2%. The report also covers traceability from requirements to code and tests, the project plan, the constraints and future work.

**Keywords:** online learning platform, modular monolith, design patterns, architecture decision record, Spring Boot

# ÖZET

Bu rapor, Yazılım Tasarımı ve Mimarisi dersi kapsamında geliştirilen Mini Udemy çevrim içi eğitim platformunun tasarımını ve gerçekleştirimini anlatır. Platformda eğitmenler video, PDF ve dokümanlardan oluşan kurslar hazırlayıp yönetici onayına gönderir. Kursiyerler yayımlanmış kurslara ücretsiz kaydolur, dersleri izler, bölüm sonu sınavlarına girer ve koşulları tamamlayınca doğrulama kodlu bir sertifika alır. Yöneticiler kursları yayından önce inceler; kullanıcıları, kategorileri, yorumları ve şikâyetleri yönetir. Kapsam, proje tanım dokümanındaki 12 modül ve 5 temel senaryodur. Bunlar 44 fonksiyonel ve 19 fonksiyonel olmayan gereksinime ve 34 kullanım durumuna ayrıldı.

Sistem bir Next.js / React arayüzü, bir Spring Boot REST API'si ve bir PostgreSQL veritabanından oluşur ve Docker Compose ile tek komutla çalışır. Backend, 10 iş modülünden oluşan katmanlı ve modüler bir monolittir; modül sınırları ArchUnit testleriyle sürekli entegrasyonda denetlenir. State, Observer, Strategy, Factory Method ve DTO + Mapper desenleri somut ihtiyaçlar için kullanıldı. 12 mimari karar ADR olarak kayda geçirildi; API bir OpenAPI sözleşmesiyle tanımlandı.

Test raporuna göre projede 1194 otomatik test vardır ve servis katmanının satır kapsamı %98,2'dir. Rapor ayrıca gereksinimden koda ve teste izlenebilirliği, proje planını, kısıtları ve gelecek çalışmaları içerir.

**Anahtar kelimeler:** çevrim içi eğitim platformu, modüler monolit, tasarım desenleri, mimari karar kaydı, Spring Boot

<!-- İÇİNDEKİLER -->

<!-- ŞEKİL LİSTESİ -->

# KISALTMALAR

| Kısaltma | Açıklama |
|---|---|
| ADR | Architecture Decision Record (mimari karar kaydı) |
| API | Application Programming Interface (uygulama programlama arayüzü) |
| C4 | Context, Container, Component, Code: yazılım mimarisi diyagram modeli |
| CDN | Content Delivery Network (içerik dağıtım ağı) |
| CI | Continuous Integration (sürekli entegrasyon) |
| CSP | Content Security Policy (içerik güvenliği politikası) |
| DTO | Data Transfer Object (veri aktarım nesnesi) |
| ER | Entity–Relationship (varlık–ilişki) |
| FR | Fonksiyonel gereksinim |
| HLS | HTTP Live Streaming |
| HTTP | Hypertext Transfer Protocol |
| JPA | Jakarta Persistence API |
| JSON | JavaScript Object Notation |
| JWT | JSON Web Token |
| NFR | Fonksiyonel olmayan gereksinim |
| PDF | Portable Document Format |
| PR | Pull request (değişiklik isteği) |
| REST | Representational State Transfer |
| SaaS | Software as a Service (hizmet olarak yazılım) |
| SMTP | Simple Mail Transfer Protocol |
| SOLID | Single responsibility, Open–closed, Liskov substitution, Interface segregation, Dependency inversion ilkeleri |
| SQL | Structured Query Language |
| UC | Use case (kullanım durumu) |
| UI | User interface (kullanıcı arayüzü) |
| UML | Unified Modeling Language |
| UUID | Universally Unique Identifier |
| WCAG | Web Content Accessibility Guidelines |
| XSS | Cross-Site Scripting |
