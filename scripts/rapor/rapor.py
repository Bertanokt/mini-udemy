#!/usr/bin/env python3
"""Proje raporunu Word'de birleştirir ve PDF'ini üretir (#69).

Kullanım (repo kökünden):
    python3 scripts/rapor/rapor.py                      # varsayılan stil
    python3 scripts/rapor/rapor.py --sablon sablon.docx # hocanın Word şablonunun düzeniyle

Girdi : rapor/on-sayfalar.md, rapor/bolum-1-3.md, rapor/bolum-4.md, rapor/bolum-5-8.md, rapor/kaynaklar.md,
        diyagramlar (docs/diagrams/*.png) ve ekran görüntüleri (docs/screenshots/*.png).
Çıktı : rapor/Mini_Udemy_Proje_Raporu.docx ve .pdf. ⟨…⟩ yer tutucuları sarıyla işaretlenir ve listesi yazdırılır.
Gerekenler: pandoc 3, LibreOffice (soffice), poppler (pdftotext), ffmpeg, Node 22 + Google Chrome (Mermaid için).
Yalnız Python standart kütüphanesi kullanılır. Ayrıntı: scripts/rapor/README.md
"""
import argparse
import datetime
import os
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[2]
RAPOR = ROOT / "rapor"
HERE = Path(__file__).resolve().parent
REPO_URL = "https://github.com/Bertanokt/mini-udemy"
BOLUMLER = ["bolum-1-3.md", "bolum-4.md", "bolum-5-8.md"]
CIKTI = RAPOR / "Mini_Udemy_Proje_Raporu"

# Sayfa düzeni (twip; 567 twip = 1 cm). Varsayılan A4 ve 2,5 cm kenar; --sablon verilirse şablonun son bölümünden okunur.
LAYOUT = {"w": 11906, "h": 16838, "top": 1418, "right": 1418, "bottom": 1418, "left": 1418, "header": 709, "footer": 709}
MAX_H_CM = 21.0
TOC_LEVELS = 3
# Ön sayfalardaki yorum işaretleri (on-sayfalar.md) ve betiğin kendi eklediği işaretler; postprocess bunları değiştirir.
TOC_MARK, FIG_MARK, KAPAK_MARK, ON_MARK = "@@ICINDEKILER@@", "@@SEKIL_LISTESI@@", "@@KAPAK_SONU@@", "@@ON_SAYFALAR_SONU@@"
MARKERS = {"<!-- İÇİNDEKİLER -->": TOC_MARK, "<!-- ŞEKİL LİSTESİ -->": FIG_MARK, "<!-- KAPAK SONU -->": KAPAK_MARK}
LISTE_BASLIKLARI = {TOC_MARK: "İÇİNDEKİLER", FIG_MARK: "ŞEKİL LİSTESİ"}


def text_w_tw():
    return LAYOUT["w"] - LAYOUT["left"] - LAYOUT["right"]


def text_w_cm():
    return text_w_tw() / 567


def buyuk(s):
    """Türkçe büyük harf (i → İ, ı → I)."""
    return s.replace("i", "İ").replace("ı", "I").upper()


def raw(mark):
    return f"```{{=openxml}}\n<w:p><w:r><w:t>{mark}</w:t></w:r></w:p>\n```"


def run(cmd, **kw):
    r = subprocess.run(cmd, capture_output=True, text=True, **kw)
    if r.returncode != 0:
        sys.exit(f"HATA: {' '.join(map(str, cmd))}\n{r.stdout}\n{r.stderr}")
    return r.stdout


def need(tool):
    if not shutil.which(tool):
        sys.exit(f"HATA: '{tool}' bulunamadı (scripts/rapor/README.md › Gerekenler)")


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        sys.exit(f"HATA: PNG değil: {path}")
    return struct.unpack(">II", head[16:24])


def fit(path, max_w=None, max_h=MAX_H_CM):
    """Görseli oranını koruyarak metin genişliğine ve sayfa yüksekliğine sığdıran genişlik (cm)."""
    w, h = png_size(path)
    # LibreOffice metin genişliğine eşit görseli birkaç punto sağa kaydırıp kenar boşluğuna taşırıyor; 0,3 cm pay bırakılır
    return round(min(max_w or text_w_cm() - 0.3, max_h * w / h), 2)


# ---------------------------------------------------------------------------------------------- markdown hazırlığı

LINK = re.compile(r"(!?)\[([^\]]*)\]\(([^)\s]+)\)")


def repo_link(target, base):
    """Göreli bağlantıyı GitHub adresine çevirir; rapor içi bağlantılar None döner (yalnız metin kalır)."""
    if re.match(r"^[a-z]+:", target):
        return target
    path, _, anchor = target.partition("#")
    if not path:
        return None
    full = (base / path).resolve()
    try:
        rel = full.relative_to(ROOT)
    except ValueError:
        return None
    if rel.parts and rel.parts[0] == "rapor" and rel.suffix == ".md":
        return None
    kind = "tree" if full.is_dir() else "blob"
    return f"{REPO_URL}/{kind}/main/{rel.as_posix()}" + (f"#{anchor}" if anchor else "")


