// Demo seed içeriği (#67): kurslar, bölümler, dersler ve sınavlar. generate.mjs bu tanımdan kapak görsellerini, PDF ders
// notlarını, video derslerini, DOCX dokümanları ve demo-seed.sql'in kurs yapısı bölümünü üretir.
//
// Kurallar (demo senaryoları, docs/demo.md):
// - "Sıfırdan Python" 12 derstir: Ayşe 12/12 (%100), Can 8/12 (%66,67), Elif 6/12 (%50), Burak 2/12 (%16,67).
// - "Java ile Nesne Yönelimli Programlama" 10 derstir: Selin 5/10 (%50).
// - Soru metinleri ve sınav adları bütün kurslarda tekildir (kullanici-verisi.sql başlıkla eşler).
// Ders türleri: VIDEO (slayt tarzı, generate.mjs üretir), PDF (ders notu), DOCUMENT (DOCX alıştırma).

export const INSTRUCTORS = [
  { email: "mehmet.kaya@miniudemy.dev", name: "Mehmet Kaya", bio: "Python, veri analizi ve veri tabanları eğitmeni." },
  { email: "zeynep.ak@miniudemy.dev", name: "Zeynep Ak", bio: "Java, web geliştirme ve arayüz tasarımı eğitmeni." },
  { email: "ece.yildiz@miniudemy.dev", name: "Ece Yıldız", bio: "İş İngilizcesi ve çevik proje yönetimi eğitmeni." },
];

/** Kapak renkleri: indigo tabanın üstünde kategoriye özgü ton. */
export const CATEGORY_STYLE = {
  "Yazılım": { from: "#6366f1", to: "#1e1b4b", icon: "code" },
  "Veri Bilimi": { from: "#14b8a6", to: "#1e1b4b", icon: "chart" },
  "Tasarım": { from: "#ec4899", to: "#1e1b4b", icon: "pen" },
  "Dil": { from: "#f59e0b", to: "#1e1b4b", icon: "chat" },
  "İş": { from: "#0ea5e9", to: "#1e1b4b", icon: "board" },
};

const V = (title, summary, points) => ({ type: "VIDEO", title, summary, points, mandatory: true });
const P = (title, minutes, summary, points, extra = {}) => ({ type: "PDF", title, minutes, summary, points, mandatory: true, ...extra });
const D = (title, minutes, summary, points, extra = {}) => ({ type: "DOCUMENT", title, minutes, summary, points, mandatory: false, ...extra });
const Q = (text, correct, ...wrong) => ({ text, options: [[correct, true], ...wrong.map((w) => [w, false])] });

