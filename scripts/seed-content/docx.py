"""En küçük geçerli DOCX'i yalnız standart kütüphaneyle üretir (#67 seed içeriği).

Kullanım: python3 -I docx.py <çıktı.docx> <json>  — json: {"title", "summary", "items": [...], "footer"}
İçerik: başlık, açıklama paragrafı, numaralı görev listesi (her görevin altında cevap için boş satırlar).
"""
import json
import sys
import zipfile
from xml.sax.saxutils import escape

CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""


def run(text, size=22, bold=False, color=None):
    props = f'<w:sz w:val="{size}"/>' + ("<w:b/>" if bold else "") + (f'<w:color w:val="{color}"/>' if color else "")
    return f'<w:r><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>{props}</w:rPr><w:t xml:space="preserve">{escape(text)}</w:t></w:r>'


def para(*runs, after=120):
    return f'<w:p><w:pPr><w:spacing w:after="{after}"/></w:pPr>{"".join(runs)}</w:p>'


def main():
    out, spec = sys.argv[1], json.loads(sys.argv[2])
    body = [
        para(run("Mini Udemy · " + spec["course"], 18, color="4F46E5"), after=60),
        para(run(spec["title"], 40, bold=True), after=200),
        para(run(spec["summary"], 22), after=240),
    ]
    for i, item in enumerate(spec["items"], 1):
        body.append(para(run(f"{i}. ", 24, bold=True, color="4F46E5"), run(item, 24, bold=True), after=80))
        body.append(para(run("Cevabın: ______________________________________________", 20, color="6B7280"), after=240))
    body.append(para(run(spec.get("footer", ""), 18, color="6B7280"), after=0))
    document = ('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>'
                + "".join(body)
                + '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/></w:sectPr>'
                '</w:body></w:document>')
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", CONTENT_TYPES)
        z.writestr("_rels/.rels", RELS)
        z.writestr("word/document.xml", document)


if __name__ == "__main__":
    main()
