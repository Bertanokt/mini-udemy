"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { listAdminCourses, listAdminReviews, setReviewVisibility, type ReviewPage } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import type { ReviewResponse } from "@/lib/api/reviews";
import { HideReviewDialog } from "./HideReviewDialog";

const PAGE_SIZE = 20;
/** Kurs filtresi ve kurs adları için; ince kapsamda tek sayfa yeterli, listede olmayan kurs "Kurs #id" görünür. */
const COURSE_LIMIT = 100;

type Result = { kind: "error"; message: string } | { kind: "ok"; data: ReviewPage };
type Loaded = { key: string; result: Result };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

// Filtrenin tek kaynağı URL'dir (kurs onayı ve kullanıcı yönetimiyle aynı kalıp).
function readQuery(params: URLSearchParams): { courseId?: number; hidden?: boolean; page: number } {
  const rawHidden = params.get("hidden");
  const courseId = Number(params.get("courseId"));
  const page = Number(params.get("page") ?? "0");
  return {
    courseId: Number.isInteger(courseId) && courseId > 0 ? courseId : undefined,
    hidden: rawHidden === "true" ? true : rawHidden === "false" ? false : undefined,
    page: Number.isInteger(page) && page > 0 ? page : 0,
  };
}

const dateFormat = new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium" });

/**
 * Yönetici yorum moderasyonu (#146 yorum kısmı, backend #142): gizliler dahil tüm yorumlar, en yeni önce. Yorum
 * gerekçeyle gizlenir ya da tek tıkla geri açılır; her işlemden sonra liste sunucudan yeniden yüklenir.
 */