export const COURSES = [
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Sıfırdan Python",
    instructor: "mehmet.kaya@miniudemy.dev",
    category: "Yazılım",
    level: "BEGINNER",
    status: "PUBLISHED",
    description:
      "Hiç programlama bilmeyenler için Python: kurulumdan veri tiplerine, koşul ve döngülerden fonksiyonlara. Kurs, küçük bir not ortalaması hesaplayıcısıyla biter.",
    outcomes: [
      "Python kurup ilk programını çalıştırır",
      "Temel veri tiplerini ve listeleri kullanır",
      "Koşul ve döngülerle akış kurar",
      "Fonksiyon yazar ve modül kullanır",
      "Küçük bir komut satırı programı geliştirir",
    ],
    sections: [
      {
        title: "Giriş",
        lessons: [
          V("Python Nedir?", "Python okunabilir sözdizimiyle öğrenmesi kolay, genel amaçlı bir dildir.", [
            "Okunabilir, girintiyle yapılanan sözdizimi",
            "Web, veri analizi ve otomasyonda yaygın",
            "Yorumlanan dil: yaz, çalıştır, sonucu gör",
            "Bu kursta Python 3 kullanıyoruz",
          ]),
          P("Kurulum", 6,
            "Python'u resmî siteden indirip kurarız ve terminalde sürümünü kontrol ederiz. Windows'ta kurulumda \"Add Python to PATH\" kutusunu işaretlemek, komutun her klasörden çalışmasını sağlar. Kod yazmak için VS Code gibi bir editör yeterlidir.",
            ["python.org adresinden Python 3 sürümünü indir", "Kurulumda PATH seçeneğini işaretle", "Terminalde sürümü kontrol et", "Editör olarak VS Code ve Python eklentisini kur"],
            { code: "$ python --version\nPython 3.12.4\n\n$ python\n>>> 2 + 3\n5", exercise: "Python'u kurup terminalde python --version komutunun çıktısını not al." }),
          V("İlk Program", "İlk programımız ekrana bir selam yazar ve kullanıcıdan adını ister.", [
            "print() ile ekrana yazdırma",
            "input() ile kullanıcıdan veri alma",
            "Değişkene değer atama",
            "Dosyayı python merhaba.py ile çalıştırma",
          ]),
        ],
        quiz: {
          title: "Giriş sınavı",
          threshold: 70,
          questions: [
            Q("Python'da ekrana yazı yazdıran fonksiyon hangisidir?", "print()", "echo()", "write()"),
            Q("Python sürümünü terminalde hangi komut gösterir?", "python --version", "python --help"),
            Q("Kullanıcıdan klavyeyle veri almak için hangi fonksiyon kullanılır?", "input()", "read()", "scan()"),
            Q("Python hangi tür bir dildir?", "Yorumlanan, genel amaçlı bir dil", "Yalnız web sayfaları için bir işaretleme dili", "Yalnız veritabanı sorgu dili"),
          ],
        },
      },
      {
        title: "Veri Tipleri",
        lessons: [
          P("Sayılar ve Metinler", 8,
            "Python'da tam sayılar (int), ondalıklı sayılar (float) ve metinler (str) en sık kullanılan veri tipleridir. Bir değerin tipini type() ile görebiliriz. Metinleri birleştirmek için + işareti, sayıyı metne çevirmek için str() kullanılır.",
            ["int: 42, float: 3.14, str: \"merhaba\"", "type() ile tip öğrenilir", "f-string ile metin içine değer yerleştirilir", "int() ve float() ile dönüşüm yapılır"],
            { code: "ad = \"Ayşe\"\nyas = 21\nprint(f\"{ad} {yas} yaşında.\")\nprint(type(yas))   # <class 'int'>", exercise: "İki sayıyı kullanıcıdan alıp toplamlarını yazdıran bir program yaz." }),
          P("Listeler ve Sözlükler", 9,
            "Listeler sıralı değer koleksiyonlarıdır; elemanlara sıra numarasıyla erişilir. Sözlükler anahtar–değer çiftlerini tutar ve bir değere anahtarıyla ulaşılır. İkisi de değiştirilebilir: eleman eklenip çıkarılabilir.",
            ["Liste: [10, 20, 30], ilk eleman liste[0]", "append() ile ekleme, len() ile uzunluk", "Sözlük: {\"ad\": \"Can\", \"not\": 85}", "Sözlükte değere sozluk[\"ad\"] ile erişilir"],
            { code: "notlar = [85, 72, 90]\nnotlar.append(64)\nogrenci = {\"ad\": \"Can\", \"notlar\": notlar}\nprint(ogrenci[\"ad\"], len(notlar))", exercise: "Üç arkadaşının adını ve yaşını tutan bir sözlük listesi oluştur." }),
          P("Koşullar ve Döngüler", 10,
            "if, elif ve else ile program farklı durumlara göre farklı yollar izler. for döngüsü bir koleksiyonun her elemanı için, while döngüsü bir koşul doğru olduğu sürece çalışır. Girinti, hangi satırların bloğa ait olduğunu belirler.",
            ["if / elif / else ile karar verme", "for eleman in liste: ile dolaşma", "range(5) ile 0–4 arası sayılar", "break ile döngüden erken çıkma"],
            { code: "for puan in [45, 70, 92]:\n    if puan >= 85:\n        print(\"Pekiyi\")\n    elif puan >= 50:\n        print(\"Geçti\")\n    else:\n        print(\"Kaldı\")", exercise: "1'den 100'e kadar olan çift sayıların toplamını bulan bir döngü yaz." }),
        ],
      },
      {
        title: "Fonksiyonlar",
        lessons: [
          P("Fonksiyon Tanımlama", 8,
            "Fonksiyon, bir işi yapan ve adıyla çağrılan kod bloğudur. def anahtar kelimesiyle tanımlanır; parametre alabilir ve return ile değer döndürebilir. Tekrar eden kodu fonksiyona almak programı kısaltır ve okunur yapar.",
            ["def ad(parametreler): ile tanımlama", "return ile sonuç döndürme", "Fonksiyon çağrılmadıkça çalışmaz", "İyi bir ad, ne yaptığını anlatır"],
            { code: "def ortalama(notlar):\n    return sum(notlar) / len(notlar)\n\nprint(ortalama([85, 72, 90]))  # 82.33...", exercise: "Bir sayının asal olup olmadığını döndüren asal_mi(n) fonksiyonunu yaz." }),
          P("Parametreler ve Dönüş Değeri", 8,
            "Parametrelere varsayılan değer verilebilir; çağıran o değeri vermezse varsayılan kullanılır. Anahtar kelimeli argümanlar okunabilirliği artırır. Bir fonksiyon birden çok değeri demet (tuple) olarak döndürebilir.",
            ["Varsayılan parametre: def selamla(ad, dil=\"tr\")", "Anahtar kelimeli çağrı: selamla(ad=\"Elif\")", "Birden çok değer: return enk, enb", "None: değer döndürmeyen fonksiyonun sonucu"],
            { code: "def sinirlar(liste):\n    return min(liste), max(liste)\n\nen_kucuk, en_buyuk = sinirlar([4, 9, 1])", exercise: "Bir metindeki ünlü ve ünsüz harf sayısını birlikte döndüren bir fonksiyon yaz." }),
          P("Modüller", 7,
            "Modül, başka dosyalardan içe aktarılabilen Python dosyasıdır. Standart kütüphane math, random ve datetime gibi hazır modüller içerir. Kendi fonksiyonlarını ayrı bir dosyada toplayıp import ile kullanabilirsin.",
            ["import math ile modülü içe aktarma", "from random import choice ile tek bir ad alma", "Kendi modülün: yardimci.py", "pip ile dış paket kurma"],
            { code: "import math\nfrom random import choice\n\nprint(math.sqrt(16))        # 4.0\nprint(choice([\"yazı\", \"tura\"]))", exercise: "random modülüyle 1–6 arası zar atan bir program yaz." }),
        ],
        quiz: {
          title: "Fonksiyonlar sınavı",
          threshold: 60,
          questions: [
            Q("Fonksiyon tanımlamak için hangi anahtar kelime kullanılır?", "def", "function", "func"),
            Q("return ifadesi ne yapar?", "Fonksiyondan değer döndürür", "Programı kapatır"),
            Q("Varsayılan parametre değeri ne zaman kullanılır?", "Çağıran o argümanı vermediğinde", "Her çağrıda, verilen değerin yerine", "Yalnız fonksiyon ilk kez çağrıldığında"),
            Q("math modülündeki karekök fonksiyonunu kullanmak için önce ne yazılır?", "import math", "include math", "using math"),
          ],
        },
      },
      {
        title: "Mini Proje",
        lessons: [
          P("Not Ortalaması Hesaplayıcı", 10,
            "Öğrendiklerimizi birleştiriyoruz: kullanıcıdan ders adlarını ve notlarını alan, ortalamayı hesaplayıp harf notunu yazan bir komut satırı programı. Programı küçük fonksiyonlara bölmek her parçayı ayrı denemeyi kolaylaştırır.",
            ["Notları bir listede topla", "Ortalamayı ayrı bir fonksiyonda hesapla", "Harf notunu koşullarla belirle", "Geçersiz girişte kullanıcıyı uyar"],
            { code: "def harf_notu(ort):\n    if ort >= 85: return \"AA\"\n    if ort >= 70: return \"BB\"\n    if ort >= 50: return \"CC\"\n    return \"FF\"", exercise: "Programı, en yüksek ve en düşük notu da gösterecek şekilde genişlet." }),
          D("Alıştırmalar", 15,
            "Kurs boyunca öğrendiklerini pekiştirmek için çözebileceğin alıştırmalar. Her alıştırmayı ayrı bir dosyada çöz ve çalıştırarak dene.",
            ["Kullanıcının girdiği metni tersten yazdır", "Bir listedeki en sık geçen elemanı bul", "Sözlükle basit bir telefon rehberi yap", "Sıcaklığı Celsius'tan Fahrenheit'a çeviren fonksiyon yaz", "Tahmin oyunu: bilgisayarın tuttuğu sayıyı bul"]),
          P("Sonraki Adımlar", 5,
            "Python temellerini tamamladın. Bundan sonra ilgi alanına göre web geliştirme, veri analizi ya da otomasyon yönünde ilerleyebilirsin. Düzenli küçük projeler yapmak öğrendiklerini kalıcı kılar.",
            ["Veri analizi için Pandas ile Veri Analizine Giriş kursu", "Dosya ve hata yönetimini öğren", "Küçük bir otomasyon projesi seç", "Kodunu Git ile sürümle"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Java ile Nesne Yönelimli Programlama",
    instructor: "zeynep.ak@miniudemy.dev",
    category: "Yazılım",
    level: "INTERMEDIATE",
    status: "PUBLISHED",
    description:
      "Sınıflar, nesneler, kapsülleme, kalıtım ve arayüzlerle Java'da nesne yönelimli tasarım. Her bölüm küçük bir kütüphane uygulamasına yeni bir parça ekler.",
    outcomes: [
      "Sınıf ve nesne tasarlar",
      "Kapsüllemeyle nesnenin durumunu korur",
      "Kalıtım ve arayüz kullanır",
      "Çok biçimliliği gerçek bir örnekte uygular",
    ],
    sections: [
      {
        title: "Sınıflar ve Nesneler",
        lessons: [
          V("Sınıf Nedir?", "Sınıf, nesnelerin şablonudur; nesne bu şablondan üretilen somut örnektir.", [
            "Sınıf: alanlar ve metotlar",
            "Nesne: new ile oluşturulan örnek",
            "Kurucu (constructor) başlangıç durumunu kurar",
            "Örnek: Kitap sınıfı ve iki kitap nesnesi",
          ]),
          P("Alanlar ve Metotlar", 8,
            "Alanlar nesnenin durumunu, metotlar davranışını tanımlar. Bir metot nesnenin alanlarını okuyup değiştirebilir. this anahtar kelimesi, metodun çağrıldığı nesneyi gösterir.",
            ["Alan: private String baslik;", "Metot: public void oduncVer()", "this ile nesnenin kendi alanı", "static: nesneye değil sınıfa ait üye"],
            { code: "public class Kitap {\n    private String baslik;\n    private boolean oduncte;\n\n    public Kitap(String baslik) {\n        this.baslik = baslik;\n    }\n\n    public void oduncVer() {\n        this.oduncte = true;\n    }\n}", exercise: "Uye sınıfı yaz: adı ve ödünç aldığı kitap sayısını tutsun." }),
          P("Kapsülleme", 7,
            "Kapsülleme, nesnenin iç durumunu dışarıdan doğrudan değiştirilemez kılar. Alanlar private yapılır, değişiklik yalnız kurallı metotlarla olur. Böylece nesne hiçbir zaman geçersiz bir duruma düşmez.",
            ["Alanlar private", "Değişiklik iş metotlarıyla: oduncVer(), iadeEt()", "Geçersiz işlemde istisna fırlat", "Getter var, gereksiz setter yok"],
            { code: "public void oduncVer() {\n    if (oduncte) {\n        throw new IllegalStateException(\"Kitap zaten ödünçte.\");\n    }\n    oduncte = true;\n}", exercise: "Uye sınıfına en fazla 3 kitap ödünç alma kuralını ekle." }),
        ],
      },
      {
        title: "Kalıtım ve Arayüzler",
        lessons: [
          V("Kalıtım", "Kalıtım, ortak davranışı üst sınıfta toplayıp alt sınıflarda özelleştirmemizi sağlar.", [
            "extends ile alt sınıf tanımlama",
            "super ile üst sınıfın kurucusunu çağırma",
            "@Override ile metodu yeniden yazma",
            "Örnek: Yayin → Kitap ve Dergi",
          ]),
          P("Soyut Sınıflar", 7,
            "Soyut sınıf doğrudan örneklenemez; alt sınıflara ortak bir iskelet sunar. Soyut metotların gövdesi yoktur, her alt sınıf kendi gövdesini yazar.",
            ["abstract class Yayin", "abstract int oduncSuresi();", "Ortak kod üst sınıfta kalır", "Alt sınıf soyut metodu yazmak zorunda"],
            { code: "public abstract class Yayin {\n    abstract int oduncSuresiGun();\n}\n\npublic class Dergi extends Yayin {\n    int oduncSuresiGun() { return 7; }\n}", exercise: "Yayin'dan türeyen bir EKitap sınıfı yaz; ödünç süresi 30 gün olsun." }),
          P("Arayüzler", 8,
            "Arayüz bir sözleşmedir: sınıfın hangi metotları sağlayacağını söyler, nasıl yapacağını söylemez. Bir sınıf birden fazla arayüzü uygulayabilir. Arayüze göre yazılan kod, uygulamayı değiştirmeden genişletilebilir.",
            ["interface Aranabilir { boolean eslesir(String ifade); }", "implements ile uygulama", "Bir sınıf birden fazla arayüz uygulayabilir", "Kod arayüze bağımlı olur, sınıfa değil"],
            { code: "public interface Aranabilir {\n    boolean eslesir(String ifade);\n}\n\npublic class Kitap implements Aranabilir {\n    public boolean eslesir(String ifade) {\n        return baslik.contains(ifade);\n    }\n}", exercise: "Uye sınıfını da Aranabilir yap; ada göre eşleşsin." }),
        ],
        quiz: {
          title: "Nesne yönelimli tasarım sınavı",
          threshold: 70,
          questions: [
            Q("Java'da bir sınıfın başka bir sınıftan türemesi için hangi anahtar kelime kullanılır?", "extends", "implements", "inherits"),
            Q("Soyut (abstract) bir sınıf için hangisi doğrudur?", "Doğrudan new ile örneklenemez", "Hiç metot içeremez", "Yalnız static alanları olabilir"),
            Q("Kapsüllemenin amacı nedir?", "Nesnenin iç durumunu yalnız kurallı metotlarla değiştirmek", "Bütün alanları public yapmak", "Sınıf sayısını azaltmak"),
            Q("Bir sınıf kaç arayüz uygulayabilir?", "Birden fazla", "En fazla bir", "Hiç; arayüzler yalnız kalıtılır"),
            Q("@Override anotasyonu neyi belirtir?", "Metodun üst sınıftaki ya da arayüzdeki bir metodu yeniden yazdığını", "Metodun statik olduğunu", "Metodun hiç çağrılmadığını"),
          ],
        },
      },
      {
        title: "Çok Biçimlilik ve Uygulama",
        lessons: [
          V("Çok Biçimlilik", "Aynı çağrı, nesnenin gerçek tipine göre farklı davranır.", [
            "Üst tipte değişken, alt tipte nesne",
            "Çağrılan metot çalışma anında seçilir",
            "if/instanceof zincirinin yerine geçer",
            "Örnek: her yayının kendi ödünç süresi",
          ]),
          P("Kütüphane Uygulaması", 12,
            "Bölümde yazdığımız sınıfları küçük bir kütüphane uygulamasında birleştiriyoruz: üye kitap ödünç alır, iade eder, gecikme varsa uyarılır. Her sınıfın tek bir sorumluluğu var.",
            ["Kutuphane sınıfı yayınları ve üyeleri tutar", "Ödünç işlemi kuralları nesnelerde", "Gecikme hesabı yayının türüne göre", "main metodunda örnek senaryo"],
            { code: "Kutuphane k = new Kutuphane();\nUye ayse = k.uyeEkle(\"Ayşe\");\nYayin kitap = k.yayinEkle(new Kitap(\"Simyacı\"));\nk.oduncVer(ayse, kitap);", exercise: "İade tarihinden sonra getirilen her gün için gecikme ücreti hesaplayan metodu ekle." }),
          P("Sık Yapılan Hatalar", 6,
            "Nesne yönelimli tasarımda sık görülen hatalar: her alana getter–setter yazmak, gereğinden derin kalıtım ağaçları, bir sınıfa çok fazla sorumluluk yüklemek. Kalıtım yerine birleşim (composition) çoğu zaman daha esnektir.",
            ["Gereksiz setter kapsüllemeyi bozar", "Derin kalıtım değişikliği zorlaştırır", "Tanrı sınıfı: her şeyi yapan sınıf", "Kalıtım yerine birleşimi düşün"]),
          D("Proje Ödevi", 20,
            "Kütüphane uygulamasını genişletme ödevi. Her maddeyi ayrı bir sınıf ya da metot olarak yaz ve küçük bir main senaryosuyla dene.",
            ["Rezervasyon: ödünçteki kitabı sıraya alma", "Üye türleri: öğrenci ve öğretmen için farklı limitler", "Arama: başlık ve yazara göre", "Raporlama: en çok ödünç alınan 5 kitap"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Pandas ile Veri Analizine Giriş",
    instructor: "mehmet.kaya@miniudemy.dev",
    category: "Veri Bilimi",
    level: "INTERMEDIATE",
    status: "PUBLISHED",
    description:
      "CSV dosyalarını okuyup temizlemekten gruplayıp özetlemeye kadar Pandas ile veri analizinin temel adımları. Örnek veri seti: bir kitabevinin bir yıllık satışları.",
    outcomes: [
      "CSV dosyasını DataFrame olarak okur",
      "Eksik ve hatalı verileri temizler",
      "Satırları süzer ve sıralar",
      "groupby ile özet tablolar çıkarır",
      "Sonuçları grafikle gösterir",
    ],
    sections: [
      {
        title: "Pandas'a Giriş",
        lessons: [
          V("DataFrame Nedir?", "DataFrame, satır ve sütunlardan oluşan, etiketli bir tablodur.", [
            "Satırlar gözlem, sütunlar değişken",
            "Her sütunun bir veri tipi var",
            "Series: tek bir sütun",
            "Excel tablosuna benzer, ama kodla",
          ]),
          P("Veriyi Okumak", 8,
            "read_csv ile bir CSV dosyasını tek satırda DataFrame'e çeviririz. head(), info() ve describe() ile verinin ilk satırlarına, sütun tiplerine ve temel istatistiklerine bakarız. Ayırıcı ve kodlama farklıysa parametreyle belirtilir.",
            ["pd.read_csv(\"satislar.csv\")", "head() ilk 5 satır", "info() sütun tipleri ve boş değerler", "describe() sayısal özet"],
            { code: "import pandas as pd\n\ndf = pd.read_csv(\"satislar.csv\", sep=\";\", encoding=\"utf-8\")\nprint(df.head())\ndf.info()", exercise: "Veri setinde kaç satır ve sütun olduğunu shape ile bul." }),
          P("Sütun Seçme ve Süzme", 8,
            "Bir sütunu df[\"ad\"] ile, birden çok sütunu liste vererek seçeriz. Satırları bir koşulla süzmek için mantıksal maske kullanılır. loc etiketle, iloc sıra numarasıyla erişir.",
            ["df[[\"tarih\", \"tutar\"]]", "df[df[\"tutar\"] > 100]", "Birden çok koşul: & ve |", "df.loc[satir, sutun]"],
            { code: "pahali = df[(df[\"tutar\"] > 100) & (df[\"kategori\"] == \"Roman\")]\nprint(len(pahali))", exercise: "Aralık ayında yapılan satışları süz." }),
        ],
      },
      {
        title: "Veri Temizleme",
        lessons: [
          P("Eksik Veriler", 9,
            "Gerçek veriler eksik değerler içerir. isna() ile eksikleri sayarız; duruma göre satırı atar (dropna) ya da makul bir değerle doldururuz (fillna). Hangi yolun seçildiği analizin sonucunu etkiler, bu yüzden kararı not etmek gerekir.",
            ["df.isna().sum() sütun başına eksik", "dropna() eksik satırı atar", "fillna(0) ya da ortalamayla doldurma", "Kararı analiz notuna yaz"],
            { code: "print(df.isna().sum())\ndf[\"indirim\"] = df[\"indirim\"].fillna(0)\ndf = df.dropna(subset=[\"tutar\"])", exercise: "Eksik müşteri şehirlerini \"Bilinmiyor\" ile doldur." }),
          V("Tipleri Düzeltmek", "Yanlış okunan sütun tipleri hesapları bozar; tarihler ve sayılar doğru tipe çevrilmelidir.", [
            "to_datetime ile tarih sütunu",
            "astype(float) ile sayıya çevirme",
            "Virgüllü ondalıkları düzeltme",
            "Tekrarlayan satırları drop_duplicates ile atma",
          ]),
          P("Yeni Sütunlar", 7,
            "Var olan sütunlardan yeni sütunlar hesaplarız: birim fiyat ile adetten toplam tutar, tarihten ay adı gibi. Hesaplanan sütunlar analizi kolaylaştırır.",
            ["df[\"toplam\"] = df[\"fiyat\"] * df[\"adet\"]", "df[\"ay\"] = df[\"tarih\"].dt.month", "apply ile özel dönüşüm", "Sütun adlarını rename ile düzenleme"],
            { code: "df[\"tarih\"] = pd.to_datetime(df[\"tarih\"])\ndf[\"ay\"] = df[\"tarih\"].dt.month\ndf[\"toplam\"] = df[\"fiyat\"] * df[\"adet\"]", exercise: "Haftanın gününü gösteren bir sütun ekle." }),
        ],
        quiz: {
          title: "Pandas temelleri sınavı",
          threshold: 70,
          questions: [
            Q("Bir CSV dosyasını DataFrame olarak okuyan fonksiyon hangisidir?", "pd.read_csv()", "pd.open_csv()", "pd.load()"),
            Q("Her sütundaki eksik değer sayısını hangi ifade verir?", "df.isna().sum()", "df.count_missing()", "df.empty()"),
            Q("Tutarı 100'den büyük satırları süzen ifade hangisidir?", "df[df[\"tutar\"] > 100]", "df.filter(tutar > 100)", "df.where(\"tutar\", 100)"),
            Q("Metin olarak okunan bir tarih sütununu tarihe çeviren fonksiyon hangisidir?", "pd.to_datetime()", "pd.as_date()", "pd.parse()"),
          ],
        },
      },
      {
        title: "Gruplama ve Görselleştirme",
        lessons: [
          P("groupby ile Özet", 9,
            "groupby veriyi bir ya da birkaç sütuna göre gruplar; ardından her grup için toplam, ortalama ya da sayı hesaplarız. Sonuç yeni ve küçük bir tablodur.",
            ["df.groupby(\"kategori\")[\"toplam\"].sum()", "Birden çok özet: agg([\"sum\", \"mean\"])", "sort_values ile sıralama", "reset_index ile düz tablo"],
            { code: "ozet = (df.groupby(\"kategori\")[\"toplam\"]\n          .agg([\"sum\", \"mean\", \"count\"])\n          .sort_values(\"sum\", ascending=False))", exercise: "Aylara göre toplam satışı hesapla; en iyi ayı bul." }),
          V("Grafik Çizmek", "Özet tabloyu bir grafikle göstermek, sonucu tek bakışta anlatır.", [
            "plot(kind=\"bar\") ile çubuk grafik",
            "Zaman serisi için çizgi grafik",
            "Eksen adları ve başlık şart",
            "Grafiği PNG olarak kaydetme",
          ]),
          D("Analiz Projesi", 25,
            "Kitabevi satış verisiyle kısa bir analiz raporu hazırla. Her soru için kullandığın kodu ve bir cümlelik sonucu yaz.",
            ["En çok satan 5 kategori hangisi?", "Hangi ayda satışlar en yüksek?", "İndirimli satışların payı nedir?", "Şehirlere göre ortalama sepet tutarı", "Bulgularını bir grafikle özetle"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Arayüz Tasarımının Temelleri",
    instructor: "zeynep.ak@miniudemy.dev",
    category: "Tasarım",
    level: "BEGINNER",
    status: "PUBLISHED",
    description:
      "İyi bir arayüzü iyi yapan şeyler: hiyerarşi, boşluk, tipografi, renk ve erişilebilirlik. Kurs boyunca bir kurs platformunun katalog sayfasını adım adım tasarlıyoruz.",
    outcomes: [
      "Görsel hiyerarşi kurar",
      "Tutarlı bir boşluk ölçeği kullanır",
      "Okunaklı bir tipografi seçer",
      "Kontrast oranını hesaplar ve erişilebilir renk seçer",
      "Basit bir tasarım sistemi oluşturur",
    ],
    sections: [
      {
        title: "Temel İlkeler",
        lessons: [
          V("Görsel Hiyerarşi", "Kullanıcının gözü önce en önemli öğeyi görmelidir; hiyerarşi bunu boyut, ağırlık ve boşlukla kurar.", [
            "Boyut ve ağırlıkla önem sırası",
            "Yakın öğeler ilişkili algılanır",
            "Tek bir birincil eylem",
            "Gereksiz süslemeyi çıkar",
          ]),
          P("Boşluk ve Hizalama", 7,
            "Boşluk, öğeleri gruplamanın en ucuz yoludur. 4 ya da 8 piksel tabanlı bir ölçek kullanmak tasarımı tutarlı kılar. Ortak kenarlara hizalanan öğeler düzenli görünür.",
            ["8 px tabanlı ölçek: 8, 16, 24, 32, 48", "İlişkili öğeler arasında az boşluk", "Gruplar arasında daha çok boşluk", "Her şeyi bir ızgaraya hizala"]),
          P("Tipografi", 8,
            "Arayüzde genellikle bir ya da iki yazı tipi yeterlidir. Gövde metni en az 16 piksel olmalı, satır uzunluğu 60–75 karakter arasında tutulmalıdır. Başlık kademeleri bir ölçekle belirlenir.",
            ["Gövde metni en az 16 px", "Satır yüksekliği 1,4–1,6", "Beş kademeli yazı ölçeği", "Vurgu için ağırlık, yeni boyut değil"]),
        ],
      },
      {
        title: "Renk ve Erişilebilirlik",
        lessons: [
          P("Renk Paleti", 7,
            "Bir arayüz için bir vurgu rengi, nötr tonlar ve durum renkleri (başarı, hata, uyarı) yeterlidir. Vurgu rengi tasarruflu kullanıldığında dikkati doğru yere çeker.",
            ["Bir vurgu rengi", "Gri tonlarında bir yüzey ölçeği", "Başarı, hata, uyarı renkleri", "Açık ve karanlık tema için aynı adlar"]),
          V("Kontrast Oranı", "Metin ile zemini arasındaki kontrast, metnin herkes tarafından okunabilmesini sağlar.", [
            "WCAG AA: normal metin en az 4,5:1",
            "Büyük metin için 3:1",
            "Gri üstüne gri en sık hata",
            "Renk tek başına bilgi taşımamalı",
          ]),
          P("Erişilebilir Formlar", 8,
            "Her form alanının görünür bir etiketi olmalı; yalnız yer tutucu metin yetmez. Hata mesajı alanın altında, ne yapılması gerektiğini söyleyen bir cümleyle verilir. Dokunmatik ekranda hedefler en az 44 piksel olmalıdır.",
            ["Her alanın etiketi var", "Hata mesajı alanın altında", "Odak halkası görünür", "Dokunma hedefi en az 44 px"]),
        ],
        quiz: {
          title: "Arayüz ilkeleri sınavı",
          threshold: 70,
          questions: [
            Q("WCAG AA'ya göre normal metin için en düşük kontrast oranı nedir?", "4,5:1", "2:1", "10:1"),
            Q("Dokunmatik ekranda bir düğme için önerilen en küçük yükseklik nedir?", "44 piksel", "16 piksel", "24 piksel"),
            Q("İlişkili öğeleri gruplamanın en basit yolu hangisidir?", "Aralarındaki boşluğu azaltmak", "Hepsini farklı renk yapmak", "Hepsine gölge eklemek"),
            Q("Form alanı için hangisi yeterli değildir?", "Yalnız yer tutucu (placeholder) metin", "Görünür bir etiket", "Alanın altında hata mesajı"),
          ],
        },
      },
      {
        title: "Tasarım Sistemi",
        lessons: [
          P("Tasarım Token'ları", 7,
            "Token'lar renk, boşluk, köşe ve gölge gibi değerlerin adlandırılmış halidir. Bileşenler değer yerine token kullanınca tema değiştirmek tek bir yerde olur.",
            ["--accent, --surface gibi adlar", "Boşluk ve köşe ölçekleri", "Açık ve karanlık temada aynı token", "Kodda sabit renk yok"],
            { code: ":root {\n  --accent: #4f46e5;\n  --surface: #ffffff;\n  --radius-md: 12px;\n}\n.button { background: var(--accent); }", exercise: "Kendi projen için beş renk token'ı tanımla." }),
          V("Bileşenler", "Düğme, form alanı, kart ve tablo gibi bileşenler token'lardan kurulur ve her yerde aynı görünür.", [
            "Her bileşenin durumları: normal, üzerinde, odakta, pasif",
            "Varyantlar: birincil, ikincil, tehlike",
            "Bileşen belgesi: ne zaman kullanılır",
            "Tutarlılık güven verir",
          ]),
          D("Tasarım Ödevi", 20,
            "Bir kurs kataloğu sayfasının masaüstü ve 360 piksel mobil tasarımını hazırla. Her madde için kararını bir cümleyle açıkla.",
            ["Arama ve süzgeçler sayfanın üstünde", "Kurs kartı: kapak, başlık, eğitmen, puan", "Boş durum ve yükleniyor görünümü", "Karanlık tema için renkleri kontrol et"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "İş İngilizcesi: E-posta ve Toplantılar",
    instructor: "ece.yildiz@miniudemy.dev",
    category: "Dil",
    level: "INTERMEDIATE",
    status: "PUBLISHED",
    description:
      "İş hayatında en çok ihtiyaç duyulan İngilizce: açık ve kibar e-postalar yazmak, toplantıya katılmak ve kısa bir sunum yapmak. Her derste hazır kalıplar ve örnek cümleler var.",
    outcomes: [
      "Resmî ve yarı resmî e-posta yazar",
      "Toplantıda söz alır ve fikir belirtir",
      "Kibar rica ve öneri kalıplarını kullanır",
      "Kısa bir sunumu yapılandırır",
    ],
    sections: [
      {
        title: "E-posta Yazmak",
        lessons: [
          V("E-postanın Yapısı", "İyi bir iş e-postası kısa, net ve tek bir amaca odaklıdır.", [
            "Konu satırı: ne istediğini söyle",
            "Selamlama: Dear / Hi",
            "Amaç ilk cümlede",
            "Kapanış: Best regards",
          ]),
          P("Rica ve Teşekkür Kalıpları", 7,
            "İngilizce iş yazışmasında rica doğrudan değil, kibar kalıplarla yapılır. Could you…?, Would it be possible to…? gibi kalıplar isteği yumuşatır. Teşekkür ve takip cümleleri ilişkiyi güçlendirir.",
            ["Could you please send me the report by Friday?", "Would it be possible to reschedule the call?", "Thank you for your quick reply.", "I look forward to hearing from you."]),
          P("Sık Yapılan Hatalar", 6,
            "Türkçeden birebir çeviri, iş e-postalarında en sık hatadır. Fazla uzun cümleler, eksik konu satırı ve aşırı resmî ifadeler okuyucuyu yorar.",
            ["\"I am writing for…\" değil \"I am writing to…\"", "Konu satırını boş bırakma", "Bir paragrafta tek fikir", "Göndermeden önce ekleri kontrol et"]),
        ],
        quiz: {
          title: "İş e-postası sınavı",
          threshold: 70,
          questions: [
            Q("Kibar bir rica için hangisi daha uygundur?", "Could you please send me the report?", "Send me the report.", "You must send the report."),
            Q("Resmî bir e-postanın kapanışında hangisi kullanılır?", "Best regards", "See ya", "Bye bye"),
            Q("\"Yanıtınızı bekliyorum\" anlamındaki kalıp hangisidir?", "I look forward to hearing from you.", "I wait your answer.", "Answer me soon."),
            Q("E-postanın amacı nerede yazılmalıdır?", "İlk paragrafta, mümkünse ilk cümlede", "Son paragrafta", "Yalnız konu satırında"),
          ],
        },
      },
      {
        title: "Toplantılar",
        lessons: [
          P("Toplantıyı Açmak", 6,
            "Toplantıyı yöneten kişi amacı ve gündemi kısaca söyler, katılımcıları tanıtır ve süreyi hatırlatır. Bu cümleler toplantıyı düzenli başlatır.",
            ["Let's get started.", "The purpose of today's meeting is to…", "We have three items on the agenda.", "We need to finish by three o'clock."]),
          V("Fikir Bildirmek", "Toplantıda fikir belirtirken kesinlik derecesini kelimelerle ayarlarız.", [
            "I think / In my opinion…",
            "Katılma: I agree with Ali on this.",
            "Kibar itiraz: I see your point, but…",
            "Öneri: How about…? / Why don't we…?",
          ]),
          P("Toplantıyı Kapatmak", 6,
            "Kapanışta kararlar ve kimin neyi ne zamana kadar yapacağı özetlenir. Toplantı sonrası kısa bir özet e-postası bu kararları kayda geçirir.",
            ["To sum up, we agreed to…", "Ayşe will prepare the budget by Monday.", "Let's schedule a follow-up meeting.", "Thank you all for your time."]),
        ],
      },
      {
        title: "Kısa Sunum",
        lessons: [
          P("Sunumun Yapısı", 7,
            "Kısa bir iş sunumu üç bölümden oluşur: giriş, ana noktalar ve kapanış. Her geçişte dinleyiciye nerede olduğunu söylemek takibi kolaylaştırır.",
            ["Today I'm going to talk about…", "First… / Next… / Finally…", "Let's move on to…", "That brings me to the end of my presentation."]),
          P("Soruları Yanıtlamak", 6,
            "Soruyu dinledikten sonra teşekkür edip kısaca yanıtlarız. Cevabı bilmiyorsak dürüstçe söyler ve sonra döneceğimizi belirtiriz.",
            ["That's a good question.", "Let me make sure I understand…", "I'm not sure, but I'll find out and get back to you.", "Does that answer your question?"]),
          D("Pratik Çalışma Kâğıdı", 15,
            "Ders boyunca öğrendiğin kalıpları kullanarak aşağıdaki görevleri yaz. Mümkünse bir arkadaşınla karşılıklı oku.",
            ["Toplantı ertelemek için bir e-posta yaz", "Bir projede gecikmeyi bildiren kısa bir mesaj yaz", "Üç maddelik bir toplantı gündemi hazırla", "İki dakikalık bir proje sunumunun açılışını yaz"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Scrum ile Çevik Proje Yönetimi",
    instructor: "ece.yildiz@miniudemy.dev",
    category: "İş",
    level: "BEGINNER",
    status: "PUBLISHED",
    description:
      "Scrum'ın rolleri, etkinlikleri ve çıktıları; Kanban panosuyla işi görünür kılmak. Örnek bir ekip üzerinden bir sprinti baştan sona planlıyoruz.",
    outcomes: [
      "Scrum rollerini ve sorumluluklarını açıklar",
      "Ürün iş listesini önceliklendirir",
      "Sprint planlaması yapar",
      "Kanban panosu kurar",
      "Retrospektif yönetir",
    ],
    sections: [
      {
        title: "Çevik Yaklaşım",
        lessons: [
          V("Çevik Nedir?", "Çevik yaklaşım, işi kısa döngülerde teslim edip geri bildirimle yön değiştirmeye dayanır.", [
            "Kısa döngüler, sık teslim",
            "Plan değil, öğrenme odaklı",
            "Müşteriyle sürekli iş birliği",
            "Çevik Manifesto'nun dört değeri",
          ]),
          P("Şelale ile Karşılaştırma", 6,
            "Şelale modelinde aşamalar sırayla ve bir kez yapılır; değişiklik pahalıdır. Çevik yaklaşımda her döngüde çalışan bir parça teslim edilir ve plan yeniden gözden geçirilir.",
            ["Şelale: önce tüm plan, sonra uygulama", "Çevik: küçük parçalar, sık geri bildirim", "Belirsiz işlerde çevik daha uygun", "Sabit kapsamlı işlerde şelale işe yarayabilir"]),
          P("Scrum Rolleri", 7,
            "Scrum'da üç sorumluluk vardır: ürün sahibi neyin yapılacağına ve önceliğe karar verir, Scrum Master süreci kolaylaştırır, geliştiriciler işi yapar ve nasıl yapılacağına karar verir.",
            ["Ürün sahibi: değer ve öncelik", "Scrum Master: süreç ve engeller", "Geliştiriciler: iş ve kalite", "Ekip kendi kendini yönetir"]),
        ],
      },
      {
        title: "Sprint",
        lessons: [
          P("Ürün İş Listesi", 7,
            "Ürün iş listesi, üründe yapılacak her şeyin öncelik sırasına dizildiği yaşayan bir listedir. Maddeler kullanıcı hikâyesi olarak yazılır ve kabul kriterleri taşır.",
            ["Kullanıcı hikâyesi: Bir … olarak … istiyorum, böylece …", "En değerli iş en üstte", "Kabul kriterleri ölçülebilir", "Liste sürekli güncellenir"]),
          V("Sprint Planlama", "Sprint planlamasında ekip, sprint hedefini ve bu sprintte bitireceği işleri seçer.", [
            "Sprint hedefi tek cümle",
            "Ekip kapasitesine göre iş seçimi",
            "Büyük işleri küçük görevlere böl",
            "Planlama sprint süresinin bir parçası",
          ]),
          P("Günlük Scrum ve İnceleme", 6,
            "Günlük Scrum 15 dakikalık bir eşgüdüm toplantısıdır; amaç sprint hedefine göre planı ayarlamaktır. Sprint sonunda inceleme toplantısında çalışan ürün paydaşlara gösterilir.",
            ["Günlük Scrum: 15 dakika, aynı saatte", "Engeller Scrum Master'a iletilir", "Sprint incelemesi: çalışan ürünün gösterimi", "Geri bildirim iş listesine girer"]),
        ],
        quiz: {
          title: "Scrum temelleri sınavı",
          threshold: 70,
          questions: [
            Q("Ürün iş listesindeki önceliklere kim karar verir?", "Ürün sahibi", "Scrum Master", "Şirket yöneticisi"),
            Q("Günlük Scrum toplantısı en fazla ne kadar sürer?", "15 dakika", "1 saat", "Yarım gün"),
            Q("Sprint sonunda çalışan ürünün paydaşlara gösterildiği etkinlik hangisidir?", "Sprint incelemesi", "Sprint planlama", "Günlük Scrum"),
            Q("Scrum Master'ın temel görevi nedir?", "Süreci kolaylaştırmak ve engelleri kaldırmak", "Görevleri geliştiricilere dağıtmak", "Ürünün özelliklerine karar vermek"),
            Q("Kullanıcı hikâyesinin hangi parçası işin ne zaman bittiğini söyler?", "Kabul kriterleri", "Hikâye puanı", "Başlık"),
          ],
        },
      },
      {
        title: "Kanban ve İyileştirme",
        lessons: [
          P("Kanban Panosu", 6,
            "Kanban panosu işi sütunlarla görünür kılar: Yapılacak, Yapılıyor, Bitti. Devam eden iş sınırı, aynı anda çok fazla işe başlamayı engeller.",
            ["Sütunlar işin aşamaları", "Her kart bir iş", "Devam eden iş (WIP) sınırı", "Darboğaz panoda görünür"]),
          P("Retrospektif", 6,
            "Retrospektifte ekip, sprint boyunca neyin iyi gittiğini ve neyin iyileştirilebileceğini konuşur. Toplantı somut bir ya da iki iyileştirme eylemiyle biter.",
            ["Ne iyi gitti?", "Ne zorladı?", "Bir sonraki sprintte neyi deneyeceğiz?", "Eylemin sahibi ve tarihi belli"]),
          D("Sprint Planlama Şablonu", 15,
            "Kendi ekibin ya da bir okul projesi için bu şablonu doldurarak bir sprint planla.",
            ["Sprint hedefi (tek cümle)", "Seçilen kullanıcı hikâyeleri ve kabul kriterleri", "Görevler ve sorumlular", "Riskler ve bağımlılıklar", "Sprint incelemesinde neyi göstereceksiniz"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Git ile Sürüm Kontrolü",
    instructor: "zeynep.ak@miniudemy.dev",
    category: "Yazılım",
    level: "BEGINNER",
    status: "PUBLISHED",
    description:
      "Kodunu kaybetmeden değiştirmek ve ekiple birlikte çalışmak için Git: commit, branch, merge, uzak depo ve pull request akışı.",
    outcomes: [
      "Bir depo oluşturup commit atar",
      "Branch açıp birleştirir",
      "Çakışmaları çözer",
      "Uzak depoyla çalışır ve pull request açar",
    ],
    sections: [
      {
        title: "Temeller",
        lessons: [
          V("Git Neden Gerekli?", "Git, kodun her halini saklar; hata yaptığında geri dönmeni ve ekiple birlikte çalışmanı sağlar.", [
            "Her değişikliğin geçmişi",
            "Eski bir hale geri dönme",
            "Aynı anda birden çok kişi",
            "Dağıtık: her kopya tam bir depo",
          ]),
          P("İlk Commit", 7,
            "git init ile bir klasör depoya dönüşür. Değişiklikler önce hazırlık alanına (git add) alınır, sonra açıklayıcı bir mesajla kaydedilir (git commit). git status ve git log durumu ve geçmişi gösterir.",
            ["git init", "git add dosya.txt", "git commit -m \"Açıklayıcı mesaj\"", "git status ve git log"],
            { code: "$ git init\n$ git add README.md\n$ git commit -m \"Proje iskeleti\"\n$ git log --oneline\n3f2a1c4 Proje iskeleti", exercise: "Bir klasörü depo yapıp üç ayrı commit at; git log ile geçmişi incele." }),
          P("İyi Commit Mesajı", 5,
            "İyi bir commit mesajı neyin ve neden değiştiğini söyler. Kısa bir başlık satırı, gerekiyorsa boş bir satırdan sonra açıklama yazılır. Her commit tek bir mantıksal değişiklik içermelidir.",
            ["Başlık 50 karakter civarı", "Emir kipi: \"Giriş formunu ekle\"", "Neden değiştiğini açıkla", "Bir commit, bir değişiklik"]),
        ],
      },
      {
        title: "Branch ve Merge",
        lessons: [
          P("Branch Açmak", 6,
            "Branch, ana koddan ayrılan bağımsız bir çalışma hattıdır. Yeni bir özellik ya da düzeltme için branch açmak ana kodu bozmadan denemeyi sağlar.",
            ["git switch -c ozellik/giris", "git branch ile branch listesi", "Her iş için ayrı branch", "Branch adı işi anlatsın"]),
          V("Merge ve Çakışma", "İki branch aynı satırı farklı değiştirdiğinde Git çakışmayı bize bırakır.", [
            "git merge ile birleştirme",
            "Çakışma işaretleri: <<<<<<< ve >>>>>>>",
            "İki tarafı okuyup doğru hali yaz",
            "Çözümden sonra git add ve commit",
          ]),
          P("Geri Almak", 6,
            "Henüz commit atılmamış değişikliği git restore ile geri alırız. Paylaşılmış bir commit'i geri almak için geçmişi silen komutlar yerine git revert kullanılır.",
            ["git restore dosya: yerel değişikliği at", "git revert <commit>: değişikliği tersine çevir", "Paylaşılan geçmişi yeniden yazma", "Emin değilsen önce yeni bir branch aç"]),
        ],
        quiz: {
          title: "Git temelleri sınavı",
          threshold: 70,
          questions: [
            Q("Bir değişikliği hazırlık alanına alan komut hangisidir?", "git add", "git push", "git init"),
            Q("Yeni bir branch açıp ona geçen komut hangisidir?", "git switch -c yeni-branch", "git merge yeni-branch", "git log yeni-branch"),
            Q("Paylaşılmış bir commit'i geçmişi silmeden geri almanın yolu nedir?", "git revert", "git reset --hard", "Dosyayı silmek"),
            Q("Çakışma çözüldükten sonra ne yapılır?", "Dosya git add ile hazırlanır ve commit atılır", "Depo silinip yeniden klonlanır", "Hiçbir şey; Git kendisi çözer"),
          ],
        },
      },
      {
        title: "Ekiple Çalışmak",
        lessons: [
          P("Uzak Depo", 6,
            "Uzak depo, ekibin ortak kopyasıdır. git clone ile indirilir, git push ile kendi commit'lerimizi gönderir, git pull ile başkalarının değişikliklerini alırız.",
            ["git clone <adres>", "git push origin branch-adi", "git pull ile güncelleme", "Göndermeden önce çek"]),
          P("Pull Request Akışı", 7,
            "Pull request, bir branch'teki değişikliğin ana koda alınması için açılan inceleme isteğidir. Ekip kodu inceler, otomatik testler çalışır ve onaydan sonra birleştirilir.",
            ["Branch'i uzak depoya gönder", "Açıklamada ne ve neden", "İnceleme yorumlarını yanıtla", "Testler yeşilse birleştir"]),
          D("Git Alıştırmaları", 15,
            "Aşağıdaki alıştırmaları kendi bilgisayarında bir deneme deposunda yap.",
            ["İki branch'te aynı satırı değiştirip çakışma oluştur ve çöz", "Bir commit'i git revert ile geri al", "Bir arkadaşınla aynı uzak depoya commit gönder", "Bir pull request açıp açıklamasını yaz"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "SQL ve Veritabanları",
    instructor: "mehmet.kaya@miniudemy.dev",
    category: "Veri Bilimi",
    level: "BEGINNER",
    status: "IN_REVIEW",
    description:
      "SELECT'ten JOIN'e ilişkisel veritabanlarıyla çalışma: tablo tasarımı, sorgular, gruplama ve tabloları birleştirme.",
    outcomes: [
      "SELECT ile veri sorgular",
      "WHERE ve ORDER BY ile süzer ve sıralar",
      "GROUP BY ile özet çıkarır",
      "JOIN ile tabloları birleştirir",
    ],
    sections: [
      {
        title: "İlk Sorgular",
        lessons: [
          V("İlişkisel Veritabanı", "İlişkisel veritabanı veriyi tablolarda tutar; tablolar ortak anahtarlarla birbirine bağlanır.", [
            "Tablo, satır ve sütun",
            "Birincil anahtar her satırı tekil kılar",
            "Yabancı anahtar tabloları bağlar",
            "SQL: veriyle konuşmanın dili",
          ]),
          P("SELECT", 7,
            "SELECT ile bir tablodan istediğimiz sütunları alırız. WHERE koşulu satırları süzer, ORDER BY sonucu sıralar, LIMIT dönen satır sayısını sınırlar.",
            ["SELECT ad, fiyat FROM kitaplar", "WHERE fiyat > 100", "ORDER BY fiyat DESC", "LIMIT 10"],
            { code: "SELECT baslik, fiyat\nFROM kitaplar\nWHERE kategori = 'Roman'\nORDER BY fiyat DESC\nLIMIT 5;", exercise: "Stokta 10'dan az kalan kitapları listeleyen sorguyu yaz." }),
          P("Veri Eklemek ve Güncellemek", 6,
            "INSERT yeni satır ekler, UPDATE var olanı değiştirir, DELETE siler. UPDATE ve DELETE'te WHERE koşulunu unutmak bütün tabloyu etkiler.",
            ["INSERT INTO kitaplar (baslik, fiyat) VALUES (…)", "UPDATE kitaplar SET fiyat = 120 WHERE id = 3", "DELETE FROM kitaplar WHERE id = 3", "WHERE'siz UPDATE tehlikeli"]),
        ],
      },
      {
        title: "Gruplama",
        lessons: [
          P("Toplama Fonksiyonları", 6,
            "COUNT, SUM, AVG, MIN ve MAX birden çok satırdan tek bir değer üretir. NULL değerler çoğu toplama fonksiyonunda dikkate alınmaz.",
            ["COUNT(*) satır sayısı", "SUM(tutar) toplam", "AVG(fiyat) ortalama", "MIN ve MAX"]),
          V("GROUP BY", "GROUP BY satırları gruplar; her grup için toplama fonksiyonu ayrı hesaplanır.", [
            "GROUP BY kategori",
            "HAVING ile grupları süzme",
            "SELECT'teki her sütun ya grupta ya fonksiyonda",
            "Sonuç: kategori başına özet",
          ]),
          P("HAVING", 5,
            "WHERE satırları gruplamadan önce, HAVING grupları gruplamadan sonra süzer. Toplama fonksiyonu üzerinde koşul yalnız HAVING ile yazılabilir.",
            ["WHERE: satır düzeyi", "HAVING: grup düzeyi", "HAVING COUNT(*) > 5", "İkisi aynı sorguda kullanılabilir"]),
        ],
        quiz: {
          title: "SQL temelleri sınavı",
          threshold: 70,
          questions: [
            Q("Bir tablodan veri okumak için hangi komut kullanılır?", "SELECT", "INSERT", "CREATE"),
            Q("Grupların toplama fonksiyonuna göre süzülmesi için hangisi kullanılır?", "HAVING", "WHERE", "ORDER BY"),
            Q("İki tabloyu ortak bir sütun üzerinden birleştiren ifade hangisidir?", "JOIN", "UNION", "MERGE INTO"),
            Q("WHERE koşulu yazılmayan bir UPDATE ne yapar?", "Tablodaki bütün satırları günceller", "Hiçbir satırı güncellemez", "Hata verir"),
          ],
        },
      },
      {
        title: "Birleştirmeler",
        lessons: [
          P("INNER JOIN", 7,
            "INNER JOIN iki tablodan yalnız eşleşen satırları getirir. Eşleşme koşulu ON ile, genellikle yabancı anahtar üzerinden yazılır.",
            ["FROM siparisler s JOIN musteriler m ON s.musteri_id = m.id", "Takma adlar okunabilirliği artırır", "Eşleşmeyen satırlar sonuçta yok", "Birden çok JOIN zincirlenebilir"],
            { code: "SELECT m.ad, COUNT(*) AS siparis_sayisi\nFROM siparisler s\nJOIN musteriler m ON m.id = s.musteri_id\nGROUP BY m.ad;", exercise: "Her kategorideki kitapların toplam satış tutarını bulan sorguyu yaz." }),
          P("LEFT JOIN", 6,
            "LEFT JOIN soldaki tablonun bütün satırlarını getirir; sağda eşleşme yoksa sağ tablonun sütunları NULL olur. Hiç siparişi olmayan müşterileri bulmak için kullanılır.",
            ["Soldaki her satır sonuçta", "Eşleşme yoksa NULL", "WHERE s.id IS NULL ile eşleşmeyenler", "INNER ile farkını dene"]),
          D("SQL Alıştırmaları", 15,
            "Kitabevi veritabanı üzerinde çözülecek sorgular.",
            ["En pahalı 3 kitap", "Her yazarın kitap sayısı", "Hiç satılmamış kitaplar", "Aylara göre toplam satış", "En çok harcama yapan 5 müşteri"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Web Geliştirmeye Giriş",
    instructor: "zeynep.ak@miniudemy.dev",
    category: "Yazılım",
    level: "BEGINNER",
    status: "DRAFT",
    rejectionReason: "Bölümlerde örnek proje eksik. Her bölüme kısa bir uygulama ekleyin.",
    description: "HTML, CSS ve biraz JavaScript ile ilk web sayfanı yap.",
    outcomes: ["HTML sayfası hazırlar", "CSS ile biçimlendirir", "Basit etkileşim ekler", "Sayfayı mobil uyumlu yapar"],
    sections: [
      {
        title: "HTML Temelleri",
        lessons: [
          V("Etiketler", "HTML, sayfanın içeriğini etiketlerle yapılandırır.", [
            "Başlık etiketleri: h1–h6",
            "Paragraf ve bağlantı",
            "Liste ve görsel",
            "Anlamlı etiketler: header, main, footer",
          ]),
          P("Sayfa İskeleti", 6,
            "Her HTML belgesi aynı iskeletle başlar: doctype, html, head ve body. head sayfanın başlığını ve karakter kodlamasını, body görünen içeriği taşır.",
            ["<!doctype html>", "<meta charset=\"utf-8\">", "<title> sekmede görünen ad", "Görünen içerik <body> içinde"],
            { code: "<!doctype html>\n<html lang=\"tr\">\n  <head>\n    <meta charset=\"utf-8\">\n    <title>İlk Sayfam</title>\n  </head>\n  <body>\n    <h1>Merhaba</h1>\n  </body>\n</html>" }),
          P("Formlar", 6,
            "Formlar kullanıcıdan veri almanın yoludur. Her alanın bir etiketi olmalı ve alan türü verinin türüne uygun seçilmelidir.",
            ["<form> ve <input>", "<label for> ile etiket", "type=\"email\", type=\"number\"", "required ile zorunlu alan"]),
        ],
        quiz: {
          title: "HTML temelleri sınavı",
          threshold: 70,
          questions: [
            Q("Sayfanın en önemli başlığı için hangi etiket kullanılır?", "<h1>", "<head>", "<title>"),
            Q("Görünen içerik hangi etiketin içinde yer alır?", "<body>", "<head>", "<meta>"),
            Q("Bir form alanını etiketiyle ilişkilendirmek için ne kullanılır?", "<label for=\"…\">", "<span>", "<div class=\"label\">"),
            Q("Türkçe karakterlerin doğru görünmesi için hangi satır gerekir?", "<meta charset=\"utf-8\">", "<html lang=\"en\">", "<link rel=\"font\">"),
          ],
        },
      },
      {
        title: "CSS ile Biçimlendirme",
        lessons: [
          P("Seçiciler", 6,
            "CSS kuralları seçicilerle öğeleri bulur ve onlara stil verir. Sınıf seçicisi en sık kullanılan seçicidir.",
            ["Etiket: p { }", "Sınıf: .kart { }", "Kimlik: #ust { }", "Özgüllük: daha özel kural kazanır"]),
          V("Kutu Modeli", "Her öğe bir kutudur: içerik, iç boşluk, kenarlık ve dış boşluk.", [
            "padding: iç boşluk",
            "border: kenarlık",
            "margin: dış boşluk",
            "box-sizing: border-box",
          ]),
          P("Flexbox", 7,
            "Flexbox öğeleri bir satır ya da sütun boyunca hizalar ve aralarındaki boşluğu dağıtır.",
            ["display: flex", "justify-content ile yatay hizalama", "align-items ile dikey hizalama", "gap ile aralık"]),
        ],
      },
      {
        title: "Biraz JavaScript",
        lessons: [
          P("Değişkenler ve Fonksiyonlar", 6,
            "JavaScript sayfaya davranış ekler. const ve let ile değişken tanımlanır, function ile fonksiyon yazılır.",
            ["const sabit, let değişken", "function selamla(ad) { }", "console.log ile hata ayıklama", "Tarayıcının geliştirici araçları"]),
          P("Olaylar", 6,
            "Kullanıcı bir düğmeye tıkladığında ya da form gönderdiğinde olay oluşur; addEventListener ile bu olaya tepki veririz.",
            ["document.querySelector", "addEventListener(\"click\", …)", "Formun gönderilmesini engelleme", "Sayfadaki metni değiştirme"]),
          D("Mini Proje", 20,
            "Kişisel bir tanıtım sayfası hazırla.",
            ["Başlık, kısa tanıtım ve fotoğraf yerine bir avatar", "Projelerini listeleyen bir bölüm", "İletişim formu", "Mobilde tek sütun"]),
        ],
      },
    ],
  },
  // ---------------------------------------------------------------------------------------------------------------
  {
    title: "Python 2 ile Programlama (eski)",
    instructor: "mehmet.kaya@miniudemy.dev",
    category: "Yazılım",
    level: "BEGINNER",
    status: "UNPUBLISHED",
    unpublishReason: "Python 2 artık desteklenmiyor. Yerine Sıfırdan Python kursunu önerin.",
    noCover: true,
    description: "Python 2 için hazırlanmış eski kurs.",
    outcomes: ["Python 2 sözdizimini tanır", "print ifadesini kullanır", "Python 3'e geçişteki farkları bilir", "Basit betikler yazar"],
    sections: [
      {
        title: "Giriş",
        lessons: [
          V("print ifadesi", "Python 2'de print bir fonksiyon değil, bir ifadedir.", [
            "print \"merhaba\" parantezsiz",
            "Python 3'te print() fonksiyon",
            "Eski kodlarda sık görülür",
            "2020'den beri destek yok",
          ]),
          P("Kurulum (Python 2.7)", 5,
            "Python 2.7 son Python 2 sürümüdür ve artık güvenlik güncellemesi almaz. Bu ders yalnız eski projeleri çalıştırmak isteyenler içindir.",
            ["Python 2.7 son sürüm", "Güvenlik güncellemesi yok", "Yeni projelerde Python 3", "Eski kod için ayrı ortam"]),
          P("raw_input", 5,
            "Python 2'de klavyeden metin almak için raw_input kullanılır; Python 3'te bu fonksiyonun adı input'tur.",
            ["raw_input(\"Adın: \")", "Python 2'deki input değeri çalıştırır", "Python 3'te yalnız input()", "Taşırken adı değiştir"]),
        ],
        quiz: {
          title: "Python 2 farkları sınavı",
          threshold: 70,
          questions: [
            Q("Python 2'de print nasıl kullanılır?", "Parantezsiz bir ifade olarak", "Yalnız print() fonksiyonu olarak", "Python 2'de print yoktur"),
            Q("Python 2'de klavyeden metin almanın güvenli yolu hangisidir?", "raw_input()", "scanf()", "readline()"),
            Q("Python 2'nin desteği hangi yıl sona erdi?", "2020", "2010", "2025"),
            Q("Python 2'de 7 / 2 ifadesinin sonucu nedir?", "3", "3.5", "4"),
          ],
        },
      },
      {
        title: "Farklar",
        lessons: [
          P("Tam Sayı Bölmesi", 5,
            "Python 2'de iki tam sayının bölümü yine tam sayıdır; 7 / 2 sonucu 3 olur. Python 3'te sonuç 3.5'tir ve tam sayı bölmesi için // kullanılır.",
            ["Python 2: 7 / 2 == 3", "Python 3: 7 / 2 == 3.5", "Tam sayı bölmesi: //", "from __future__ import division"]),
          P("Unicode", 5,
            "Python 2'de metinler varsayılan olarak bayt dizisidir; Türkçe karakterler için u\"…\" önekli unicode metin gerekir. Python 3'te her metin unicode'dur.",
            ["str bayt, unicode metin", "u\"şeker\" öneki", "Python 3'te str unicode", "Kodlama hataları sık görülür"]),
          P("2to3 Aracı", 5,
            "2to3 aracı Python 2 kodunu otomatik olarak Python 3'e çevirmeye yardım eder; sonucu yine de testlerle kontrol etmek gerekir.",
            ["2to3 -w dosya.py", "print ve raw_input dönüşür", "Bölme davranışını elle kontrol et", "Önce testleri çalıştır"]),
        ],
      },
      {
        title: "Kapanış",
        lessons: [
          V("Neden Python 3?", "Yeni projelerde Python 3 kullanılmalıdır; kütüphanelerin çoğu artık yalnız Python 3'ü destekler.", [
            "Python 2 desteği bitti",
            "Kütüphaneler Python 3'te",
            "Daha tutarlı metin ve bölme",
            "Sıfırdan Python kursuna geç",
          ]),
          P("Taşıma Kontrol Listesi", 5,
            "Eski bir projeyi Python 3'e taşırken izlenecek adımlar.",
            ["Testleri Python 2'de yeşil yap", "2to3 ile otomatik dönüşüm", "Metin ve bayt ayrımını düzelt", "Testleri Python 3'te çalıştır"]),
          D("Taşıma Alıştırması", 15,
            "Verilen küçük Python 2 betiğini Python 3'e taşı.",
            ["print ifadelerini fonksiyona çevir", "raw_input'u input yap", "Bölmeleri kontrol et", "Türkçe metinleri dene"]),
        ],
      },
    ],
  },
];