class Hazirlik:
    def __init__(self, build):
        self.build = build
        self.figures = {}        # bölüm → sayaç
        self.chapter = None
        self.ek = []             # Ek A: (no, rol, ekran, etiket, masaüstü, mobil, koyu)
        self.ek_no = 0
        self.mermaid_no = 0

    def figure(self, path, caption, width=None):
        n = self.figures.get(self.chapter, 0) + 1
        self.figures[self.chapter] = n
        no = f"{self.chapter}.{n}"
        w = width or fit(path)
        return (f'::: {{custom-style="Sekil"}}\n![]({path}){{width={w}cm}}\n:::\n\n'
                f'::: {{custom-style="Sekil Altyazisi"}}\nŞekil {no}. {caption}\n:::\n')

    def links(self, text, base):
        def sub(m):
            bang, label, target = m.groups()
            if bang:
                return m.group(0)
            url = repo_link(target, base)
            return f"[{label}]({url})" if url else label
        return LINK.sub(sub, text)

    def mermaid(self, code):
        need("node")
        self.mermaid_no += 1
        src = self.build / f"mermaid-{self.mermaid_no}.mmd"
        out = self.build / f"mermaid-{self.mermaid_no}.png"
        src.write_text(code, encoding="utf-8")
        run(["node", str(HERE / "mermaid.mjs"), str(src), str(out)])
        title = re.search(r"^\s*title\s+(.+)$", code, re.M)
        return self.figure(out, title.group(1).strip() if title else "Diyagram")

    def screenshot_cell(self, row_cells, cell):
        """3.4.7 tablosundaki ekran görüntüsü bağlantılarını Ek A şekil numaralarına çevirir."""
        rol, ekran = row_cells[0].strip(), row_cells[1].strip()
        refs = []
        for entry in re.split(r"<br\s*/?>", cell):
            files = re.findall(r"\]\(([^)]*docs/screenshots/[^)]+\.png)\)", entry)
            if not files:
                continue
            label = entry.split(":", 1)[0].strip() if re.match(r"^\s*[^\[]+:", entry) else ""
            by_kind = {}
            for f in files:
                name = Path(f).name
                kind = "koyu" if name.endswith("-koyu.png") else ("mobil" if name.endswith("-mobil.png") else "masaustu")
                by_kind[kind] = (RAPOR / f).resolve()
            self.ek_no += 1
            first = self.ek_no
            self.ek.append((first, rol, ekran, label, by_kind.get("masaustu"), by_kind.get("mobil"), None))
            ref = f"A.{first}"
            if "koyu" in by_kind:
                self.ek_no += 1
                self.ek.append((self.ek_no, rol, ekran, label, None, None, by_kind["koyu"]))
                ref += f" (koyu A.{self.ek_no})"
            refs.append(f"{label}: {ref}" if label else ref)
        return "Şekil " + "; ".join(refs) if refs else None

    def table_widths(self, rows):
        """Pipe tablolarında sütun genişliklerini içerik uzunluğuna göre ayarlar (pandoc ayırıcı satırdaki tire sayısına bakar)."""
        cells = [split_row(r) for r in rows if not re.match(r"^\|[\s:|-]+\|\s*$", r)]
        ncol = max(len(c) for c in cells)
        # Her sütuna en az başlıktaki en uzun kelime kadar yer; kalan yer ortalama içerik uzunluğuna göre dağıtılır.
        clean = lambda c: re.sub(r"\]\([^)]*\)|[`*\[]", "", c)
        mins, prefs = [], []
        for i in range(ncol):
            body = [clean(c[i]) for c in cells[1:] if i < len(c)] or [""]
            head = clean(cells[0][i]) if i < len(cells[0]) else ""
            words = [len(w) for t in body for w in t.split()] or [0]
            mins.append(max(5, max((len(w) for w in head.split()), default=0) + 2, min(max(words), 14) + 1))
            prefs.append(sum(len(t) for t in body) / len(body))
        capacity = 84  # 16 cm'lik satıra sığan yaklaşık karakter (9,5 pt, hücre boşlukları düşülmüş)
        extra = [max(0.0, p - m) for p, m in zip(prefs, mins)]
        room = max(0, capacity - sum(mins))
        lens = [m + (room * e / sum(extra) if sum(extra) else 0) for m, e in zip(mins, extra)]
        total = sum(lens)
        sep = "|" + "|".join("-" * max(3, round(100 * l / total)) for l in lens) + "|"
        return [sep if re.match(r"^\|[\s:|-]+\|\s*$", r) else r for r in rows]

    def chapter_md(self, path):
        text = path.read_text(encoding="utf-8")
        lines = text.split("\n")
        start = next(i for i, l in enumerate(lines) if re.match(r"^## \d+\.", l))
        out, i, in_code, table, after = [], start, False, [], []

        def flush_table():
            if table:
                out.extend(self.table_widths(table))
                table.clear()
                out.append("")
                out.extend(after)
                after.clear()

        def cell_image(m):
            src = (path.parent / m.group(2)).resolve()
            after.append(self.figure(src, m.group(1)))
            return f"Şekil {self.chapter}.{self.figures[self.chapter]}"

        while i < len(lines):
            line = lines[i]
            if line.startswith("```"):
                flush_table()
                if line.strip() == "```mermaid" and not in_code:
                    j = i + 1
                    while not lines[j].startswith("```"):
                        j += 1
                    out.append(self.mermaid("\n".join(lines[i + 1:j])))
                    i = j + 1
                    continue
                in_code = not in_code
                out.append(line)
                i += 1
                continue
            if in_code:
                out.append(line)
                i += 1
                continue
            if line.strip() == "---":
                flush_table()
                i += 1
                continue
            m = re.match(r"^(#{2,6}) (.*)$", line)
            if m:
                flush_table()
                title = m.group(2)
                if len(m.group(1)) == 2:  # bölüm başlığı şablondaki gibi büyük harf: "1. GİRİŞ"
                    self.chapter = re.match(r"(\d+)\.", title).group(1)
                    title = buyuk(title)
                else:  # alt başlık numarası şablondaki gibi noktalı: "1.1. Amaç"
                    title = re.sub(r"^(\d+(?:\.\d+)+)(?=\s)", r"\1.", title)
                out.append(m.group(1)[1:] + " " + title)
                i += 1
                continue
            img = re.match(r"^!\[([^\]]*)\]\(([^)]+)\)\s*$", line)
            if img:
                flush_table()
                out.append(self.figure((path.parent / img.group(2)).resolve(), img.group(1)))
                i += 1
                continue
            if line.startswith("|"):
                cells = split_row(line)
                if any("docs/screenshots/" in c for c in cells):
                    cells = [self.screenshot_cell(cells, c) or c for c in cells]
                    line = "| " + " | ".join(cells) + " |"
                elif re.search(r"!\[[^\]]*\]\([^)]+\.png\)", line):
                    line = re.sub(r"!\[([^\]]*)\]\(([^)]+\.png)\)", cell_image, line)
                elif cells and cells[-1].strip() == "Görüntü" and any("docs/screenshots/" in l for l in lines[i:i + 4]):
                    cells[-1] = "Görüntü (Ek A)"
                    line = "| " + " | ".join(cells) + " |"
                table.append(self.links(line, path.parent))
                i += 1
                continue
            flush_table()
            out.append(self.links(line, path.parent))
            i += 1
        flush_table()
        return "\n".join(out)

    def appendix_md(self):
        if not self.ek:
            return ""
        date = run(["git", "log", "-1", "--format=%cd", "--date=format:%d.%m.%Y", "--", "docs/screenshots"], cwd=ROOT).strip()
        parts = ["# EKLER", "", "## Ek A. Ekran görüntüleri", "",
                 f"Görüntüler seed verisiyle {date} tarihinde çekildi (3.4.7). Her şekilde solda masaüstü (1280 px), sağda mobil "
                 "(360 px) görünüm vardır; koyu tema örnekleri ayrı şekillerdedir. Uzun sayfaların yalnız ilk ekranı gösterilir; "
                 f"tam boy görüntüler [`docs/screenshots/`]({REPO_URL}/tree/main/docs/screenshots) altındadır.", ""]
        crop = self.build / "kirp"
        crop.mkdir(exist_ok=True)
        for no, rol, ekran, label, desk, mob, dark in self.ek:
            caption = f"{rol} — {ekran}" + (f" ({label})" if label else "")
            shots = []
            for src, max_h in ((desk, 960), (mob, 780), (dark, 960)):
                if src is None:
                    continue
                if not src.exists():
                    sys.exit(f"HATA: ekran görüntüsü yok: {src}")
                dst = crop / src.name
                run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-vf", f"crop=iw:min(ih\\,{max_h}):0:0", str(dst)])
                w, h = png_size(dst)
                shots.append((dst, w / h))
            # Görüntüler yan yana ve aynı yükseklikte; toplam genişlik metin genişliğini aşmaz.
            height = min(8.0 if dark is None else 9.5, (text_w_cm() - 2.0) / sum(r for _, r in shots))
            imgs = [f"![]({dst}){{width={round(height * r, 2)}cm}}" for dst, r in shots]
            if dark is not None:
                caption += ", koyu tema"
            parts += ['::: {custom-style="Sekil"}', "  ".join(imgs), ":::", "",
                      '::: {custom-style="Sekil Altyazisi"}', f"Şekil A.{no}. {caption}", ":::", ""]
        return "\n".join(parts)