export function ReviewModerationList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryKey = searchParams.toString();
  const query = readQuery(new URLSearchParams(queryKey));

  const [reload, setReload] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [courses, setCourses] = useState<Map<number, string>>(new Map());
  const [notice, setNotice] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [target, setTarget] = useState<ReviewResponse | null>(null);
  const [working, setWorking] = useState<number | null>(null);

  const requestKey = `${queryKey}#${reload}`;
  const result: Result | null = loaded?.key === requestKey ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    listAdminCourses({ size: COURSE_LIMIT })
      .then((page) => {
        if (!cancelled) setCourses(new Map(page.content.map((c) => [c.id, c.title])));
      })
      // Kurs adları yalnız kolaylık; gelmezse "Kurs #id" gösterilir.
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    const key = `${queryKey}#${reload}`;
    const q = readQuery(new URLSearchParams(queryKey));
    listAdminReviews({ ...q, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const message = errorMessage(e, "Yorumlar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.");
        setLoaded({ key, result: { kind: "error", message } });
      });
    return () => {
      cancelled = true;
    };
  }, [queryKey, reload]);

  function go(changes: Record<string, string | null>) {
    const next = new URLSearchParams(searchParams);
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    if (!("page" in changes)) next.delete("page");
    const qs = next.toString();
    setNotice(null);
    setActionError(null);
    router.push(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  function courseTitle(id: number): string {
    return courses.get(id) ?? `Kurs #${id}`;
  }

  function onHidden(updated: ReviewResponse) {
    setTarget(null);
    setNotice(`${updated.author.fullName} adlı kullanıcının yorumu gizlendi.`);
    setReload((n) => n + 1);
  }

  async function unhide(review: ReviewResponse) {
    if (working !== null) return;
    setNotice(null);
    setActionError(null);
    setWorking(review.id);
    try {
      const updated = await setReviewVisibility(review.id, false);
      setNotice(`${updated.author.fullName} adlı kullanıcının yorumu yeniden görünür.`);
    } catch (e) {
      setActionError(errorMessage(e, "İşlem tamamlanamadı. Lütfen tekrar deneyin."));
    } finally {
      setWorking(null);
      setReload((n) => n + 1);
    }
  }

  const data = result?.kind === "ok" ? result.data : null;
  const emptyPage = data !== null && data.content.length === 0 && data.page > 0;
  const courseOptions = [...courses.entries()].sort((a, b) => a[1].localeCompare(b[1], "tr"));

  return (
    <section className="qb usr">
      <div className="qb-head">
        <h1>Yorum moderasyonu</h1>
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

      <div className="usr-filters">
        <label>
          Durum
          <select
            value={query.hidden === undefined ? "" : String(query.hidden)}
            onChange={(e) => go({ hidden: e.target.value })}
          >
            <option value="">Tümü</option>
            <option value="false">Görünür</option>
            <option value="true">Gizli</option>
          </select>
        </label>
        <label>
          Kurs
          <select
            value={query.courseId === undefined ? "" : String(query.courseId)}
            onChange={(e) => go({ courseId: e.target.value })}
          >
            <option value="">Tüm kurslar</option>
            {query.courseId !== undefined && !courses.has(query.courseId) && (
              <option value={query.courseId}>{courseTitle(query.courseId)}</option>
            )}
            {courseOptions.map(([id, title]) => (
              <option key={id} value={id}>
                {title}
              </option>
            ))}
          </select>
        </label>
      </div>

      {result === null && <p className="auth-status">Yorumlar yükleniyor…</p>}

      {result?.kind === "error" && (
        <div className="qb-state" role="alert">
          <p className="auth-error">{result.message}</p>
          <button type="button" className="qb-btn" onClick={() => setReload((n) => n + 1)}>
            Tekrar dene
          </button>
        </div>
      )}

      {data && data.content.length === 0 && (
        <div className="qb-state">
          <p>
            {emptyPage
              ? "Bu sayfada yorum kalmadı."
              : query.hidden === true
                ? "Gizli yorum yok."
                : "Bu filtreyle yorum yok."}
          </p>
          {emptyPage && (
            <button type="button" className="qb-btn" onClick={() => go({ page: null })}>
              İlk sayfaya dön
            </button>
          )}
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <p className="qb-state">{data.totalElements} yorum</p>
          <div className="usr-wrap">
            <table className="usr-table">
              <thead>
                <tr>
                  <th scope="col">Kurs</th>
                  <th scope="col">Yazar</th>
                  <th scope="col">Puan</th>
                  <th scope="col">Yorum</th>
                  <th scope="col">Tarih</th>
                  <th scope="col">Durum</th>
                  <th scope="col">
                    <span className="visually-hidden">İşlem</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((r) => (
                  <tr key={r.id}>
                    <td data-label="Kurs" className="usr-name">
                      <Link href={`/admin/courses/${r.courseId}`}>{courseTitle(r.courseId)}</Link>
                    </td>
                    <td data-label="Yazar" className="usr-name">
                      {r.author.fullName}
                    </td>
                    <td data-label="Puan">{r.rating}/5</td>
                    <td data-label="Yorum" className="rvm-comment">
                      {r.comment ?? <span className="rvm-muted">Yorum yok</span>}
                      {r.hidden && r.moderationReason && (
                        <span className="ic-reason ic-reason-reject rvm-reason">Gerekçe: {r.moderationReason}</span>
                      )}
                    </td>
                    <td data-label="Tarih">{dateFormat.format(new Date(r.createdAt))}</td>
                    <td data-label="Durum" className={r.hidden ? "usr-off" : "usr-on"}>
                      {r.hidden ? "Gizli" : "Görünür"}
                    </td>
                    <td className="usr-act rvm-act">
                      {r.hidden ? (
                        <button
                          type="button"
                          className="qb-btn"
                          disabled={working !== null}
                          aria-label={`${r.author.fullName} yorumu: Geri aç`}
                          onClick={() => void unhide(r)}
                        >
                          {working === r.id ? "Kaydediliyor…" : "Geri aç"}
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="qb-btn qb-danger"
                          disabled={working !== null}
                          aria-label={`${r.author.fullName} yorumu: Gizle`}
                          onClick={() => {
                            setNotice(null);
                            setActionError(null);
                            setTarget(r);
                          }}
                        >
                          Gizle
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.totalPages > 1 && (
            <nav className="pagination" aria-label="Sayfalar">
              <button
                type="button"
                className="qb-btn"
                disabled={data.page <= 0}
                onClick={() => go({ page: String(data.page - 1) })}
              >
                ‹ Önceki
              </button>
              <span>
                Sayfa {data.page + 1} / {data.totalPages}
              </span>
              <button
                type="button"
                className="qb-btn"
                disabled={data.page + 1 >= data.totalPages}
                onClick={() => go({ page: String(data.page + 1) })}
              >
                Sonraki ›
              </button>
            </nav>
          )}
        </>
      )}

      {target && (
        <HideReviewDialog
          key={target.id}
          review={target}
          courseTitle={courseTitle(target.courseId)}
          onClose={() => setTarget(null)}
          onDone={onHidden}
          onStale={() => setReload((n) => n + 1)}
        />
      )}
    </section>
  );
}
