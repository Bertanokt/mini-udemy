"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  listComplaints,
  setComplaintStatus,
  type ComplaintPage,
  type ComplaintResponse,
  type ComplaintStatus,
} from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

const PAGE_SIZE = 20;

const STATUS_LABELS: Record<ComplaintStatus, string> = {
  OPEN: "Açık",
  REVIEWED: "İncelendi",
  REJECTED: "Reddedildi",
};

type Result = { kind: "error"; message: string } | { kind: "ok"; data: ComplaintPage };
type Loaded = { key: string; result: Result };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

function readPage(params: URLSearchParams): number {
  const page = Number(params.get("page") ?? "0");
  return Number.isInteger(page) && page > 0 ? page : 0;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString("tr-TR", { dateStyle: "short", timeStyle: "short" });
}

/**
 * Yönetici şikâyet ekranı (#146, İnce kapsam): en yeni önce sayfalı tablo, satırda "İncelendi" / "Reddet".
 * Filtre yok. Yorumu gizlemek ya da kursu yayından kaldırmak kendi ekranlarından yapılır (#142, #62).
 */
export function ComplaintList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryKey = searchParams.toString();

  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [workingId, setWorkingId] = useState<number | null>(null);

  // Son yanıt şimdiki sayfaya ait değilse yükleniyor sayılır.
  const requestKey = `${queryKey}#${retry}`;
  const result: Result | null = loaded?.key === requestKey ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    const key = `${queryKey}#${retry}`;
    listComplaints(readPage(new URLSearchParams(queryKey)), PAGE_SIZE)
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const message = errorMessage(e, "Şikâyetler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.");
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [queryKey, retry]);

  function goToPage(page: number) {
    setNotice(null);
    setActionError(null);
    router.push(page > 0 ? `${pathname}?page=${page}` : pathname, { scroll: false });
  }

  async function changeStatus(complaint: ComplaintResponse, status: Exclude<ComplaintStatus, "OPEN">) {
    setWorkingId(complaint.id);
    setNotice(null);
    setActionError(null);
    try {
      const updated = await setComplaintStatus(complaint.id, status);
      setLoaded((cur) =>
        cur && cur.result.kind === "ok"
          ? {
              ...cur,
              result: {
                kind: "ok",
                data: {
                  ...cur.result.data,
                  content: cur.result.data.content.map((c) => (c.id === updated.id ? updated : c)),
                },
              },
            }
          : cur,
      );
      setNotice(`Şikâyet #${updated.id}: ${STATUS_LABELS[updated.status]}.`);
    } catch (e) {
      setActionError(errorMessage(e, "Durum değiştirilemedi. Lütfen tekrar deneyin."));
    } finally {
      setWorkingId(null);
    }
  }

  return (
    <section className="qb usr">
      <div className="qb-head">
        <h1>Şikâyetler</h1>
        <Link href="/admin">Yönetici paneli</Link>
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}
      {actionError && (
        <p className="auth-error" role="alert">
          {actionError}
        </p>
      )}

      {result === null && <Loading text="Şikâyetler yükleniyor…" />}

      {result?.kind === "error" && <ErrorMessage message={result.message} onRetry={() => setRetry((n) => n + 1)} />}

      {result?.kind === "ok" && result.data.content.length === 0 && (
        <EmptyState message="Henüz şikâyet yok." />
      )}

      {result?.kind === "ok" && result.data.content.length > 0 && (
        <>
          <p className="qb-state">{result.data.totalElements} şikâyet</p>
          <div className="usr-wrap">
            <table className="usr-table">
              <thead>
                <tr>
                  <th scope="col">Hedef</th>
                  <th scope="col">Gerekçe</th>
                  <th scope="col">Şikâyet eden</th>
                  <th scope="col">Tarih</th>
                  <th scope="col">Durum</th>
                  <th scope="col">
                    <span className="visually-hidden">İşlem</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {result.data.content.map((c) => {
                  const busy = workingId === c.id;
                  return (
                    <tr key={c.id}>
                      <td data-label="Hedef">
                        {c.courseId !== null ? (
                          <Link href={`/admin/courses/${c.courseId}`}>Kurs #{c.courseId}</Link>
                        ) : (
                          `Yorum #${c.reviewId}`
                        )}
                      </td>
                      <td data-label="Gerekçe" className="cmp-reason">
                        {c.reason}
                      </td>
                      <td data-label="Şikâyet eden" className="usr-name">
                        {c.reporter.fullName}
                      </td>
                      <td data-label="Tarih">{formatDate(c.createdAt)}</td>
                      <td data-label="Durum" className={`cmp-${c.status.toLowerCase()}`}>
                        {STATUS_LABELS[c.status]}
                      </td>
                      <td>
                        <div className="adm-actions">
                          <button
                            type="button"
                            className="qb-btn qb-primary"
                            disabled={busy || c.status === "REVIEWED"}
                            aria-label={`Şikâyet #${c.id}: incelendi olarak işaretle`}
                            onClick={() => void changeStatus(c, "REVIEWED")}
                          >
                            İncelendi
                          </button>
                          <button
                            type="button"
                            className="qb-btn qb-danger"
                            disabled={busy || c.status === "REJECTED"}
                            aria-label={`Şikâyet #${c.id}: reddet`}
                            onClick={() => void changeStatus(c, "REJECTED")}
                          >
                            Reddet
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {result.data.totalPages > 1 && (
            <nav className="pagination" aria-label="Sayfalar">
              <button
                type="button"
                className="qb-btn"
                disabled={result.data.page <= 0}
                onClick={() => goToPage(result.data.page - 1)}
              >
                ‹ Önceki
              </button>
              <span>
                Sayfa {result.data.page + 1} / {result.data.totalPages}
              </span>
              <button
                type="button"
                className="qb-btn"
                disabled={result.data.page + 1 >= result.data.totalPages}
                onClick={() => goToPage(result.data.page + 1)}
              >
                Sonraki ›
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
