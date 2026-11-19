# Dokümanlar

Kapsamın tek kaynağı hocanın proje tanım dokümanıdır (modüller A–L, senaryo 1–5, bölüm 5–7). Claude Code ile çalışma akışı: [claude-code-workflow.md](claude-code-workflow.md).

| Doküman | Dosya | Milestone |
|---|---|---|
| Fonksiyonel / fonksiyonel olmayan gereksinimler | [`requirements.md`](requirements.md), [`non-functional-requirements.md`](non-functional-requirements.md) | Hafta 1 |
| Kullanım senaryoları + use-case diyagramı | [`use-cases.md`](use-cases.md), [`diagrams/use-case.puml`](diagrams/use-case.puml) ([SVG](diagrams/use-case.svg), [PNG](diagrams/use-case.png)) | Hafta 1 |
| ER diyagramı | `diagrams/er.*` | Hafta 1 |
| API sözleşmesi | `api/openapi.yaml` | Hafta 1 |
| Mimari kurallar (10 modül, 5 desen) | `architecture.md` | Hafta 1 |
| Mimari karar kayıtları (ADR) | [`adr/`](adr/README.md) | Hafta 1 → |
| Gantt, görev dağılımı, risk analizi | [`plan.md`](plan.md), [`diagrams/gantt.mmd`](diagrams/gantt.mmd) | Hafta 1 |
| Haftalık ilerleme raporları | `progress/hafta-N.md` | her hafta |
| Sistem mimarisi (C4) | bağlam: [`diagrams/c4-context.puml`](diagrams/c4-context.puml) ([SVG](diagrams/c4-context.svg), [PNG](diagrams/c4-context.png)); container: [`diagrams/c4-container.puml`](diagrams/c4-container.puml) ([SVG](diagrams/c4-container.svg), [PNG](diagrams/c4-container.png)) | Hafta 2 |
| UI taslakları | `diagrams/wireframes/` | Hafta 2 |
| Sekans: kursa kayıt | [`diagrams/seq-enroll.puml`](diagrams/seq-enroll.puml) ([SVG](diagrams/seq-enroll.svg), [PNG](diagrams/seq-enroll.png)) | Hafta 2 |
| Sınıf diyagramı | `diagrams/class.*` | Hafta 3 |
| Bileşen diyagramı | `diagrams/component.*` | Hafta 3 |
| Test planı | [`test-plan.md`](test-plan.md) | Hafta 3 |
| Sekans: sınav çözme | [`diagrams/seq-quiz.puml`](diagrams/seq-quiz.puml) ([SVG](diagrams/seq-quiz.svg), [PNG](diagrams/seq-quiz.png)) | Hafta 4 |
| Sekans: sertifika oluşturma ve bildirim | [`diagrams/seq-certificate.puml`](diagrams/seq-certificate.puml) ([SVG](diagrams/seq-certificate.svg), [PNG](diagrams/seq-certificate.png)) | Hafta 4 |
| Dağıtım diyagramı | [`diagrams/deployment.puml`](diagrams/deployment.puml) ([SVG](diagrams/deployment.svg), [PNG](diagrams/deployment.png)) | Hafta 4 |
| SOLID ve OOP (koddan örneklerle) | `solid.md` | Hafta 4 |
| İzlenebilirlik matrisi, test raporu, kütüphaneler | [`traceability.md`](traceability.md), `test-report.md`, `dependencies.md` | Hafta 5 |
| Demo senaryosu | [`demo.md`](demo.md) | Hafta 5 |
| Proje raporu (Word şablonu) | `rapor/` | Sunum (teslim 02.12.2026, .docx) |

Diyagramlar metin tabanlı tutulur (PlantUML `.puml` ya da Mermaid `.mmd`) ve yanında render edilmiş `.png`/`.svg` bulunur; böylece PR'da diff okunabilir.

PlantUML render'ı (Graphviz gerekmez; diyagramlar `!pragma layout smetana` kullanır). `plantuml.jar` Maven Central'dan (`net.sourceforge.plantuml:plantuml`) indirilir, repoya eklenmez. Türkçe yerel ayarlı Java'da C4 kütüphanesi yüklenemediği için dil İngilizce verilir:

```bash
java -Duser.language=en -Duser.country=US -jar plantuml.jar -tpng -tsvg docs/diagrams/c4-context.puml docs/diagrams/c4-container.puml docs/diagrams/deployment.puml
```
