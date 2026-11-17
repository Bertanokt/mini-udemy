"use client";

import { use, useEffect, useState } from "react";
import { ApiError } from "@/lib/api/client";
import {
  getQuizForStudent,
  listMyQuizAttempts,
  type AttemptSummaryResponse,
} from "@/lib/api/quizzes";
import styles from "./attempts.module.css";

type Result =
  | { kind: "ok"; attempts: AttemptSummaryResponse[]; title: string | null; passThreshold: number | null }
  | { kind: "forbidden" }
  | { kind: "notFound" }
  | { kind: "error"; message: string };

type Loaded = { key: string; result: Result };

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("tr-TR", { dateStyle: "medium", timeStyle: "short" });
}

function formatScore(n: number): string {
  return n.toLocaleString("tr-TR", { maximumFractionDigits: 2 });
}

function formatPercent(n: number): string {
  return `%${n.toLocaleString("tr-TR", { maximumFractionDigits: 1 })}`;
}

/** En yeni önce sıralar; en iyi deneme en yüksek yüzdelidir, eşitlikte daha yeni olan. */
function prepare(list: AttemptSummaryResponse[]): { sorted: AttemptSummaryResponse[]; bestId: number | null } {
  const sorted = [...list].sort((a, b) => Date.parse(b.submittedAt) - Date.parse(a.submittedAt));
  let best: AttemptSummaryResponse | null = null;
  for (const a of sorted) {
    if (best === null || a.percent > best.percent) best = a;
  }
  return { sorted, bestId: best?.id ?? null };
}

/**
 * Sınav deneme geçmişi (#141). Sayfa RequireRole ile yalnız STUDENT'a açılır (quizzes/layout.tsx).
 * Başlık ve geçme eşiği `GET /api/quizzes/{id}`'den alınır; o istek başarısız olsa da geçmiş gösterilir.
 */
export function AttemptsView({ params }: { params: Promise<{ id: string }> }) {
  const { id: rawId } = use(params);
  const id = /^\d+$/.test(rawId) ? Number(rawId) : null;
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const key = `${rawId}#${retry}`;
  const result: Result | null = id === null ? { kind: "notFound" } : loaded?.key === key ? loaded.result : null;

  useEffect(() => {
    if (id === null) return;
    let cancelled = false;
    Promise.all([
      listMyQuizAttempts(id),
      getQuizForStudent(id).then(
        (q) => q,
        () => null,
      ),
    ])
      .then(([attempts, quiz]) => {
        if (cancelled) return;
        setLoaded({
          key,
          result: {
            kind: "ok",
            attempts,
            title: quiz?.title ?? null,
            passThreshold: quiz?.passThreshold ?? null,
          },
        });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 403) {
          setLoaded({ key, result: { kind: "forbidden" } });
        } else if (err instanceof ApiError && err.status === 404) {
          setLoaded({ key, result: { kind: "notFound" } });
        } else {
          const message =
            err instanceof ApiError && err.body?.message
              ? err.body.message
              : "Denemeler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.";
          setLoaded({ key, result: { kind: "error", message } });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [id, key]);

  return (
    <section>
      <h1>{result?.kind === "ok" && result.title ? result.title : "Sınav denemelerim"}</h1>
      {result === null && <p className={styles.status}>Denemeler yükleniyor…</p>}
      {result?.kind === "forbidden" && (
        <p role="alert" className={styles.error}>
          Bu sınavı görmek için kursa kayıtlı olmalısın.
        </p>
      )}
      {result?.kind === "notFound" && (
        <p role="alert" className={styles.error}>
          Sınav bulunamadı.
        </p>
      )}
      {result?.kind === "error" && (
        <div role="alert">
          <p className={styles.error}>{result.message}</p>
          <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
            Tekrar dene
          </button>
        </div>
      )}
      {result?.kind === "ok" && <History result={result} />}
    </section>
  );
}

function History({ result }: { result: Extract<Result, { kind: "ok" }> }) {
  const { sorted, bestId } = prepare(result.attempts);
  const best = sorted.find((a) => a.id === bestId) ?? null;
  const threshold = result.passThreshold;
  return (
    <>
      {threshold !== null && <p className={styles.meta}>Geçme eşiği: %{threshold}</p>}
      {sorted.length === 0 ? (
        <p className={styles.empty}>Henüz denemen yok.</p>
      ) : (
        <>
          <div className={styles.summary} role="group" aria-label="Özet">
            <p>Toplam deneme: {sorted.length}</p>
            <p>En yüksek: {best ? formatPercent(best.percent) : "-"}</p>
            <p>
              Durum:{" "}
              {sorted.some((a) => a.passed) ? (
                <span className={styles.pass}>✓ Geçildi</span>
              ) : (
                <span className={styles.fail}>✗ Henüz geçilmedi</span>
              )}
            </p>
          </div>
          <div className={styles.tableWrap}>
            <table className={styles.table} aria-label="Deneme geçmişi, en yeni önce">
              <thead>
                <tr>
                  <th scope="col">Tarih</th>
                  <th scope="col">Puan</th>
                  <th scope="col">Yüzde</th>
                  <th scope="col">Sonuç</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((a) => (
                  <tr key={a.id} className={a.id === bestId ? styles.best : undefined}>
                    <td>
                      {formatDateTime(a.submittedAt)}
                      {a.id === bestId && <span className={styles.bestTag}>★ En iyi deneme</span>}
                    </td>
                    <td>
                      {formatScore(a.score)} / {formatScore(a.maxScore)}
                    </td>
                    <td>{formatPercent(a.percent)}</td>
                    <td>
                      {a.passed ? (
                        <span className={styles.pass}>✓ Geçti</span>
                      ) : (
                        <span className={styles.fail}>✗ Kaldı</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  );
}
