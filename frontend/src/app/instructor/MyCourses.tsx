"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { API_BASE_URL, ApiError } from "@/lib/api/client";
import { LEVEL_LABELS, type CourseStatus } from "@/lib/api/catalog";
import { STATUS_LABELS, listMyCourses, submitCourse, type CourseResponse } from "@/lib/api/instructor";

const STATUSES = Object.keys(STATUS_LABELS) as CourseStatus[];
// Renk dışı ipucu: her durumun kendi simgesi var.
const STATUS_ICONS: Record<CourseStatus, string> = { DRAFT: "✎", IN_REVIEW: "⏳", PUBLISHED: "✓", UNPUBLISHED: "⊘" };

type Loaded = { key: string; result: { kind: "ok"; courses: CourseResponse[] } | { kind: "error"; message: string } };

function apiMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) return e.body?.message ?? fallback;
  return "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.";
}

/** Eğitmen paneli (#38): kurslarım, durum süzgeci (URL'de), onaya gönder. */
export function MyCourses() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const raw = searchParams.get("status");
  const status = raw && (STATUSES as string[]).includes(raw) ? (raw as CourseStatus) : undefined;

  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [overrides, setOverrides] = useState<Record<number, CourseResponse>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const requestKey = `${status ?? ""}#${retry}`;
  const result = loaded?.key === requestKey ? loaded.result : null;

  useEffect(() => {
    let cancelled = false;
    const key = `${status ?? ""}#${retry}`;
    listMyCourses(status)
      .then((courses) => {
        if (cancelled) return;
        setOverrides({});
        setLoaded({ key, result: { kind: "ok", courses } });
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoaded({ key, result: { kind: "error", message: apiMessage(e, "Kurslar yüklenemedi.") } });
      });
    return () => {
      cancelled = true;
    };
  }, [status, retry]);

  const [toSubmit, setToSubmit] = useState<CourseResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (toSubmit && !dialog.open) dialog.showModal();
    if (!toSubmit && dialog.open) dialog.close();
  }, [toSubmit]);

  function closeDialog() {
    if (submitting) return;
    setToSubmit(null);
    setSubmitError(null);
  }

  async function confirmSubmit() {
    if (!toSubmit) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const updated = await submitCourse(toSubmit.id);
      setOverrides((o) => ({ ...o, [updated.id]: updated }));
      setNotice(`“${updated.title}” incelemeye gönderildi.`);
      setToSubmit(null);
    } catch (e) {
      setSubmitError(apiMessage(e, "Kurs onaya gönderilemedi. Lütfen tekrar deneyin."));
    } finally {
      setSubmitting(false);
    }
  }

  function setFilter(value: string) {
    setNotice(null);
    router.push(value ? `${pathname}?status=${value}` : pathname, { scroll: false });
  }

  const courses = result?.kind === "ok" ? result.courses.map((c) => overrides[c.id] ?? c) : [];

  return (
    <section className="qb ic">
      <div className="qb-head">
        <h1>Kurslarım</h1>
      </div>

      <label className="ic-filter">
        Durum
        <select value={status ?? ""} onChange={(e) => setFilter(e.target.value)}>
          <option value="">Tümü</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </label>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      {!result && <p className="auth-status">Kurslar yükleniyor…</p>}

      {result?.kind === "error" && (
        <div className="qb-state" role="alert">
          <p className="auth-error">{result.message}</p>
          <button type="button" className="qb-btn" onClick={() => setRetry((n) => n + 1)}>
            Tekrar dene
          </button>
        </div>
      )}

      {result?.kind === "ok" && courses.length === 0 && (
        <div className="qb-state">
          <p>{status ? `“${STATUS_LABELS[status]}” durumunda kursun yok.` : "Henüz kursun yok."}</p>
          {status && (
            <button type="button" className="qb-btn" onClick={() => setFilter("")}>
              Süzgeci temizle
            </button>
          )}
        </div>
      )}

      {result?.kind === "ok" && courses.length > 0 && (
        <ul className="qb-list">
          {courses.map((c) => {
            const cover = c.coverUrl ? `${API_BASE_URL}${c.coverUrl}` : null;
            const canSubmit = c.status === "DRAFT" || c.status === "UNPUBLISHED";
            return (
              <li key={c.id} className="qb-item ic-item">
                <div
                  className="course-cover ic-cover"
                  role="img"
                  aria-label={`${c.title} kapak görseli`}
                  style={cover ? { backgroundImage: `url("${cover}")` } : undefined}
                >
                  {!cover && <span className="course-cover-placeholder">{c.title.charAt(0)}</span>}
                </div>
                <div className="ic-body">
                  <div className="ic-title-row">
                    <h2 className="ic-title">{c.title}</h2>
                    <span className={`ic-badge ic-${c.status.toLowerCase()}`}>
                      <span aria-hidden="true">{STATUS_ICONS[c.status]}</span> {STATUS_LABELS[c.status]}
                    </span>
                  </div>
                  <div className="course-tags">
                    <span className="tag">{c.category.name}</span>
                    <span className="tag">{LEVEL_LABELS[c.level]}</span>
                  </div>
                  {c.status === "DRAFT" && c.rejectionReason && (
                    <p className="ic-reason ic-reason-reject">
                      <strong>Reddedildi:</strong> {c.rejectionReason}
                    </p>
                  )}
                  {c.status === "UNPUBLISHED" && c.unpublishReason && (
                    <p className="ic-reason">
                      <strong>Yayından kaldırma gerekçesi:</strong> {c.unpublishReason}
                    </p>
                  )}
                  <div className="qb-actions">
                    {canSubmit && (
                      <button
                        type="button"
                        className="qb-btn qb-primary"
                        onClick={() => {
                          setNotice(null);
                          setSubmitError(null);
                          setToSubmit(c);
                        }}
                        aria-label={`${c.title} kursunu ${c.status === "DRAFT" ? "onaya gönder" : "yeniden incelemeye gönder"}`}
                      >
                        {c.status === "DRAFT" ? "Onaya gönder" : "Yeniden incelemeye gönder"}
                      </button>
                    )}
                    <Link className="qb-btn" href={`/instructor/courses/${c.id}/questions`}>
                      Soru bankası
                    </Link>
                    <Link className="qb-btn" href={`/instructor/courses/${c.id}/quizzes`}>
                      Sınavlar
                    </Link>
                    <Link className="qb-btn" href={`/instructor/courses/${c.id}/analytics`}>
                      Analitik
                    </Link>
                    <Link className="qb-btn" href={`/courses/${c.id}`}>
                      Kurs detayı
                    </Link>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <dialog
        ref={dialogRef}
        className="qb-dialog"
        onClose={closeDialog}
        onCancel={(e) => {
          if (submitting) e.preventDefault();
        }}
      >
        <h2>Kursu incelemeye gönder</h2>
        {toSubmit && <p className="qb-dialog-text">“{toSubmit.title}”</p>}
        <p>Kurs yönetici onayına gönderilecek. İnceleme sürerken içeriğini düzenleyemezsin.</p>
        {submitError && (
          <p className="auth-error" role="alert">
            {submitError}
          </p>
        )}
        <div className="qb-actions">
          <button type="button" className="qb-btn qb-primary" onClick={() => void confirmSubmit()} disabled={submitting}>
            {submitting ? "Gönderiliyor…" : "Evet, gönder"}
          </button>
          <button type="button" className="qb-btn" onClick={closeDialog} disabled={submitting}>
            {submitError ? "Kapat" : "Vazgeç"}
          </button>
        </div>
      </dialog>
    </section>
  );
}
