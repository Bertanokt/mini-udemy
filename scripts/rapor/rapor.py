#!/usr/bin/env python3
"""Proje raporunu Word'de birleştirir ve PDF'ini üretir (#69).

Kullanım (repo kökünden):
    python3 scripts/rapor/rapor.py                      # varsayılan stil
    python3 scripts/rapor/rapor.py --sablon sablon.docx # hocanın Word şablonunun stilleriyle

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

# Sayfa: A4, 2,5 cm kenar boşlukları. Metin genişliği 16 cm.
TEXT_W_CM, MAX_H_CM = 16.0, 21.0
TEXT_W_TW = 11906 - 2 * 1418
TOC_MARK = "@@ICINDEKILER@@"
TOC_LEVELS = 3


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


def fit(path, max_w=TEXT_W_CM, max_h=MAX_H_CM):
    """Görseli oranını koruyarak metin genişliğine ve sayfa yüksekliğine sığdıran genişlik (cm)."""
    w, h = png_size(path)
    return round(min(max_w, max_h * w / h), 2)


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
                if len(m.group(1)) == 2:
                    self.chapter = re.match(r"(\d+)\.", m.group(2)).group(1)
                out.append(m.group(1)[1:] + " " + m.group(2))
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
        parts = ["# Ek A. Ekran görüntüleri", "",
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
            height = min(8.0 if dark is None else 9.5, (TEXT_W_CM - 2.0) / sum(r for _, r in shots))
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
    text = text.replace("<!-- İÇİNDEKİLER -->", f"```{{=openxml}}\n<w:p><w:r><w:t>{TOC_MARK}</w:t></w:r></w:p>\n```")
    return re.sub(r"<!--.*?-->\s*", "", text, flags=re.S)


# ---------------------------------------------------------------------------------------------- Word stilleri

FONT = '<w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:eastAsia="Times New Roman" w:cs="Times New Roman"/>'
MONO = '<w:rFonts w:ascii="Courier New" w:hAnsi="Courier New" w:cs="Courier New"/>'


def pstyle(sid, name, ppr="", rpr="", based="Normal", nxt="BodyText", extra=""):
    return (f'<w:style w:type="paragraph" w:customStyle="1" w:styleId="{sid}"><w:name w:val="{name}"/>'
            f'<w:basedOn w:val="{based}"/><w:next w:val="{nxt}"/><w:qFormat/>{extra}'
            f'<w:pPr>{ppr}</w:pPr><w:rPr>{rpr}</w:rPr></w:style>')


def heading(level, size, before, after, page_break=False):
    return (f'<w:style w:type="paragraph" w:styleId="Heading{level}"><w:name w:val="heading {level}"/>'
            f'<w:basedOn w:val="Normal"/><w:next w:val="BodyText"/><w:uiPriority w:val="9"/><w:qFormat/>'
            f'<w:pPr><w:keepNext/><w:keepLines/>{"<w:pageBreakBefore/>" if page_break else ""}'
            f'<w:spacing w:before="{before}" w:after="{after}" w:line="276" w:lineRule="auto"/><w:jc w:val="left"/>'
            f'<w:outlineLvl w:val="{level - 1}"/></w:pPr>'
            f'<w:rPr>{FONT}<w:b/><w:bCs/><w:color w:val="000000"/><w:sz w:val="{size}"/><w:szCs w:val="{size}"/></w:rPr></w:style>')


def toc_style(level):
    return (f'<w:style w:type="paragraph" w:styleId="TOC{level}"><w:name w:val="toc {level}"/>'
            f'<w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:uiPriority w:val="39"/>'
            f'<w:pPr><w:tabs><w:tab w:val="right" w:leader="dot" w:pos="{TEXT_W_TW}"/></w:tabs>'
            f'<w:spacing w:before="{60 if level == 1 else 0}" w:after="40" w:line="264" w:lineRule="auto"/>'
            f'<w:ind w:left="{(level - 1) * 360}"/><w:jc w:val="left"/></w:pPr>'
            f'<w:rPr>{"<w:b/>" if level == 1 else ""}</w:rPr></w:style>')


STYLES = {
    "BodyText": pstyle("BodyText", "Body Text", '<w:spacing w:before="0" w:after="120" w:line="360" w:lineRule="auto"/><w:jc w:val="both"/>'),
    "FirstParagraph": pstyle("FirstParagraph", "First Paragraph", "", "", based="BodyText"),
    "Compact": pstyle("Compact", "Compact", '<w:spacing w:before="0" w:after="40" w:line="312" w:lineRule="auto"/>', "", based="BodyText"),
    "TabloMetni": pstyle("TabloMetni", "Tablo Metni", '<w:spacing w:before="20" w:after="20" w:line="240" w:lineRule="auto"/><w:jc w:val="left"/>',
                         '<w:sz w:val="19"/><w:szCs w:val="19"/>'),
    "Heading1": heading(1, 32, 0, 240, page_break=True),
    "Heading2": heading(2, 28, 360, 160),
    "Heading3": heading(3, 24, 280, 120),
    "Heading4": heading(4, 24, 240, 120),
    "SourceCode": pstyle("SourceCode", "Source Code", '<w:shd w:val="clear" w:color="auto" w:fill="F3F4F6"/><w:spacing w:before="0" w:after="120" w:line="240" w:lineRule="auto"/><w:jc w:val="left"/>',
                         f'{MONO}<w:sz w:val="18"/><w:szCs w:val="18"/>'),
    "Sekil": pstyle("Sekil", "Sekil", '<w:keepNext/><w:spacing w:before="160" w:after="60" w:line="240" w:lineRule="auto"/><w:jc w:val="center"/>'),
    "SekilAltyazisi": pstyle("SekilAltyazisi", "Sekil Altyazisi", '<w:spacing w:before="0" w:after="240" w:line="240" w:lineRule="auto"/><w:jc w:val="center"/>',
                             '<w:i/><w:sz w:val="20"/><w:szCs w:val="20"/>'),
    "KapakKurum": pstyle("KapakKurum", "Kapak Kurum", '<w:spacing w:before="0" w:after="60"/><w:jc w:val="center"/>', '<w:b/><w:sz w:val="28"/><w:szCs w:val="28"/>'),
    "KapakBaslik": pstyle("KapakBaslik", "Kapak Baslik", '<w:spacing w:before="2400" w:after="240"/><w:jc w:val="center"/>', '<w:b/><w:sz w:val="48"/><w:szCs w:val="48"/>'),
    "KapakAltBaslik": pstyle("KapakAltBaslik", "Kapak Alt Baslik", '<w:spacing w:before="0" w:after="1200"/><w:jc w:val="center"/>', '<w:sz w:val="32"/><w:szCs w:val="32"/>'),
    "KapakMetin": pstyle("KapakMetin", "Kapak Metin", '<w:spacing w:before="0" w:after="100" w:line="276" w:lineRule="auto"/><w:jc w:val="center"/>'),
    "TOCHeading": pstyle("TOCHeading", "TOC Heading", '<w:keepNext/><w:pageBreakBefore/><w:spacing w:before="0" w:after="240"/>',
                         f'{FONT}<w:b/><w:sz w:val="32"/><w:szCs w:val="32"/>', nxt="Normal"),
    "TOC1": toc_style(1), "TOC2": toc_style(2), "TOC3": toc_style(3),
    "YerTutucu": ('<w:style w:type="character" w:customStyle="1" w:styleId="YerTutucu"><w:name w:val="Yer Tutucu"/>'
                  '<w:rPr><w:highlight w:val="yellow"/></w:rPr></w:style>'),
    "Table": ('<w:style w:type="table" w:styleId="Table"><w:name w:val="Table"/><w:basedOn w:val="TableNormal"/><w:uiPriority w:val="99"/>'
              '<w:pPr><w:spacing w:before="20" w:after="20" w:line="240" w:lineRule="auto"/></w:pPr>'
              '<w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/><w:left w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/>'
              '<w:bottom w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/><w:right w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/>'
              '<w:insideH w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/><w:insideV w:val="single" w:sz="4" w:space="0" w:color="9CA3AF"/></w:tblBorders>'
              '<w:tblCellMar><w:top w:w="30" w:type="dxa"/><w:left w:w="80" w:type="dxa"/><w:bottom w:w="30" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr>'
              '<w:tblStylePr w:type="firstRow"><w:rPr><w:b/></w:rPr><w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="E5E7EB"/></w:tcPr></w:tblStylePr></w:style>'),
}

DOC_DEFAULTS = (f'<w:docDefaults><w:rPrDefault><w:rPr>{FONT}<w:sz w:val="24"/><w:szCs w:val="24"/><w:lang w:val="tr-TR" w:eastAsia="en-US" w:bidi="ar-SA"/>'
                '</w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="360" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>')

FOOTER = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
          '<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:jc w:val="center"/></w:pPr>'
          '<w:r><w:fldChar w:fldCharType="begin"/></w:r><w:r><w:instrText xml:space="preserve"> PAGE </w:instrText></w:r>'
          '<w:r><w:fldChar w:fldCharType="separate"/></w:r><w:r><w:t>1</w:t></w:r><w:r><w:fldChar w:fldCharType="end"/></w:r></w:p></w:ftr>')


def read_zip(path):
    with zipfile.ZipFile(path) as z:
        return {n: z.read(n) for n in z.namelist() if not n.endswith("/")}


def write_zip(path, files):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name in ["[Content_Types].xml"] + sorted(n for n in files if n != "[Content_Types].xml"):
            z.writestr(name, files[name])


def make_reference(build):
    """pandoc'un varsayılan reference.docx'ini rapor düzenine çeker: Times New Roman 12, 1,5 satır, iki yana yaslı,
    A4 ve 2,5 cm kenar, siyah başlıklar (bölüm başlığı yeni sayfada), çizgili tablolar, ortada sayfa numarası."""
    ref = build / "referans.docx"
    with open(ref, "wb") as f:
        f.write(subprocess.run(["pandoc", "--print-default-data-file", "reference.docx"], capture_output=True, check=True).stdout)
    files = read_zip(ref)
    styles = files["word/styles.xml"].decode("utf-8")
    styles = re.sub(r"<w:docDefaults>.*?</w:docDefaults>", DOC_DEFAULTS, styles, flags=re.S)
    for sid, xml in STYLES.items():
        styles = re.sub(rf'<w:style [^>]*w:styleId="{sid}"[^>]*>.*?</w:style>', "", styles, flags=re.S)
        styles = styles.replace("</w:styles>", xml + "</w:styles>")
    # Başlık renkleri ve karakter stillerinin yazı tipleri (pandoc varsayılanı mavi ve Cambria)
    styles = re.sub(r'(<w:style [^>]*w:styleId="(?:Title|Subtitle)"[^>]*>.*?)<w:color w:val="[0-9A-Fa-f]+"/>', r"\1", styles, flags=re.S)
    styles = re.sub(r'(<w:style [^>]*w:styleId="VerbatimChar"[^>]*>.*?<w:rPr>)', rf"\1{MONO}<w:sz w:val='20'/>".replace("'", '"'), styles, count=1, flags=re.S)
    files["word/styles.xml"] = styles.encode("utf-8")

    doc = files["word/document.xml"].decode("utf-8")
    sect = ('<w:sectPr><w:footerReference w:type="default" r:id="rIdRaporFooter"/><w:pgSz w:w="11906" w:h="16838"/>'
            '<w:pgMar w:top="1418" w:right="1418" w:bottom="1418" w:left="1418" w:header="709" w:footer="709" w:gutter="0"/>'
            '<w:titlePg/></w:sectPr>')
    doc = re.sub(r"<w:sectPr\b.*?</w:sectPr>", sect, doc, flags=re.S)
    if 'xmlns:r="' not in doc[:2000]:
        doc = doc.replace("<w:document ", '<w:document xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" ', 1)
    files["word/document.xml"] = doc.encode("utf-8")
    files["word/footer-rapor.xml"] = FOOTER.encode("utf-8")
    rels = files["word/_rels/document.xml.rels"].decode("utf-8").replace(
        "</Relationships>", '<Relationship Id="rIdRaporFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer-rapor.xml"/></Relationships>')
    files["word/_rels/document.xml.rels"] = rels.encode("utf-8")
    ct = files["[Content_Types].xml"].decode("utf-8").replace(
        "</Types>", '<Override PartName="/word/footer-rapor.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/></Types>')
    files["[Content_Types].xml"] = ct.encode("utf-8")
    write_zip(ref, files)
    return ref


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


def headings(md):
    out, in_code = [], False
    for line in md.split("\n"):
        if line.startswith("```"):
            in_code = not in_code
            continue
        m = None if in_code else re.match(r"^(#{1,%d}) (.*)$" % TOC_LEVELS, line)
        if m:
            text = re.sub(r"[`*]", "", re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", m.group(2))).strip()
            out.append((len(m.group(1)), text))
    return out


def toc_xml(entries, pages):
    paras = ['<w:p><w:pPr><w:pStyle w:val="TOCHeading"/></w:pPr><w:r><w:t>İçindekiler</w:t></w:r></w:p>']
    for k, (level, text) in enumerate(entries):
        begin = ('<w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
                 '<w:r><w:instrText xml:space="preserve"> TOC \\o "1-%d" \\h \\z \\u </w:instrText></w:r>'
                 '<w:r><w:fldChar w:fldCharType="separate"/></w:r>' % TOC_LEVELS) if k == 0 else ""
        end = '<w:r><w:fldChar w:fldCharType="end"/></w:r>' if k == len(entries) - 1 else ""
        page = str(pages[k]) if pages and pages[k] else "–"
        paras.append(f'<w:p><w:pPr><w:pStyle w:val="TOC{level}"/></w:pPr>{begin}'
                     f'<w:r><w:t xml:space="preserve">{escape(text)}</w:t></w:r><w:r><w:tab/></w:r><w:r><w:t>{page}</w:t></w:r>{end}</w:p>')
    return "".join(paras)


def fix_table(tbl):
    """Tablo hücreleri küçük, tek satır aralıklı ve sola yaslı; tam genişlikli tabloda sütunlar metin genişliğine ölçeklenir."""
    tbl = re.sub(r'<w:pStyle w:val="(?:Compact|BodyText|FirstParagraph)"\s*/>', '<w:pStyle w:val="TabloMetni"/>', tbl)
    if re.search(r'<w:tblW w:type="pct" w:w="5000"\s*/>', tbl):
        cols = [int(x) for x in re.findall(r'<w:gridCol w:w="(\d+)"\s*/>', tbl)]
        if cols and sum(cols):
            k = TEXT_W_TW / sum(cols)
            new = iter(round(c * k) for c in cols)
            tbl = re.sub(r'<w:gridCol w:w="\d+"\s*/>', lambda _: f'<w:gridCol w:w="{next(new)}"/>', tbl)
    return tbl


def postprocess(docx, entries, pages):
    files = read_zip(docx)
    doc = files["word/document.xml"].decode("utf-8")
    # yer tutucu: stilin yanında doğrudan sarı vurgu (şablonda stil başka tanımlıysa da görünsün)
    doc = re.sub(r'<w:rStyle w:val="YerTutucu"\s*/>', '<w:rStyle w:val="YerTutucu"/><w:highlight w:val="yellow"/>', doc)
    # tablo hücreleri: küçük, tek satır aralıklı metin
    doc = re.sub(r"<w:tbl>.*?</w:tbl>", lambda m: fix_table(m.group(0)), doc, flags=re.S)
    mark = re.search(r"<w:p>(?:(?!<w:p>).)*?%s.*?</w:p>" % TOC_MARK, doc, flags=re.S)
    if not mark:
        sys.exit("HATA: içindekiler işareti bulunamadı")
    doc = doc[:mark.start()] + toc_xml(entries, pages) + doc[mark.end():]
    files["word/document.xml"] = doc.encode("utf-8")
    settings = files["word/settings.xml"].decode("utf-8")
    if "updateFields" not in settings:  # Word açılışta içindekileri güncellemeyi önersin
        settings = settings.replace("</w:settings>", '<w:updateFields w:val="true"/></w:settings>')
    files["word/settings.xml"] = settings.encode("utf-8")
    write_zip(docx, files)


def to_pdf(docx, outdir):
    run(["soffice", "--headless", "--norestore", "--convert-to", "pdf", "--outdir", str(outdir), str(docx)])
    return outdir / (docx.stem + ".pdf")


def find_pages(pdf, entries):
    """Her başlığın PDF'teki sayfası: içindekiler sayfaları atlanır, başlıklar sırayla aranır."""
    pages = run(["pdftotext", "-layout", str(pdf), "-"]).split("\f")
    norm = lambda s: re.sub(r"\s+", " ", s).strip().lower()
    leader = re.compile(r"\.{5,}\s*(\d+|–)$")
    toc = {i for i, p in enumerate(pages) if sum(bool(leader.search(norm(l))) for l in p.split("\n")) >= 5}
    found, cur = [], 0
    for _, title in entries:
        key = norm(title)[:45]
        hit = next((p for p in range(cur, len(pages)) if p not in toc
                    and any(norm(l).startswith(key) for l in pages[p].split("\n"))), None)
        found.append(hit + 1 if hit is not None else None)
        cur = hit if hit is not None else cur
    return found


