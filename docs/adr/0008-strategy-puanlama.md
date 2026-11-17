# ADR-008: Strategy deseni ile sınav puanlama

- **Durum:** Kabul edildi
- **Karar tarihi:** 17.11.2026 (#52 üzerinde çalışırken; desen seçimi #52'nin kabul kriteriydi)
- **Kayıt tarihi:** 17.11.2026
- **Karar verenler:** Bertan (Yazılım Mimarı)
- **İlgili:** #52, #54 (sınav çözme servisi, henüz yok), [ADR-001](0001-moduler-monolit.md), [`docs/architecture.md`](../architecture.md) › Tasarım desenleri

## Bağlam

Sınavın puanlama kuralı sınav bazında seçilir (`quizzes.scoring_type`: `SINGLE_ANSWER`, `MULTI_ALL_OR_NOTHING`, `MULTI_PARTIAL`; enum `ScoringType`). Üç kural bugün var; negatif puan ya da ağırlıklı soru gibi yenilerinin eklenmesi bekleniyor. Cevapları puanlayacak servis (#54) henüz yazılmadı; kural nerede duracak sorusu şimdi cevaplanırsa servis baştan doğru kurulur.

## Karar

Puanlama **Strategy** deseniyle yazılır; kodu `quiz/service` paketindedir.

- `ScoringStrategy`: iki metotlu arayüz. `type()` stratejinin hangi `ScoringType`'a karşılık geldiğini söyler, `score(ScoringInput)` tek bir soruyu puanlar ve `QuestionScore` döner.
- Girdi ve çıktı sade `record`'lardır, entity değildir: `ScoringInput` (soru puanı, doğru şık id'leri, seçilen şık id'leri), `QuestionScore` (alınan puan, en yüksek puan). Stratejiler JPA'ya ve veritabanına bağımlı değildir; servis entity'den girdiyi kurar.
- Üç uygulama Spring bean'idir: `SingleAnswerScoring`, `MultiAnswerAllOrNothing`, `MultiAnswerPartialScoring`.
- `ScoringStrategyResolver` bean listesini alır, her stratejiyi kendi `type()` değeriyle bir `EnumMap`'e koyar; `resolve(ScoringType)` if/switch olmadan eşlemeden okur. Bir tip için strateji yoksa ya da iki strateji aynı tipi iddia ederse constructor `IllegalStateException` fırlatır, yani uygulama başlamaz.

**Kuralların tanımı** (`ScoringInput`: P = soru puanı, D = doğru şık kümesi, S = seçilen şık kümesi):

| Strateji | Puan |
|---|---|
| `SingleAnswerScoring` | S, D ile aynıysa P, değilse 0. Soruda tam bir doğru şık olmalı; aksi halde `IllegalArgumentException`. Fazla seçim ya da boş cevap 0. |
| `MultiAnswerAllOrNothing` | S, D ile birebir aynıysa P, değilse 0. Doğru şıklara fazladan bir yanlış eklemek de 0 verir. |
| `MultiAnswerPartialScoring` | P × max(0, doğru seçilen − yanlış seçilen) / \|D\|; iki ondalığa yarım yukarı yuvarlanır. Her yanlış seçim bir doğru seçimin kazandırdığını geri alır, puan 0'ın altına inmez. Boş cevap 0, tüm doğrular ve yanlışsız seçim P. |

Kısmi kuralın `ScoringType.MULTI_PARTIAL` açıklaması ("doğru seçilen şık oranında puan, yanlış seçim puanı düşürür, en az 0") ile uyumlu somut hali budur. Puanlar `BigDecimal`'dir; kesirli puan (ör. 10 puanlık soruda 3 doğrudan 1'i için 3,33) tam sayıya yuvarlanıp kaybolmaz.

## Değerlendirilen alternatifler

### Serviste if/switch

Puanlama servisi `switch (quiz.getScoringType())` ile üç hesabı kendi içinde yapar.

- **Artıları:** ek sınıf yok; üç kural için kısa ve doğrudan.
- **Neden seçilmedi:** her yeni kural servisin değiştirilmesini gerektirir (Open/Closed'a aykırı); üç kuralın hesabı ve testi tek sınıfta birikir. Tür ekleyip bir `case`'i unutmak derleyicide değil çalışma anında görülür; resolver'da ise eksik strateji uygulamanın başlamasını engeller.

### Tek sınıf içinde mod bayrağı

Tek bir `ScoringCalculator` sınıfı, `partial` ve `allOrNothing` gibi bayraklarla davranışı değiştirir.

- **Artıları:** tek sınıf, bean ve eşleme yönetimi yok.
- **Neden seçilmedi:** if/switch'in bayrağa taşınmış halidir; bayraklar çoğaldıkça kombinasyonlar (hangi bayrak hangisiyle geçerli) artar ve her kuralı ayrı test etmek zorlaşır. Kural ile `ScoringType` arasındaki eşleme kodda değil bayrak değerlerinde saklanır.

## Sonuçlar

**Olumlu:**

- Yeni kural = `ScoringStrategy` uygulayan yeni bir `@Component` ve `ScoringType`'a yeni bir değer (ile veritabanındaki CHECK kısıtının yeni migration ile genişletilmesi); resolver ve çağıran servis değişmez.
- Her kural kendi sınıfında ve kendi unit testinde; sınır durumları (boş cevap, fazla seçim, hepsi doğru) tek tek yazılı.
- Eksik ya da çift strateji kaydı başlangıçta yakalanır.

**Olumsuz / ödünleşimler:**

- Üç basit kural için dört yeni sınıf ve iki değer tipi var; dolaylılık kod okumayı biraz uzatır.
- Stratejiler tek soruyu puanlar; sınav toplamını, eşiği ve deneme kaydını hesaplayan kod (#54) bu sınıfların dışındadır ve strateji sonuçlarını toplamak ona düşer.
- Kısmi puandaki "yanlış seçim doğruyu götürür" kuralı bir tercihtir; farklı bir ceza isteniyorsa yeni bir strateji olarak eklenir, mevcut olan değiştirilmez.

**Kararın kodda nasıl korunduğu:**

- `ScoringStrategyResolverTest`: her `ScoringType` için doğru strateji; eksik ve çift strateji başlangıçta `IllegalStateException`.
- `ScoringStrategyTest`: her stratejinin başarılı ve sınır durumları.
- Bu PR'da `QuizService` puanlamaya bağlı değildir (cevap çözme #54'tedir); servisin yalnız `ScoringStrategy` arayüzüne bağımlı kalması o issue'da gözden geçirilir.
