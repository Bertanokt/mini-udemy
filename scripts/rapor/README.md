# Rapor birleştirici (#69)

Proje raporunu Word'de birleştirir ve PDF'ini üretir. Rapor metni Markdown dosyalarında yazılır; Word ve PDF her zaman bu betikle baştan üretilir, elle düzenlenmez.

```bash
python3 scripts/rapor/rapor.py                          # varsayılan stil
python3 scripts/rapor/rapor.py --sablon ~/Downloads/Proje_Yazim_Sablonu.docx   # hocanın şablonunun stilleriyle
python3 scripts/rapor/rapor.py --pdf-yok                # yalnız .docx (hızlı deneme)
```

Çıktı: `rapor/Mini_Udemy_Proje_Raporu.docx` ve `.pdf`. Bu iki dosya `.gitignore`'dadır: ara sürümler commit'lenmez, herkes kendi makinesinde üretir. Betik sonunda sayfa sayısını, içindekilerdeki başlık sayısını ve **⟨…⟩ yer tutucularının listesini** yazar; bunlar Word'de sarıyla işaretlidir. Teslimden önce liste boş olmalı.

## Girdi ve sıra

| Sıra | Kaynak | Not |
|---|---|---|
| Kapak, Önsöz | `rapor/on-sayfalar.md` | Kapak `::: {custom-style="Kapak …"}` bloklarıdır |
| İçindekiler | (otomatik) | `on-sayfalar.md`'deki `İÇİNDEKİLER` yorumunun yeri; 3 düzey |
| Kısaltmalar, Özet, Summary | `rapor/on-sayfalar.md` | |
| Bölüm 1–8 | `rapor/bolum-1-3.md`, `bolum-4.md`, `bolum-5-8.md` | Dosyanın ilk `## <n>.` başlığından öncesi (taslak notları) alınmaz |
| Kaynaklar | `rapor/kaynaklar.md` | `docs/dependencies.md` ve ADR'lerden |
| Ek A. Ekran görüntüleri | `docs/screenshots/*.png` | 3.4.7 tablosundaki bağlantılardan; tablo hücreleri "Şekil A.n" olur |

## Ne yapar

- **Başlıklar:** bölüm dosyalarındaki `##` başlıklar Word'de Başlık 1 olur (her biri yeni sayfada), `###` Başlık 2, `####` Başlık 3. Numaralar metindeki gibidir; Word'ün otomatik numaralandırması kullanılmaz.
- **Görseller:** diyagramlar (`docs/diagrams/*.png`) metin genişliğine ve sayfa yüksekliğine sığdırılır ve bölüm bazında numaralanır ("Şekil 4.2. …"). Tablo hücresindeki görsel tablodan sonra şekil olarak çıkar, hücrede numarası kalır. Mermaid blokları (Gantt) Chrome ile PNG'ye çizilir.
- **Ekran görüntüleri:** masaüstü ve mobil görünüm yan yana; uzun sayfaların yalnız ilk ekranı (masaüstü 960, mobil 780 piksel) gösterilir.
- **Bağlantılar:** repodaki dosyalara giden göreli bağlantılar GitHub adresine çevrilir; rapor bölümleri arasındaki bağlantılar düz metin olur.
- **İçindekiler:** Word alanı olarak (Word'de "Alanı güncelleştir" çalışır), sayfa numaraları dolu. Betik PDF'i üretir, başlıkların sayfalarını PDF'ten okur ve numaralar değişmeyene kadar (en çok 3 kez) yeniden yazar.
- **Stil:** `--sablon` verilmezse pandoc'un varsayılan şablonu şu düzene çekilir: A4, 2,5 cm kenar, Times New Roman 12 pt, 1,5 satır aralığı, iki yana yaslı metin, siyah başlıklar, çizgili tablolar (9,5 pt), sayfa altında ortada sayfa numarası (kapakta yok). `--sablon` verilirse şablonun stilleri kullanılır (pandoc `--reference-doc`); şablondaki **metin** (örn. hazır kapak) alınmaz, yalnız stiller ve sayfa düzeni.

## Gerekenler

Python 3.11+ (yalnız standart kütüphane), [pandoc](https://pandoc.org/) 3, LibreOffice (`soffice`, PDF için), poppler (`pdftotext`, `pdfinfo`), `ffmpeg` (ekran görüntüsü kırpma), Node.js 22+ ve Google Chrome (Mermaid için; Mermaid 11.4.1 jsDelivr'dan yüklenir, internet gerekir). Chrome başka bir yoldaysa `CHROME=/yol/chrome`. macOS'ta: `brew install pandoc poppler ffmpeg` ve `brew install --cask libreoffice`.

## 30.11'de yeniden üretmek

1. Bölüm dosyalarındaki ⟨…⟩ yerlerini doldurun (bug bash, demo provası, son sayılar); kapak bilgilerini `on-sayfalar.md`'de tamamlayın.
2. Hocanın şablonu elinizdeyse `--sablon` ile çalıştırın. Şablondaki başlık sırası bölüm dosyalarındakinden farklıysa sıra Markdown'da düzeltilir.
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