# ---------------------------------------------------------------------------------------------- ana akış

def main():
    ap = argparse.ArgumentParser(description="Proje raporunu Word'de birleştirir (#69).")
    ap.add_argument("--sablon", type=Path, help="hocanın Word şablonu (.docx/.dotx); stilleri kullanılır")
    ap.add_argument("--cikti", type=Path, default=CIKTI, help="çıktı yolu, uzantısız (varsayılan: rapor/Mini_Udemy_Proje_Raporu)")
    ap.add_argument("--pdf-yok", action="store_true", help="PDF üretme")
    args = ap.parse_args()
    for tool in ("pandoc", "ffmpeg", "git") + (() if args.pdf_yok else ("soffice", "pdftotext", "pdfinfo")):
        need(tool)

    with tempfile.TemporaryDirectory(prefix="rapor-") as tmp:
        build = Path(tmp)
        h = Hazirlik(build)
        parts = [front_md(RAPOR / "on-sayfalar.md")]
        parts += [h.chapter_md(RAPOR / name) for name in BOLUMLER]
        parts.append(front_md(RAPOR / "kaynaklar.md"))
        parts.append(h.appendix_md())
        md = "\n\n".join(parts)
        (build / "rapor.md").write_text(md, encoding="utf-8")
        (build / "yer-tutucu.lua").write_text(LUA, encoding="utf-8")

        if args.sablon:
            ref = build / "referans.docx"
            if args.sablon.suffix.lower() == ".dotx":  # şablonu docx olarak kullan
                files = read_zip(args.sablon)
                files["[Content_Types].xml"] = files["[Content_Types].xml"].replace(
                    b"template.main+xml", b"document.main+xml")
                write_zip(ref, files)
            else:
                shutil.copy(args.sablon, ref)
        else:
            ref = make_reference(build)

        docx = build / "rapor.docx"
        run(["pandoc", str(build / "rapor.md"), "-f", "markdown-implicit_figures+pipe_tables+fenced_divs+raw_attribute",
             "-t", "docx", "-o", str(docx), "--reference-doc", str(ref), "--lua-filter", str(build / "yer-tutucu.lua"),
             "--resource-path", f"{RAPOR}:{ROOT}", "--metadata", "lang=tr-TR", "--columns=10"])
        entries = [e for e in headings(md)]
        base = docx.read_bytes()

        pages = None
        if not args.pdf_yok:
            # 1. geçiş: sayfa numaraları "–" ile; PDF'ten başlıkların sayfaları okunur, 2. geçişte yazılır.
            for _ in range(3):
                docx.write_bytes(base)
                postprocess(docx, entries, pages)
                pdf = to_pdf(docx, build)
                new = find_pages(pdf, entries)
                if new == pages:
                    break
                pages = new
            docx.write_bytes(base)
            postprocess(docx, entries, pages)
            pdf = to_pdf(docx, build)
        else:
            postprocess(docx, entries, None)

        out = args.cikti
        out.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(docx, out.with_suffix(".docx"))
        page_count = None
        if not args.pdf_yok:
            shutil.copy(pdf, out.with_suffix(".pdf"))
            page_count = re.search(r"Pages:\s+(\d+)", run(["pdfinfo", str(pdf)])).group(1)

    missing = [t for (l, t), p in zip(entries, pages or []) if p is None]
    ph = placeholders(md)
    print(f"Çıktı: {out.with_suffix('.docx').relative_to(ROOT) if out.is_relative_to(ROOT) else out.with_suffix('.docx')}"
          + ("" if args.pdf_yok else f" ve .pdf ({page_count} sayfa)"))
    print(f"Şablon: {args.sablon or 'yok (varsayılan stil)'} · içindekiler: {len(entries)} başlık"
          + (f" · sayfası bulunamayan: {missing}" if missing else ""))
    print(f"Yer tutucular (⟨…⟩, sarı): {len(ph)}")
    for head, text in ph:
        print(f"  - {head}: {text}")


if __name__ == "__main__":
    main()