def split_row(line):
    s = line.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|"):
        s = s[:-1]
    cells, cur, depth = [], "", 0
    for ch in s:  # bağlantı ve kod içindeki | işaretini bölme
        if ch == "`":
            depth ^= 1
        if ch == "|" and not depth:
            cells.append(cur)
            cur = ""
        else:
            cur += ch
    cells.append(cur)
    return [c.strip() for c in cells]


def front_md(path):
    text = path.read_text(encoding="utf-8")
    for comment, mark in MARKERS.items():
        text = text.replace(comment, raw(mark))
    return re.sub(r"<!--.*?-->\s*", "", text, flags=re.S)


# ---------------------------------------------------------------------------------------------- Word stilleri

def rfonts(name):
    return f'<w:rFonts w:ascii="{name}" w:hAnsi="{name}" w:eastAsia="{name}" w:cs="{name}"/>'


MONO = rfonts("Courier New").replace(' w:eastAsia="Courier New"', "")


def sz(half_points):
    return f'<w:sz w:val="{half_points}"/><w:szCs w:val="{half_points}"/>'


def pstyle(sid, name, ppr="", rpr="", based="Normal", nxt="BodyText", extra=""):
    return (f'<w:style w:type="paragraph" w:customStyle="1" w:styleId="{sid}"><w:name w:val="{name}"/>'
            f'<w:basedOn w:val="{based}"/><w:next w:val="{nxt}"/><w:qFormat/>{extra}'
            f'<w:pPr>{ppr}</w:pPr><w:rPr>{rpr}</w:rPr></w:style>')


def heading(font, level, size, before, after, page_break=False):
    return (f'<w:style w:type="paragraph" w:styleId="Heading{level}"><w:name w:val="heading {level}"/>'
            f'<w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:uiPriority w:val="9"/><w:qFormat/>'
            f'<w:pPr><w:keepNext/><w:keepLines/>{"<w:pageBreakBefore/>" if page_break else ""}'
            f'<w:spacing w:before="{before}" w:after="{after}" w:line="276" w:lineRule="auto"/><w:jc w:val="left"/>'
            f'<w:outlineLvl w:val="{level - 1}"/></w:pPr>'
            f'<w:rPr>{font}<w:b/><w:bCs/><w:color w:val="000000"/>{sz(size)}</w:rPr></w:style>')


def toc_style(sid, name, level, bold, line=264, after=60, indent=360):
    return (f'<w:style w:type="paragraph" w:styleId="{sid}"><w:name w:val="{name}"/>'
            f'<w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="39"/>'
            f'<w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="{text_w_tw()}"/></w:tabs>'
            f'<w:spacing w:before="{60 if level == 1 and bold else 0}" w:after="{after}" w:line="{line}" w:lineRule="auto"/>'
            f'<w:ind w:left="{max(0, level - 1) * indent}"/><w:jc w:val="left"/></w:pPr>'
            f'<w:rPr>{"<w:b/>" if level == 1 and bold else ""}</w:rPr></w:style>')


