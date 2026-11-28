# Rapor birleştirici (#69)

Proje raporunu Word'de birleştirir ve PDF'ini üretir. Rapor metni Markdown dosyalarında yazılır; Word ve PDF her zaman bu betikle baştan üretilir, elle düzenlenmez.

```bash
python3 scripts/rapor/rapor.py                          # varsayılan stil
python3 scripts/rapor/rapor.py --sablon <hocanın şablonu .docx>   # şablonun sayfa düzeni, yazı tipi ve kapak logosuyla (teslim sürümü)
python3 scripts/rapor/rapor.py --pdf-yok                # yalnız .docx (hızlı deneme)
```

Çıktı: `rapor/Mini_Udemy_Proje_Raporu.docx` ve `.pdf`. Bu iki dosya `.gitignore`'dadır: ara sürümler commit'lenmez, herkes kendi makinesinde üretir. Betik sonunda sayfa sayısını, içindekilerdeki başlık sayısını ve **⟨…⟩ yer tutucularının listesini** yazar; bunlar Word'de sarıyla işaretlidir. Teslimden önce liste boş olmalı.

## Girdi ve sıra

Sıra hocanın şablonundaki gibidir (YM 341 örnek raporu): kapak, ön sayfalar (Önsöz → Summary → Özet → İçindekiler → Şekil listesi → Kısaltmalar), bölümler, Referanslar, Ekler.

| Sıra | Kaynak | Not |
|---|---|---|
| Kapak | `rapor/on-sayfalar.md` | `::: {custom-style="Kapak …"}` blokları; "Kapak Uye" bloğunda her üye iki satır (AD SOYAD, numara), betik iki sütuna dizer; `--sablon` ile şablondaki logo eklenir |
| Önsöz, Summary, Özet | `rapor/on-sayfalar.md` | |
| İçindekiler, Şekil listesi | (otomatik) | `on-sayfalar.md`'deki `İÇİNDEKİLER` ve `ŞEKİL LİSTESİ` yorumlarının yeri; içindekiler 3 düzey |
| Kısaltmalar | `rapor/on-sayfalar.md` | |
| Bölüm 1–8 | `rapor/bolum-1-3.md`, `bolum-4.md`, `bolum-5-8.md` | Dosyanın ilk `## <n>.` başlığından öncesi (taslak notları) alınmaz |
| Referanslar | `rapor/kaynaklar.md` | `docs/dependencies.md` ve ADR'lerden |
| Ekler › Ek A. Ekran görüntüleri | `docs/screenshots/*.png` | 3.4.7 tablosundaki bağlantılardan; tablo hücreleri "Şekil A.n" olur |

## Ne yapar

