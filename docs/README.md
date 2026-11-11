# Dokümanlar

Kapsamın tek kaynağı hocanın proje tanım dokümanıdır (modüller A–L, senaryo 1–5, bölüm 5–7). Claude Code ile çalışma akışı: [claude-code-workflow.md](claude-code-workflow.md).

| Doküman | Dosya | Milestone |
|---|---|---|
| Fonksiyonel / fonksiyonel olmayan gereksinimler | `requirements.md` | Hafta 1 |
| Kullanım senaryoları + use-case diyagramı | `requirements.md`, `diagrams/use-case.*` | Hafta 1 |
| ER diyagramı | `diagrams/er.*` | Hafta 1 |
| API sözleşmesi | `api/openapi.yaml` | Hafta 1 |
| Mimari kurallar (10 modül, 5 desen) | `architecture.md` | Hafta 1 |
| Mimari karar kayıtları (ADR) | [`adr/`](adr/README.md) | Hafta 1 → |
| Gantt, görev dağılımı, risk analizi | `plan.md` | Hafta 1 |
| Haftalık ilerleme raporları | `progress/hafta-N.md` | her hafta |
| Sistem mimarisi (C4) | `diagrams/c4-*` | Hafta 2 |
| UI taslakları | `diagrams/wireframes/` | Hafta 2 |
| Sekans: kursa kayıt | `diagrams/seq-enroll.*` | Hafta 2 |
| Sınıf diyagramı | `diagrams/class.*` | Hafta 3 |
| Bileşen diyagramı | `diagrams/component.*` | Hafta 3 |
| Test planı | `test-plan.md` | Hafta 3 |
| Sekans: sınav çözme | `diagrams/seq-quiz.*` | Hafta 4 |
| Sekans: sertifika oluşturma ve bildirim | `diagrams/seq-certificate.*` | Hafta 4 |
| Dağıtım diyagramı | `diagrams/deployment.*` | Hafta 4 |
| SOLID ve OOP (koddan örneklerle) | `solid.md` | Hafta 4 |
| İzlenebilirlik matrisi, test raporu, kütüphaneler | `traceability.md`, `test-report.md`, `dependencies.md` | Hafta 5 |
| Demo senaryosu | `demo.md` | Hafta 5 |
| Proje raporu (Word şablonu) | `rapor/` | Sunum (teslim 02.12.2026, .docx) |

Diyagramlar metin tabanlı tutulur (PlantUML `.puml` ya da Mermaid `.mmd`) ve yanında render edilmiş `.png`/`.svg` bulunur; böylece PR'da diff okunabilir.