def build_styles(sb):
    """Rapor stilleri. Şablon (sb) verilirse yazı tipi, punto, satır aralığı, paragraf sonrası boşluk ve şekil altyazısı
    şablondan gelir; başlıklar şablondaki gibi metin puntosunda ve kalın, tablolar siyah çizgili olur."""
    font = rfonts(sb.get("font", "Times New Roman"))
    size, line, after = sb.get("size", 24), sb.get("line", 360), sb.get("after", 120)
    sablon = bool(sb)
    h1, h2, h3 = (size, size, size) if sablon else (32, 28, 24)
    caption = sb.get("caption", "<w:i/>" + sz(20))
    border = "000000" if sablon else "9CA3AF"
    head_fill = "" if sablon else '<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="E5E7EB"/></w:tcPr>'
    edges = "".join(f'<w:{e} w:val="single" w:sz="4" w:space="0" w:color="{border}"/>'
                    for e in ("top", "left", "bottom", "right", "insideH", "insideV"))
    box = "".join(f'<w:{e} w:val="single" w:sz="6" w:space="10" w:color="000000"/>' for e in ("top", "left", "bottom", "right"))
    center = '<w:jc w:val="center"/>'
    styles = {
        "BodyText": pstyle("BodyText", "Body Text", f'<w:spacing w:before="0" w:after="{after}" w:line="{line}" w:lineRule="auto"/><w:jc w:val="both"/>'),
        "FirstParagraph": pstyle("FirstParagraph", "First Paragraph", "", "", based="BodyText"),
        "Compact": pstyle("Compact", "Compact", '<w:spacing w:before="0" w:after="40" w:line="312" w:lineRule="auto"/>', "", based="BodyText"),
        "TabloMetni": pstyle("TabloMetni", "Tablo Metni", '<w:spacing w:before="20" w:after="20" w:line="240" w:lineRule="auto"/><w:jc w:val="left"/>', sz(19)),
        "Heading1": heading(font, 1, h1, 0, 240, page_break=True),
        "Heading2": heading(font, 2, h2, 360 if not sablon else 280, 160 if not sablon else 120),
        "Heading3": heading(font, 3, h3, 280 if not sablon else 240, 120),
        "Heading4": heading(font, 4, size, 240, 120),
        "SourceCode": pstyle("SourceCode", "Source Code", '<w:shd w:val="clear" w:color="auto" w:fill="F3F4F6"/><w:spacing w:before="0" w:after="120" w:line="240" w:lineRule="auto"/><w:jc w:val="left"/>',
                             f'{MONO}{sz(18)}'),
        "Sekil": pstyle("Sekil", "Sekil", f'<w:keepNext/><w:spacing w:before="160" w:after="60" w:line="240" w:lineRule="auto"/>{center}'),
        "SekilAltyazisi": pstyle("SekilAltyazisi", "Sekil Altyazisi", f'<w:spacing w:before="0" w:after="240" w:line="240" w:lineRule="auto"/>{center}', caption),
        # Kapak (şablondaki düzen): logo, üniversite, fakülte ve bölüm, çerçeveli proje adı, ders, hazırlayanlar, danışman, yer ve tarih
        "KapakLogo": pstyle("KapakLogo", "Kapak Logo", f'<w:spacing w:before="0" w:after="120" w:line="240" w:lineRule="auto"/>{center}'),
        "KapakKurum": pstyle("KapakKurum", "Kapak Kurum", f'<w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(36)),
        "KapakBirim": pstyle("KapakBirim", "Kapak Birim", f'<w:spacing w:before="0" w:after="1100" w:line="240" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(size)),
        "KapakBaslik": pstyle("KapakBaslik", "Kapak Baslik", f'<w:pBdr>{box}</w:pBdr><w:spacing w:before="0" w:after="900" w:line="360" w:lineRule="auto"/>'
                              f'<w:ind w:left="200" w:right="200"/>{center}', "<w:b/><w:bCs/>" + sz(size)),
        "KapakDers": pstyle("KapakDers", "Kapak Ders", f'<w:spacing w:before="0" w:after="1000" w:line="240" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(size)),
        "KapakEtiket": pstyle("KapakEtiket", "Kapak Etiket", f'<w:keepNext/><w:spacing w:before="0" w:after="200" w:line="240" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(size)),
        "KapakUye": pstyle("KapakUye", "Kapak Uye", '<w:spacing w:before="0" w:after="0" w:line="360" w:lineRule="auto"/><w:jc w:val="left"/>', sz(size)),
        "KapakDanisman": pstyle("KapakDanisman", "Kapak Danisman", f'<w:spacing w:before="1400" w:after="0" w:line="360" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(size)),
        "KapakTarih": pstyle("KapakTarih", "Kapak Tarih", f'<w:spacing w:before="700" w:after="0" w:line="240" w:lineRule="auto"/>{center}', "<w:b/><w:bCs/>" + sz(20)),
        # İçindekiler ve şekil listesi başlığı bölüm başlığı gibi görünür ve Word'ün içindekiler alanına girer (şablondaki gibi)
        "TOCHeading": pstyle("TOCHeading", "TOC Heading", f'<w:keepNext/><w:pageBreakBefore/><w:spacing w:before="0" w:after="240"/><w:outlineLvl w:val="0"/>',
                             f'{font}<w:b/><w:bCs/>{sz(h1)}', nxt="Normal"),
        # şablonda içindekiler gövde metni aralığında (1,5 satır, 5 pt sonra), düzey başına 0,42 cm girinti
        **{f"TOC{n}": toc_style(f"TOC{n}", f"toc {n}", n, not sablon, *((line, 100, 240) if sablon else ())) for n in (1, 2, 3)},
        "TableofFigures": toc_style("TableofFigures", "table of figures", 0, False, *((line, 100) if sablon else (264, 40))),
        "YerTutucu": ('<w:style w:type="character" w:customStyle="1" w:styleId="YerTutucu"><w:name w:val="Yer Tutucu"/>'
                      '<w:rPr><w:highlight w:val="yellow"/></w:rPr></w:style>'),
        "Table": ('<w:style w:type="table" w:styleId="Table"><w:name w:val="Table"/><w:basedOn w:val="TableNormal"/><w:uiPriority w:val="99"/>'
                  '<w:pPr><w:spacing w:before="20" w:after="20" w:line="240" w:lineRule="auto"/></w:pPr>'
                  f'<w:tblPr><w:tblBorders>{edges}</w:tblBorders>'
                  '<w:tblCellMar><w:top w:w="30" w:type="dxa"/><w:left w:w="80" w:type="dxa"/><w:bottom w:w="30" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr>'
                  f'<w:tblStylePr w:type="firstRow"><w:rPr><w:b/></w:rPr>{head_fill}</w:tblStylePr></w:style>'),
    }
    defaults = (f'<w:docDefaults><w:rPrDefault><w:rPr>{font}{sz(size)}<w:lang w:val="tr-TR" w:eastAsia="en-US" w:bidi="ar-SA"/>'
                f'</w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="{after}" w:line="{line}" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>')
    return styles, defaults


def read_zip(path):
    with zipfile.ZipFile(path) as z:
        return {n: z.read(n) for n in z.namelist() if not n.endswith("/")}


def write_zip(path, files):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name in ["[Content_Types].xml"] + sorted(n for n in files if n != "[Content_Types].xml"):
            z.writestr(name, files[name])


def sablon_oku(path):
    """Hocanın şablonundan (.docx/.dotx) sayfa düzenini, gövde metninin yazı tipini ve aralıklarını, şekil altyazısı
    biçimini ve kapaktaki logoyu okur. Şablonun metni ve stil dosyası rapora kopyalanmaz."""
    files = read_zip(path)
    doc = files["word/document.xml"].decode("utf-8")
    styles = files["word/styles.xml"].decode("utf-8")
    sb = {}

    def num(xml, attr):
        m = re.search(rf'\bw:{attr}="(-?\d+)"', xml or "")
        return int(m.group(1)) if m else None

    sects = re.findall(r"<w:sectPr\b.*?</w:sectPr>", doc, re.S)
    if sects:
        pg = re.search(r"<w:pgSz\b[^>]*>", sects[-1])
        mar = re.search(r"<w:pgMar\b[^>]*>", sects[-1])
        if pg and mar:
            sb["layout"] = {"w": num(pg.group(0), "w"), "h": num(pg.group(0), "h"),
                            **{k: num(mar.group(0), k) for k in ("top", "right", "bottom", "left", "header", "footer")}}

    def style(pattern):
        m = re.search(r'<w:style\b[^>]*>(?:(?!</w:style>).)*?' + pattern + r'.*?</w:style>', styles, re.S)
        return m.group(0) if m else ""

    normal = re.search(r'<w:style [^>]*w:type="paragraph"[^>]*w:default="1"[^>]*>.*?</w:style>', styles, re.S)
    normal = normal.group(0) if normal else ""
    font = re.search(r'w:ascii="([^"]+)"', normal)
    if font:
        sb["font"] = font.group(1)
    for key, attr, src in (("size", "val", re.search(r"<w:sz [^>]*>", normal)), ("line", "line", re.search(r"<w:spacing [^>]*>", normal))):
        if src and num(src.group(0), attr):
            sb[key] = num(src.group(0), attr)
    spacing = re.search(r"<w:spacing [^>]*>", normal)
    default_spacing = re.search(r"<w:pPrDefault>.*?(<w:spacing [^>]*>)", styles, re.S)
    after = num(spacing.group(0), "after") if spacing else None
    if after is None and default_spacing:
        after = num(default_spacing.group(1), "after")
    if after is not None:
        sb["after"] = after
    caption = style(r'<w:name w:val="caption"/>')
    if caption:
        rpr = re.search(r"<w:rPr>(.*?)</w:rPr>", caption, re.S)
        rpr = rpr.group(1) if rpr else ""
        sb["caption"] = ("<w:b/><w:bCs/>" if "<w:b/>" in rpr else "") + ("<w:i/>" if "<w:i/>" in rpr else "") + sz(num(rpr, "val") or 20)

    # Kapak logosu: ilk bölümdeki ilk görsel
    cover = doc[:doc.find("<w:sectPr")] if "<w:sectPr" in doc else doc
    drawing = re.search(r"<w:drawing>.*?</w:drawing>", cover, re.S)
    if drawing:
        embed = re.search(r'r:embed="([^"]+)"', drawing.group(0))
        extent = re.search(r'<wp:extent cx="(\d+)" cy="(\d+)"', drawing.group(0))
        rels = files["word/_rels/document.xml.rels"].decode("utf-8")
        for rel in re.findall(r"<Relationship\b[^>]*>", rels):
            if embed and f'Id="{embed.group(1)}"' in rel:
                target = re.search(r'Target="([^"]+)"', rel).group(1)
                data = files.get("word/" + target.lstrip("/").removeprefix("word/"))
                suffix = Path(target).suffix.lower()
                if data and suffix in (".png", ".jpg", ".jpeg") and extent:
                    sb["logo"] = (data, suffix, round(int(extent.group(1)) / 360000, 2))
                    # şablonda logo paragrafın üstüne, kenar boşluğuna taşacak şekilde sabitlenmiş; kapak bölümünün üst
                    # boşluğu bu kadar azaltılır (EMU → twip: 635)
                    up = re.search(r'<wp:positionV relativeFrom="paragraph"><wp:posOffset>(-\d+)</wp:posOffset>', drawing.group(0))
                    if up:
                        sb["cover_top"] = max(567, sb.get("layout", LAYOUT)["top"] + int(up.group(1)) // 635)
    return sb


def make_reference(build, sb):
    """pandoc'un varsayılan reference.docx'ini rapor düzenine çeker: Times New Roman 12 (ya da şablonun yazı tipi),
    1,5 satır, iki yana yaslı, siyah başlıklar (bölüm başlığı yeni sayfada), çizgili tablolar; sayfa boyutu ve kenarlar
    LAYOUT'tan. Üst ve alt bilgiler (sayfa numaraları) postprocess'te bölüm bölüm eklenir."""
    ref = build / "referans.docx"
    with open(ref, "wb") as f:
        f.write(subprocess.run(["pandoc", "--print-default-data-file", "reference.docx"], capture_output=True, check=True).stdout)
    files = read_zip(ref)
    styles = files["word/styles.xml"].decode("utf-8")
    own, defaults = build_styles(sb)
    styles = re.sub(r"<w:docDefaults>.*?</w:docDefaults>", defaults, styles, flags=re.S)
    for sid, xml in own.items():
        styles = re.sub(rf'<w:style [^>]*w:styleId="{sid}"[^>]*>.*?</w:style>', "", styles, flags=re.S)
        styles = styles.replace("</w:styles>", xml + "</w:styles>")
    # Başlık renkleri ve karakter stillerinin yazı tipleri (pandoc varsayılanı mavi ve Cambria)
    styles = re.sub(r'(<w:style [^>]*w:styleId="(?:Title|Subtitle)"[^>]*>.*?)<w:color w:val="[0-9A-Fa-f]+"/>', r"\1", styles, flags=re.S)
    styles = re.sub(r'(<w:style [^>]*w:styleId="VerbatimChar"[^>]*>.*?<w:rPr>)', rf"\1{MONO}<w:sz w:val='20'/>".replace("'", '"'), styles, count=1, flags=re.S)
    files["word/styles.xml"] = styles.encode("utf-8")
    doc = files["word/document.xml"].decode("utf-8")
    doc = re.sub(r"<w:sectPr\b.*?</w:sectPr>", sect_pr(), doc, flags=re.S)
    files["word/document.xml"] = doc.encode("utf-8")
    write_zip(ref, files)
    return ref


# ---------------------------------------------------------------------------------------------- bölümler, üst/alt bilgi

def page_field(align):
    return (f'<w:p><w:pPr><w:jc w:val="{align}"/><w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>'
            '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>'
            '<w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>')


W_NS = 'xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"'
HF_PARTS = {  # rId: (dosya, tür, içerik). Şablondaki gibi: kapakta numara yok, ön sayfalarda altta ortada i, ii…, gövdede sağ üstte 1, 2…
    "rIdRaporUstBos": ("header-bos.xml", "header", "<w:p/>"),
    "rIdRaporAltBos": ("footer-bos.xml", "footer", "<w:p/>"),
    "rIdRaporAltRoma": ("footer-roma.xml", "footer", page_field("center")),
    "rIdRaporUstSayfa": ("header-sayfa.xml", "header", page_field("right")),
}


def sect_pr(header=None, footer=None, numbering="", top=None):
    refs = (f'<w:headerReference w:type="default" r:id="{header}"/>' if header else "") + \
           (f'<w:footerReference w:type="default" r:id="{footer}"/>' if footer else "")
    L = LAYOUT
    return (f'<w:sectPr>{refs}<w:pgSz w:w="{L["w"]}" w:h="{L["h"]}"/>'
            f'<w:pgMar w:top="{top or L["top"]}" w:right="{L["right"]}" w:bottom="{L["bottom"]}" w:left="{L["left"]}" '
            f'w:header="{L["header"]}" w:footer="{L["footer"]}" w:gutter="0"/>{numbering}</w:sectPr>')


def section_break(sect):
    """Bölümü bitiren boş paragraf: yüksekliği yok denecek kadar küçük, sonraki içerik yeni sayfada başlar."""
    return f'<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="20" w:lineRule="exact"/>{sect}</w:pPr></w:p>'


def add_header_footer_parts(files):
    rels = files["word/_rels/document.xml.rels"].decode("utf-8")
    ct = files["[Content_Types].xml"].decode("utf-8")
    for rid, (name, kind, body) in HF_PARTS.items():
        tag = "hdr" if kind == "header" else "ftr"
        files[f"word/{name}"] = (f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:{tag} {W_NS}>{body}</w:{tag}>').encode("utf-8")
        if f'Id="{rid}"' not in rels:
            rels = rels.replace("</Relationships>", f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/{kind}" Target="{name}"/></Relationships>')
        if f'/word/{name}"' not in ct:
            ct = ct.replace("</Types>", f'<Override PartName="/word/{name}" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.{kind}+xml"/></Types>')
    files["word/_rels/document.xml.rels"] = rels.encode("utf-8")
    files["[Content_Types].xml"] = ct.encode("utf-8")


def cover_members(doc):
    """Kapaktaki "Kapak Uye" paragraflarını (ad, numara, ad, numara …) şablondaki gibi iki sütuna dizer (çizgisiz tablo)."""
    block = re.search(r'(?:<w:p>(?:(?!</w:p>).)*?<w:pStyle w:val="KapakUye"\s*/>(?:(?!</w:p>).)*</w:p>\s*)+', doc, re.S)
    if not block:
        return doc
    paras = re.findall(r"<w:p>.*?</w:p>", block.group(0), re.S)
    members = [paras[i:i + 2] for i in range(0, len(paras), 2)]
    half = text_w_tw() // 2
    none = "".join(f'<w:{e} w:val="nil"/>' for e in ("top", "left", "bottom", "right", "insideH", "insideV"))
    rows = []
    for i in range(0, len(members), 2):
        cells = members[i:i + 2] + [["<w:p/>"]] * (2 - len(members[i:i + 2]))
        rows.append("<w:tr>" + "".join(f'<w:tc><w:tcPr><w:tcW w:w="{half}" w:type="dxa"/></w:tcPr>{"".join(c)}</w:tc>' for c in cells) + "</w:tr>")
    table = (f'<w:tbl><w:tblPr><w:tblW w:w="{2 * half}" w:type="dxa"/><w:tblBorders>{none}</w:tblBorders><w:tblLayout w:type="fixed"/>'
             '<w:tblCellMar><w:left w:w="0" w:type="dxa"/><w:right w:w="0" w:type="dxa"/></w:tblCellMar></w:tblPr>'
             f'<w:tblGrid><w:gridCol w:w="{half}"/><w:gridCol w:w="{half}"/></w:tblGrid>{"".join(rows)}</w:tbl>')
    return doc[:block.start()] + table + doc[block.end():]


# ---------------------------------------------------------------------------------------------- yer tutucu, içindekiler

LUA = r'''
-- ⟨…⟩ arasını "Yer Tutucu" karakter stiline sarar (Word'de sarı vurgu).
local function mark(inlines)
  local out, buf, open = pandoc.List(), nil, false
  local function emit(x) if open then buf:insert(x) else out:insert(x) end end
  for _, el in ipairs(inlines) do
    if el.t == "Str" and (el.text:find("⟨", 1, true) or el.text:find("⟩", 1, true)) then
      local s = el.text
      while #s > 0 do
        if not open then
          local i = s:find("⟨", 1, true)
          if not i then out:insert(pandoc.Str(s)); s = "" else
            if i > 1 then out:insert(pandoc.Str(s:sub(1, i - 1))) end
            open = true; buf = pandoc.List(); s = s:sub(i)
          end
        else
          local j = s:find("⟩", 1, true)
          if not j then buf:insert(pandoc.Str(s)); s = "" else
            local stop = j + #"⟩" - 1
            buf:insert(pandoc.Str(s:sub(1, stop)))
            out:insert(pandoc.Span(buf, {["custom-style"] = "Yer Tutucu"}))
            open = false; buf = nil; s = s:sub(stop + 1)
          end
        end
      end
    else
      emit(el)
    end
  end
  if open then out:insert(pandoc.Span(buf, {["custom-style"] = "Yer Tutucu"})) end
  return out
end
return {{ Inlines = mark }}
'''


def placeholders(md):
    """(başlık, ⟨…⟩) listesi; kod blokları dahil."""
    found, head = [], "Kapak"
    for line in md.split("\n"):
        h = re.match(r"^(#{1,6}) (.*)$", line)
        if h:
            head = h.group(2)
        for m in re.finditer(r"⟨[^⟩]*⟩", line):
            found.append((head, m.group(0)))
    return found


def clean(text):
    return re.sub(r"[`*]", "", re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)).strip()


def headings(md):
    """İçindekiler girdileri (düzey, başlık); İÇİNDEKİLER ve ŞEKİL LİSTESİ de şablondaki gibi listede yer alır."""
    out, in_code = [], False
    for line in md.split("\n"):
        for mark, title in LISTE_BASLIKLARI.items():
            if mark in line:
                out.append((1, title))
        if line.startswith("```"):
            in_code = not in_code
            continue
        m = None if in_code else re.match(r"^(#{1,%d}) (.*)$" % TOC_LEVELS, line)
        if m:
            out.append((len(m.group(1)), clean(m.group(2))))
    return out


def figures(md):
    """Şekil altyazıları, rapordaki sırayla ("Şekil 4.2. Bileşen diyagramı")."""
    return [clean(c) for c in re.findall(r'::: \{custom-style="Sekil Altyazisi"\}\n(Şekil [^\n]+)', md)]


def toc_xml(entries, pages):
    paras = ['<w:p><w:pPr><w:pStyle w:val="TOCHeading"/></w:pPr><w:r><w:t>İÇİNDEKİLER</w:t></w:r></w:p>']
    for k, (level, text) in enumerate(entries):
        begin = ('<w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
                 '<w:r><w:instrText xml:space="preserve"> TOC \\o "1-%d" \\h \\z \\u </w:instrText></w:r>'
                 '<w:r><w:fldChar w:fldCharType="separate"/></w:r>' % TOC_LEVELS) if k == 0 else ""
        end = '<w:r><w:fldChar w:fldCharType="end"/></w:r>' if k == len(entries) - 1 else ""
        page = str(pages[k]) if pages and pages[k] else "–"
        paras.append(f'<w:p><w:pPr><w:pStyle w:val="TOC{level}"/></w:pPr>{begin}'
                     f'<w:r><w:t xml:space="preserve">{escape(text)}</w:t></w:r><w:r><w:tab/></w:r><w:r><w:t>{page}</w:t></w:r>{end}</w:p>')
    return "".join(paras)


def fig_xml(figs, pages):
    """Şekil listesi: altyazılar Word alanı (SEQ) olmadığı için düz metin; sayfa numaraları betik her çalıştığında yenilenir."""
    paras = ['<w:p><w:pPr><w:pStyle w:val="TOCHeading"/></w:pPr><w:r><w:t>ŞEKİL LİSTESİ</w:t></w:r></w:p>']
    for k, text in enumerate(figs):
        page = str(pages[k]) if pages and pages[k] else "–"
        paras.append(f'<w:p><w:pPr><w:pStyle w:val="TableofFigures"/></w:pPr><w:r><w:t xml:space="preserve">{escape(text)}</w:t></w:r>'
                     f'<w:r><w:tab/></w:r><w:r><w:t>{page}</w:t></w:r></w:p>')
    return "".join(paras)


def fix_table(tbl):
    """Tablo hücreleri küçük, tek satır aralıklı ve sola yaslı; tam genişlikli tabloda sütunlar metin genişliğine ölçeklenir."""
    tbl = re.sub(r'<w:pStyle w:val="(?:Compact|BodyText|FirstParagraph)"\s*/>', '<w:pStyle w:val="TabloMetni"/>', tbl)
    if re.search(r'<w:tblW w:type="pct" w:w="5000"\s*/>', tbl):
        cols = [int(x) for x in re.findall(r'<w:gridCol w:w="(\d+)"\s*/>', tbl)]
        if cols and sum(cols):
            k = text_w_tw() / sum(cols)
            new = iter(round(c * k) for c in cols)
            tbl = re.sub(r'<w:gridCol w:w="\d+"\s*/>', lambda _: f'<w:gridCol w:w="{next(new)}"/>', tbl)
    return tbl


def mark_para(doc, mark):
    m = re.search(r"<w:p>(?:(?!<w:p>).)*?%s.*?</w:p>" % mark, doc, flags=re.S)
    if not m:
        sys.exit(f"HATA: işaret bulunamadı: {mark}")
    return m


def postprocess(docx, entries, pages, figs, fig_pages, cover_top=None):
    files = read_zip(docx)
    doc = files["word/document.xml"].decode("utf-8")
    # yer tutucu: stilin yanında doğrudan sarı vurgu
    doc = re.sub(r'<w:rStyle w:val="YerTutucu"\s*/>', '<w:rStyle w:val="YerTutucu"/><w:highlight w:val="yellow"/>', doc)
    # tablo hücreleri: küçük, tek satır aralıklı metin
    doc = re.sub(r"<w:tbl>.*?</w:tbl>", lambda m: fix_table(m.group(0)), doc, flags=re.S)
    doc = cover_members(doc)
    for mark, xml in ((TOC_MARK, toc_xml(entries, pages)), (FIG_MARK, fig_xml(figs, fig_pages)),
                      (KAPAK_MARK, section_break(sect_pr(None, "rIdRaporAltBos", top=cover_top))),  # kapakta üst bilgi yok: logo yukarı çıkabilsin
                      (ON_MARK, section_break(sect_pr("rIdRaporUstBos", "rIdRaporAltRoma", '<w:pgNumType w:fmt="lowerRoman" w:start="1"/>')))):
        m = mark_para(doc, mark)
        doc = doc[:m.start()] + xml + doc[m.end():]
    # bölüm sonundan hemen sonra gelen başlık zaten yeni sayfada: ayrıca sayfa sonu koyma (boş sayfa çıkmasın)
    doc = re.sub(r'(</w:sectPr></w:pPr></w:p>\s*(?:<w:bookmarkStart[^>]*/>\s*)*<w:p><w:pPr><w:pStyle w:val="(?:Heading1|TOCHeading)"\s*/>)',
                 r'\1<w:pageBreakBefore w:val="0"/>', doc)
    # gövde: sağ üstte 1'den başlayan sayfa numarası
    doc = re.sub(r"<w:sectPr\b(?:(?!<w:sectPr\b).)*?</w:sectPr>\s*</w:body>",
                 sect_pr("rIdRaporUstSayfa", "rIdRaporAltBos", '<w:pgNumType w:start="1"/>') + "</w:body>", doc, flags=re.S)
    if 'xmlns:r="' not in doc[:3000]:
        doc = doc.replace("<w:document ", '<w:document xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" ', 1)
    files["word/document.xml"] = doc.encode("utf-8")
    add_header_footer_parts(files)
    settings = files["word/settings.xml"].decode("utf-8")
    if "updateFields" not in settings:  # Word açılışta içindekileri güncellemeyi önersin
        settings = settings.replace("</w:settings>", '<w:updateFields w:val="true"/></w:settings>')
    files["word/settings.xml"] = settings.encode("utf-8")
    write_zip(docx, files)


def to_pdf(docx, outdir):
    run(["soffice", "--headless", "--norestore", "--convert-to", "pdf", "--outdir", str(outdir), str(docx)])
    return outdir / (docx.stem + ".pdf")


def roman(n):
    out = ""
    for value, sym in ((1000, "m"), (900, "cm"), (500, "d"), (400, "cd"), (100, "c"), (90, "xc"), (50, "l"), (40, "xl"),
                       (10, "x"), (9, "ix"), (5, "v"), (4, "iv"), (1, "i")):
        while n >= value:
            out, n = out + sym, n - value
    return out


def find_pages(pdf, entries, figs):
    """Başlıkların ve şekillerin PDF'teki sayfaları, sayfada görünen numarayla: ön sayfalar (ÖNSÖZ'den ilk bölüme kadar)
    i, ii…; gövde 1'den. İçindekiler ve şekil listesi sayfaları atlanır; başlıklar ve şekiller sırayla aranır."""
    pages = run(["pdftotext", "-layout", str(pdf), "-"]).split("\f")
    norm = lambda s: re.sub(r"\s+", " ", s).strip().lower()
    lines = [[norm(l) for l in p.split("\n") if l.strip()] for p in pages]
    leader = re.compile(r"\.{5,}\s*(\d+|[ivxlc]+|–)$")
    lists = {i for i, ls in enumerate(lines) if sum(bool(leader.search(l)) for l in ls) >= 5}
    titles = set(LISTE_BASLIKLARI.values())

    def seek(title, cur):
        key = norm(title)[:45]
        for p in range(cur, len(pages)):
            if title in titles:  # listenin ilk sayfası: başlık sayfanın ilk satırı
                if lines[p] and lines[p][0] == key:
                    return p
            elif p not in lists and any(l.startswith(key) for l in lines[p]):
                return p
        return None

    def walk(items):
        found, cur = [], 0
        for title in items:
            hit = seek(title, cur)
            found.append(hit)
            cur = hit if hit is not None else cur
        return found

    phys = walk([t for _, t in entries])
    fig_phys = walk(figs)
    body = next((phys[i] for i, (l, t) in enumerate(entries) if l == 1 and re.match(r"\d+\.", t)), None)
    front = phys[0] if phys else None

    def shown(p):
        if p is None:
            return None
        if body is not None and front is not None:
            return roman(p - front + 1) if p < body else p - body + 1
        return p + 1
    return [shown(p) for p in phys], [shown(p) for p in fig_phys]


# ---------------------------------------------------------------------------------------------- ana akış

def main():
    ap = argparse.ArgumentParser(description="Proje raporunu Word'de birleştirir (#69).")
    ap.add_argument("--sablon", type=Path, help="hocanın Word şablonu (.docx/.dotx); sayfa düzeni, yazı tipi, aralıklar ve kapak logosu alınır")
    ap.add_argument("--cikti", type=Path, default=CIKTI, help="çıktı yolu, uzantısız (varsayılan: rapor/Mini_Udemy_Proje_Raporu)")
    ap.add_argument("--pdf-yok", action="store_true", help="PDF üretme")
    args = ap.parse_args()
    for tool in ("pandoc", "ffmpeg", "git") + (() if args.pdf_yok else ("soffice", "pdftotext", "pdfinfo")):
        need(tool)

    sb = sablon_oku(args.sablon) if args.sablon else {}
    LAYOUT.update(sb.get("layout") or {})

    with tempfile.TemporaryDirectory(prefix="rapor-") as tmp:
        build = Path(tmp)
        h = Hazirlik(build)
        parts = []
        if sb.get("logo"):  # şablonun kapağındaki logo, aynı boyutta
            data, suffix, width = sb["logo"]
            (build / f"logo{suffix}").write_bytes(data)
            parts.append(f'::: {{custom-style="Kapak Logo"}}\n![]({build / f"logo{suffix}"}){{width={width}cm}}\n:::')
        parts += [front_md(RAPOR / "on-sayfalar.md"), raw(ON_MARK)]
        parts += [h.chapter_md(RAPOR / name) for name in BOLUMLER]
        parts.append(front_md(RAPOR / "kaynaklar.md"))
        parts.append(h.appendix_md())
        md = "\n\n".join(parts)
        (build / "rapor.md").write_text(md, encoding="utf-8")
        (build / "yer-tutucu.lua").write_text(LUA, encoding="utf-8")
        ref = make_reference(build, sb)

        docx = build / "rapor.docx"
        run(["pandoc", str(build / "rapor.md"), "-f", "markdown-implicit_figures+pipe_tables+fenced_divs+raw_attribute",
             "-t", "docx", "-o", str(docx), "--reference-doc", str(ref), "--lua-filter", str(build / "yer-tutucu.lua"),
             "--resource-path", f"{RAPOR}:{ROOT}", "--metadata", "lang=tr-TR", "--columns=10"])
        entries = headings(md)
        figs = figures(md)
        base = docx.read_bytes()

        pages = fig_pages = None
        if not args.pdf_yok:
            # 1. geçiş: sayfa numaraları "–" ile; PDF'ten başlıkların ve şekillerin sayfaları okunur, sonraki geçişte yazılır.
            for _ in range(4):
                docx.write_bytes(base)
                postprocess(docx, entries, pages, figs, fig_pages, sb.get("cover_top"))
                pdf = to_pdf(docx, build)
                new, new_figs = find_pages(pdf, entries, figs)
                if (new, new_figs) == (pages, fig_pages):
                    break
                pages, fig_pages = new, new_figs
            docx.write_bytes(base)
            postprocess(docx, entries, pages, figs, fig_pages, sb.get("cover_top"))
            pdf = to_pdf(docx, build)
        else:
            postprocess(docx, entries, None, figs, None, sb.get("cover_top"))

        out = args.cikti
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(docx, out.with_suffix(".docx"))
        page_count = None
        if not args.pdf_yok:
            shutil.copy(pdf, out.with_suffix(".pdf"))
            page_count = re.search(r"Pages:\s+(\d+)", run(["pdfinfo", str(pdf)])).group(1)

    missing = [t for (l, t), p in zip(entries, pages or []) if p is None]
    missing += [t for t, p in zip(figs, fig_pages or []) if p is None]
    ph = placeholders(md)
    print(f"Çıktı: {out.with_suffix('.docx').relative_to(ROOT) if out.is_relative_to(ROOT) else out.with_suffix('.docx')}"
          + ("" if args.pdf_yok else f" ve .pdf ({page_count} sayfa)"))
    print(f"Şablon: {args.sablon or 'yok (varsayılan stil)'} · içindekiler: {len(entries)} başlık · şekil listesi: {len(figs)} şekil"
          + (f" · sayfası bulunamayan: {missing}" if missing else ""))
    print(f"Yer tutucular (⟨…⟩, sarı): {len(ph)}")
    for head, text in ph:
        print(f"  - {head}: {text}")


if __name__ == "__main__":
    main()
