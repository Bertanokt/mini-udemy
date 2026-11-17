"use client";

import { useEffect, useId, useState } from "react";
import Link from "next/link";
import { ApiError } from "@/lib/api/client";
import {
  createReview,
  listCourseReviews,
  updateReview,
  type CourseReviewsResponse,
  type RatingSummary,
  type ReviewResponse,
} from "@/lib/api/reviews";
import { loginHref } from "@/lib/auth/routes";
import styles from "./reviews.module.css";

const MAX_COMMENT = 2000;

export type ReviewerState = "loading" | "anonymous" | "not-student" | "unknown" | "not-enrolled" | "enrolled";

type Loaded =
  | { key: string; kind: "ok"; reviews: ReviewResponse[]; summary: RatingSummary; page: number; totalPages: number }
  | { key: string; kind: "error"; message: string };

function Stars({ value }: { value: number }) {
  return (
    <span className={styles.stars} role="img" aria-label={`5 üzerinden ${value} yıldız`}>
      {"★".repeat(value)}
      {"☆".repeat(5 - value)}
    </span>
  );
}

function errorMessage(e: unknown, fallback: string): string {
  return e instanceof ApiError && e.body?.message ? e.body.message : fallback;
}

function Summary({ summary }: { summary: RatingSummary }) {
  const max = Math.max(1, ...[1, 2, 3, 4, 5].map((n) => summary.distribution[String(n)] ?? 0));
  return (
    <div className={styles.summary}>
      <div className={styles.average}>
        <strong>{summary.averageRating != null ? summary.averageRating.toFixed(1) : "–"}</strong>
        <Stars value={summary.averageRating != null ? Math.round(summary.averageRating) : 0} />
        <div className={styles.hint}>{summary.reviewCount} değerlendirme</div>
      </div>
      <ul className={styles.bars} aria-label="Puan dağılımı">
        {[5, 4, 3, 2, 1].map((n) => {
          const count = summary.distribution[String(n)] ?? 0;
          return (
            <li key={n}>
              <span>{n} ★</span>
              <span className={styles.track} aria-hidden="true">
                <span className={styles.fill} style={{ width: `${(count / max) * 100}%` }} />
              </span>
              <span className={styles.count} aria-label={`${n} yıldız: ${count} değerlendirme`}>
                {count}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function ReviewForm({
  initial,
  onSubmit,
  onCancel,
}: {
  initial: ReviewResponse | null;
  onSubmit: (rating: number, comment: string) => Promise<{ rating?: string; comment?: string; form?: string } | null>;
  onCancel?: () => void;
}) {
  const groupName = useId();
  const [rating, setRating] = useState(initial?.rating ?? 0);
  const [comment, setComment] = useState(initial?.comment ?? "");
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<{ rating?: string; comment?: string; form?: string }>({});

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const local: typeof errors = {};
    if (rating < 1) local.rating = "Lütfen 1 ile 5 arasında bir puan seçin.";
    if (comment.length > MAX_COMMENT) local.comment = `Yorum en fazla ${MAX_COMMENT} karakter olabilir.`;
    if (local.rating || local.comment) {
      setErrors(local);
      return;
    }
    setBusy(true);
    setErrors({});
    const result = await onSubmit(rating, comment.trim());
    if (result) setErrors(result);
    setBusy(false);
  }

  return (
    <form className={styles.form} onSubmit={submit} noValidate>
      <h3>{initial ? "Yorumunu düzenle" : "Kursu değerlendir"}</h3>
      <fieldset className={styles.picker} aria-describedby={errors.rating ? `${groupName}-rating-err` : undefined}>
        <legend>Puanınız</legend>
        <div className={styles["picker-row"]}>
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n} className={`${styles.star} ${n <= rating ? styles.on : ""}`}>
              <input
                type="radio"
                name={groupName}
                value={n}
                checked={rating === n}
                onChange={() => setRating(n)}
                aria-label={`${n} yıldız`}
              />
              <span aria-hidden="true">★</span>
            </label>
          ))}
        </div>
        {errors.rating && (
          <p id={`${groupName}-rating-err`} className={styles.error} role="alert">
            {errors.rating}
          </p>
        )}
      </fieldset>
      <div>
        <label htmlFor={`${groupName}-comment`}>Yorumunuz (isteğe bağlı)</label>
        <textarea
          id={`${groupName}-comment`}
          className={styles.textarea}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          aria-invalid={errors.comment ? true : undefined}
          aria-describedby={`${groupName}-comment-hint`}
        />
        <div id={`${groupName}-comment-hint`} className={comment.length > MAX_COMMENT ? styles.error : styles.hint}>
          {comment.length} / {MAX_COMMENT}
        </div>
        {errors.comment && (
          <p className={styles.error} role="alert">
            {errors.comment}
          </p>
        )}
      </div>
      {errors.form && (
        <p className={styles.error} role="alert">
          {errors.form}
        </p>
      )}
      <div className={styles.actions}>
        <button type="submit" className={styles.button} disabled={busy}>
          {busy ? "Kaydediliyor…" : initial ? "Güncelle" : "Gönder"}
        </button>
        {onCancel && (
          <button type="button" className={`${styles.button} ${styles.secondary}`} disabled={busy} onClick={onCancel}>
            Vazgeç
          </button>
        )}
      </div>
    </form>
  );
}

/**
 * Kurs detayındaki değerlendirmeler (#127): özet + dağılım, sayfalı liste, kayıtlı kursiyer için puan/yorum formu.
 * Kullanıcının kendi yorumu listede `author.id` ile bulunur; ikinci gönderimde 409 gelirse kalan sayfalar taranıp
 * yorum düzenleme moduna alınır.
 */
export function CourseReviews({
  courseId,
  reviewer,
  userId,
}: {
  courseId: number;
  reviewer: ReviewerState;
  userId: number | null;
}) {
  const [reload, setReload] = useState(0);
  const key = `${courseId}#${reload}`;
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [mine, setMine] = useState<ReviewResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [moreBusy, setMoreBusy] = useState(false);
  const [moreError, setMoreError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listCourseReviews(courseId)
      .then((r: CourseReviewsResponse) => {
        if (!cancelled)
          setLoaded({ key, kind: "ok", reviews: r.content, summary: r.summary, page: r.page, totalPages: r.totalPages });
      })
      .catch((e: unknown) => {
        if (!cancelled)
          setLoaded({ key, kind: "error", message: errorMessage(e, "Değerlendirmeler yüklenemedi. Bağlantınızı kontrol edin.") });
      });
    return () => {
      cancelled = true;
    };
  }, [courseId, key]);

  const current = loaded?.key === key ? loaded : null;
  const ownInList =
    userId !== null && current?.kind === "ok" ? current.reviews.find((r) => r.author.id === userId) : undefined;
  const own = ownInList ?? (mine && mine.author.id === userId ? mine : null);

  async function loadMore() {
    if (!current || current.kind !== "ok") return;
    setMoreBusy(true);
    setMoreError(null);
    try {
      const r = await listCourseReviews(courseId, current.page + 1);
      setLoaded((prev) => {
        if (!prev || prev.kind !== "ok" || prev.key !== key) return prev;
        const seen = new Set(prev.reviews.map((x) => x.id));
        return {
          ...prev,
          reviews: [...prev.reviews, ...r.content.filter((x) => !seen.has(x.id))],
          summary: r.summary,
          page: r.page,
          totalPages: r.totalPages,
        };
      });
    } catch (e) {
      setMoreError(errorMessage(e, "Yorumlar yüklenemedi."));
    } finally {
      setMoreBusy(false);
    }
  }

  /** 409 sonrası: kullanıcının yorumunu kalan sayfalarda ara. */
  async function findOwn(): Promise<ReviewResponse | null> {
    let page = 0;
    let totalPages = 1;
    while (page < totalPages) {
      const r = await listCourseReviews(courseId, page);
      const found = r.content.find((x) => x.author.id === userId);
      if (found) return found;
      totalPages = r.totalPages;
      page += 1;
    }
    return null;
  }

  async function submit(rating: number, comment: string) {
    const body = comment === "" ? { rating } : { rating, comment };
    try {
      const saved = own ? await updateReview(own.id, body) : await createReview(courseId, body);
      setMine(saved);
      setEditing(false);
      setNotice(null);
      setReload((n) => n + 1);
      return null;
    } catch (e) {
      if (e instanceof ApiError && e.status === 409 && !own) {
        try {
          const found = await findOwn();
          if (found) setMine(found);
          setNotice("Bu kursu zaten değerlendirdiniz. Aşağıdan yorumunuzu düzenleyebilirsiniz.");
          setEditing(found !== null);
          setReload((n) => n + 1);
          return null;
        } catch {
          return { form: "Bu kursu zaten değerlendirdiniz; yorumunuz şu an alınamadı." };
        }
      }
      const fieldErrors = e instanceof ApiError ? e.body?.errors : undefined;
      if (fieldErrors && (fieldErrors.rating || fieldErrors.comment)) {
        return { rating: fieldErrors.rating, comment: fieldErrors.comment };
      }
      return { form: errorMessage(e, "Değerlendirme kaydedilemedi. Bağlantınızı kontrol edip tekrar deneyin.") };
    }
  }

  let formArea: React.ReactNode = null;
  if (reviewer === "anonymous") {
    formArea = (
      <p className={styles.notice}>
        Kursu değerlendirmek için{" "}
        <Link href={loginHref(`/courses/${courseId}`)} className="auth-switch">
          giriş yapın
        </Link>
        .
      </p>
    );
  } else if (reviewer === "not-student") {
    formArea = <p className={styles.notice}>Yalnızca kursa kayıtlı kursiyerler değerlendirme yapabilir.</p>;
  } else if (reviewer === "not-enrolled") {
    formArea = <p className={styles.notice}>Bu kursu değerlendirmek için önce kursa kayıt olmalısınız.</p>;
  } else if (reviewer === "unknown") {
    formArea = <p className={styles.notice}>Kayıt durumunuz alınamadığı için değerlendirme formu gösterilemiyor.</p>;
  } else if (reviewer === "enrolled") {
    if (own && !editing) {
      formArea = (
        <div className={styles.notice}>
          <p>
            Yorumunuz: <Stars value={own.rating} />
          </p>
          {own.comment && <p className={styles.comment}>{own.comment}</p>}
          <div className={styles.actions}>
            <button type="button" className={`${styles.button} ${styles.secondary}`} onClick={() => setEditing(true)}>
              Yorumumu düzenle
            </button>
          </div>
        </div>
      );
    } else if (current?.kind === "ok" || own) {
      formArea = (
        <ReviewForm
          key={own ? `edit-${own.id}-${own.updatedAt}` : "new"}
          initial={own}
          onSubmit={submit}
          onCancel={own ? () => setEditing(false) : undefined}
        />
      );
    }
  }

  let listArea: React.ReactNode;
  if (!current) {
    listArea = <p className={styles.status}>Değerlendirmeler yükleniyor…</p>;
  } else if (current.kind === "error") {
    listArea = (
      <div role="alert">
        <p className={styles.error}>{current.message}</p>
        <button type="button" className={styles.button} onClick={() => setReload((n) => n + 1)}>
          Tekrar dene
        </button>
      </div>
    );
  } else {
    listArea = (
      <>
        {current.summary.reviewCount === 0 && current.reviews.length === 0 ? (
          <p className={styles.status}>Bu kurs için henüz değerlendirme yok.</p>
        ) : (
          <>
            <Summary summary={current.summary} />
            <ul className={styles.list}>
              {current.reviews.map((r) => (
                <li key={r.id} className={styles.item}>
                  <header>
                    <strong>{r.author.fullName}</strong>
                    {r.author.id === userId && <span className={styles.hint}>(siz)</span>}
                    <Stars value={r.rating} />
                    <time dateTime={r.createdAt}>{new Date(r.createdAt).toLocaleDateString("tr-TR")}</time>
                  </header>
                  {r.comment && <p className={styles.comment}>{r.comment}</p>}
                </li>
              ))}
            </ul>
            {current.page + 1 < current.totalPages && (
              <div className={styles.more}>
                <button type="button" className={`${styles.button} ${styles.secondary}`} disabled={moreBusy} onClick={loadMore}>
                  {moreBusy ? "Yükleniyor…" : "Daha fazla yorum"}
                </button>
                {moreError && (
                  <p className={styles.error} role="alert">
                    {moreError}
                  </p>
                )}
              </div>
            )}
          </>
        )}
      </>
    );
  }

  return (
    <div>
      {notice && (
        <p className={styles.notice} role="status">
          {notice}
        </p>
      )}
      {formArea}
      {listArea}
    </div>
  );
}