- **Başlıklar:** bölüm dosyalarındaki `##` başlıklar Word'de Başlık 1 olur (her biri yeni sayfada) ve şablondaki gibi büyük harfle yazılır ("1. GİRİŞ"); `###` Başlık 2, `####` Başlık 3, numaraları noktalı ("1.1. Amaç"). Numaralar metindeki gibidir; Word'ün otomatik numaralandırması kullanılmaz. Ön sayfa ve ek başlıkları (`ÖNSÖZ`, `REFERANSLAR` …) Markdown'da büyük harfle yazılıdır.
- **Görseller:** diyagramlar (`docs/diagrams/*.png`) metin genişliğine ve sayfa yüksekliğine sığdırılır ve bölüm bazında numaralanır ("Şekil 4.2. …"). Tablo hücresindeki görsel tablodan sonra şekil olarak çıkar, hücrede numarası kalır. Mermaid blokları (Gantt) Chrome ile PNG'ye çizilir.
- **Ekran görüntüleri:** masaüstü ve mobil görünüm yan yana; uzun sayfaların yalnız ilk ekranı (masaüstü 960, mobil 780 piksel) gösterilir.
- **Bağlantılar:** repodaki dosyalara giden göreli bağlantılar GitHub adresine çevrilir; rapor bölümleri arasındaki bağlantılar düz metin olur.
- **Sayfa numaraları:** şablondaki gibi kapakta yok; ön sayfalarda altta ortada i, ii, iii…; Bölüm 1'den itibaren sağ üstte 1, 2, 3…
- **İçindekiler ve şekil listesi:** içindekiler Word alanıdır (Word'de "Alanı güncelleştir" çalışır), şekil listesi düz metindir (altyazılar Word alanı olmadığı için); ikisinde de sayfa numaraları dolu ve sayfada görünen biçimde (iv, 12…). Betik PDF'i üretir, başlıkların ve şekillerin sayfalarını PDF'ten okur ve numaralar değişmeyene kadar (en çok 4 kez) yeniden yazar.
- **Stil:** `--sablon` verilmezse: A4, 2,5 cm kenar, Times New Roman 12 pt, 1,5 satır aralığı, iki yana yaslı metin, siyah başlıklar (16/14/12 pt), gri çizgili tablolar (9,5 pt). `--sablon` verilirse şablondan okunur: sayfa boyutu ve kenarlar (YM 341 şablonunda Letter; üst/alt 3 cm, sol 3,5 cm, sağ 2 cm), gövde metninin yazı tipi, puntosu, satır aralığı ve paragraf sonrası boşluğu, şekil altyazısı biçimi (kalın 10 pt) ve kapaktaki logo. Başlıklar o zaman şablondaki gibi metin puntosunda ve kalın, tablolar siyah çizgili, içindekiler 1,5 satır aralıklı olur. Şablonun **metni** ve stil dosyası rapora kopyalanmaz.
- **Görseller ve taşma:** görseller metin genişliğinden 0,3 cm dar tutulur (LibreOffice tam genişlikteki görseli kenar boşluğuna kaydırıyor).

## Gerekenler

Python 3.11+ (yalnız standart kütüphane), [pandoc](https://pandoc.org/) 3, LibreOffice (`soffice`, PDF için), poppler (`pdftotext`, `pdfinfo`), `ffmpeg` (ekran görüntüsü kırpma), Node.js 22+ ve Google Chrome (Mermaid için; Mermaid 11.4.1 jsDelivr'dan yüklenir, internet gerekir). Chrome başka bir yoldaysa `CHROME=/yol/chrome`. macOS'ta: `brew install pandoc poppler ffmpeg` ve `brew install --cask libreoffice`.

## 30.11'de yeniden üretmek

1. Bölüm dosyalarındaki ⟨…⟩ yerlerini doldurun (bug bash, demo provası, son sayılar); kapaktaki soyad ve öğrenci numaralarını `on-sayfalar.md`'de tamamlayın.
2. Hocanın şablonuyla (`--sablon`) çalıştırın. Şablon repoda değildir (hocanın dosyası); Bertan'dan alınır.
3. Betiğin yazdığı yer tutucu listesi boşalana kadar 1–2'yi tekrarlayın; çıktıyı PDF'ten bir kez gözle kontrol edin.

## Teslim sürümü (01.12)

Repoya yalnız son sürüm, **tek commit'le** girer. Dosyalar `.gitignore`'da olduğu için `-f` gerekir:

```bash
python3 scripts/rapor/rapor.py --sablon <şablon.docx>
git add -f rapor/Mini_Udemy_Proje_Raporu.docx rapor/Mini_Udemy_Proje_Raporu.pdf
git commit -m "docs(#69): proje raporu — teslim sürümü"
git tag -a teslim -m "Proje raporu teslim sürümü (01.12.2026)"
```

Commit PR ile `main`'e girer; `teslim` etiketi merge commit'ine konur ve `git push origin teslim` ile gönderilir. Teslimden sonra dosya değişirse yeni sürüm yine tek commit'le eklenir, etiket taşınmaz (`teslim-2` gibi yeni etiket).
