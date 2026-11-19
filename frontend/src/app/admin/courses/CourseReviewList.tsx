"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { listAdminCourses } from "@/lib/api/admin";
import type { CourseStatus, CourseSummaryPage } from "@/lib/api/catalog";
import { ApiError } from "@/lib/api/client";
import { STATUS_LABELS, type CourseResponse } from "@/lib/api/instructor";
import {
  CourseActionButtons,
  CourseActionDialog,
  actionDoneMessage,
  type CourseAction,
  type CourseActionTarget,
} from "./CourseActionDialog";

const PAGE_SIZE = 20;
const STATUSES = Object.keys(STATUS_LABELS) as CourseStatus[];
/** Varsayılan görünüm onay kuyruğudur; "Tümü" için `status=ALL`. */
const DEFAULT_STATUS: CourseStatus = "IN_REVIEW";
const ALL = "ALL";

type Result = { kind: "error"; message: string } | { kind: "ok"; data: CourseSummaryPage };
type Loaded = { key: string; result: Result };

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

// Filtrenin tek kaynağı URL'dir (kullanıcı yönetimiyle aynı kalıp).
function readQuery(params: URLSearchParams): { status: CourseStatus | undefined; page: number } {
  const raw = params.get("status");
  const page = Number(params.get("page") ?? "0");
  let status: CourseStatus | undefined = DEFAULT_STATUS;
  if (raw === ALL) status = undefined;
  else if (raw && (STATUSES as string[]).includes(raw)) status = raw as CourseStatus;
  return { status, page: Number.isInteger(page) && page > 0 ? page : 0 };
}

/**
 * Yönetici kurs listesi (#62): varsayılan olarak incelemedeki kurslar (onay kuyruğu, en eski güncellenen başta).
 * İncelemedeki kurs onaylanır ya da gerekçeyle reddedilir; yayımlanmış kurs gerekçeyle yayından kaldırılır.
 * Her işlemden sonra liste sunucudan yeniden yüklenir.
 */
export function CourseReviewList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryKey = searchParams.toString();
  const query = readQuery(new URLSearchParams(queryKey));

  const [reload, setReload] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [target, setTarget] = useState<CourseActionTarget | null>(null);

  // Son yanıt şimdiki filtreye ait değilse yükleniyor sayılır.
  const requestKey = `${queryKey}#${reload}`;
  const result: Result | null = loaded?.key === requestKey ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    const key = `${queryKey}#${reload}`;
    const q = readQuery(new URLSearchParams(queryKey));
    listAdminCourses({ ...q, size: PAGE_SIZE })
      .then((data) => {
        if (!cancelled) setLoaded({ key, result: { kind: "ok", data } });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        const message = errorMessage(e, "Kurslar yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.");
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
    router.push(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  function onDone(updated: CourseResponse, action: CourseAction) {
    setTarget(null);
    setNotice(`“${updated.title}”: ${actionDoneMessage(action)}`);
    setReload((n) => n + 1);
  }

  const data = result?.kind === "ok" ? result.data : null;
  // İşlemden sonra son sayfa boşaldıysa kullanıcı boş bir sayfada kalmasın.
  const emptyPage = data !== null && data.content.length === 0 && data.page > 0;

  return (
    <section className="qb usr">
      <div className="qb-head">
        <h1>Kurs onayı</h1>
        <Link href="/admin">Yönetici paneli</Link>
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      <div className="usr-filters">
        <label>
          Durum
          <select
            value={query.status ?? ALL}
            onChange={(e) => go({ status: e.target.value === DEFAULT_STATUS ? null : e.target.value })}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABELS[s]}
              </option>
            ))}
            <option value={ALL}>Tümü</option>
          </select>
        </label>
      </div>

      {result === null && <p className="auth-status">Kurslar yükleniyor…</p>}

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
              ? "Bu sayfada kurs kalmadı."
              : query.status === "IN_REVIEW"
                ? "İncelemeyi bekleyen kurs yok."
                : query.status
                  ? `“${STATUS_LABELS[query.status]}” durumunda kurs yok.`
                  : "Henüz kurs yok."}
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
          <p className="qb-state">{data.totalElements} kurs</p>
          <div className="usr-wrap">
            <table className="usr-table">
              <thead>
                <tr>
                  <th scope="col">Kurs</th>
                  <th scope="col">Eğitmen</th>
                  <th scope="col">Kategori</th>
                  <th scope="col">Ders</th>
                  <th scope="col">Durum</th>
                  <th scope="col">
                    <span className="visually-hidden">İşlem</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id}>
                    <td data-label="Kurs" className="usr-name">
                      <Link href={`/admin/courses/${c.id}`}>{c.title}</Link>
                    </td>
                    <td data-label="Eğitmen" className="usr-name">
                      {c.instructor.fullName}
                    </td>
                    <td data-label="Kategori">{c.category.name}</td>
                    <td data-label="Ders">{c.lessonCount}</td>
                    <td data-label="Durum">
                      <span className={`ic-badge ic-${c.status.toLowerCase()}`}>{STATUS_LABELS[c.status]}</span>
                    </td>
                    <td className="usr-act">
                      <div className="adm-actions">
                        <Link href={`/admin/courses/${c.id}`} className="qb-btn" aria-label={`${c.title}: İncele`}>
                          İncele
                        </Link>
                        <CourseActionButtons
                          course={c}
                          onSelect={(t) => {
                            setNotice(null);
                            setTarget(t);
                          }}
                        />
                      </div>
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
        <CourseActionDialog
          key={`${target.course.id}-${target.action}`}
          target={target}
          onClose={() => setTarget(null)}
          onDone={onDone}
          onStale={() => setReload((n) => n + 1)}
        />
      )}
    </section>
  );
}
