"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { setReviewVisibility } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import type { ReviewResponse } from "@/lib/api/reviews";

/** Backend'deki ReviewVisibilityRequest ile aynı sınır; boş gerekçeyi sunucu da reddeder (`errors.reason`). */
export const HIDE_REASON_MAX = 1000;

/** Hata mesajı ya da geçerliyse null. Gerekçe kırpılmış haliyle gönderilir. */
export function validateHideReason(reason: string): string | null {
  const trimmed = reason.trim();
  if (trimmed.length === 0) return "Gizleme gerekçesi zorunludur.";
  if (trimmed.length > HIDE_REASON_MAX) return `Gerekçe en fazla ${HIDE_REASON_MAX} karakter olabilir.`;
  return null;
}

interface Props {
  review: ReviewResponse;
  courseTitle: string;
  onClose: () => void;
  onDone: (updated: ReviewResponse) => void;
  /** Yorum bu arada silinmişse (404) çağıranın listeyi tazelemesi için. */
  onStale: () => void;
}

/**
 * Yorum gizleme penceresi (#146): gerekçe zorunludur ve sunucuda saklanır (#142). Her yorum için yeniden kurulur
 * (çağıran `key` verir), böylece gerekçe ve hata durumu taşınmaz.
 */
export function HideReviewDialog({ review, courseTitle, onClose, onDone, onStale }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [reason, setReason] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);
  const [stale, setStale] = useState(false);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  function close() {
    if (!working) onClose();
  }

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (working) return;
    setError(null);
    const invalid = validateHideReason(reason);
    setReasonError(invalid);
    if (invalid) return;
    setWorking(true);
    try {
      onDone(await setReviewVisibility(review.id, true, reason.trim()));
    } catch (err) {
      const body = err instanceof ApiError ? err.body : undefined;
      if (body?.errors?.reason) setReasonError(body.errors.reason);
      else setError(body?.message ?? "İşlem tamamlanamadı. Lütfen tekrar deneyin.");
      if (err instanceof ApiError && err.status === 404) {
        setStale(true);
        onStale();
      }
    } finally {
      setWorking(false);
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="qb-dialog qb"
      aria-labelledby="hide-review-title"
      onClose={close}
      onCancel={(e) => {
        if (working) e.preventDefault();
      }}
    >
      <form onSubmit={(e) => void submit(e)} noValidate>
        <h2 id="hide-review-title">Yorumu gizle</h2>
        <p className="qb-dialog-text">
          {courseTitle} · {review.author.fullName} · {review.rating}/5
        </p>
        {review.comment && <p className="rvm-quote">{review.comment}</p>}
        <p>Yorum herkese açık listeden kalkar ve kursun ortalamasına katılmaz. Gerekçe yalnız yöneticilere görünür.</p>
        <label className="adm-reason">
          Gerekçe
          <textarea
            name="reason"
            rows={4}
            maxLength={HIDE_REASON_MAX}
            value={reason}
            onChange={(e) => {
              setReason(e.target.value);
              if (reasonError) setReasonError(null);
            }}
            aria-invalid={reasonError ? true : undefined}
            aria-describedby={reasonError ? "hide-review-reason-error" : undefined}
            disabled={working}
            autoFocus
          />
          {reasonError && (
            <span id="hide-review-reason-error" className="auth-error">
              {reasonError}
            </span>
          )}
        </label>
        {error && (
          <p className="auth-error" role="alert">
            {error}
          </p>
        )}
        <div className="qb-actions">
          <button type="submit" className="qb-btn qb-danger" disabled={working || stale}>
            {working ? "Kaydediliyor…" : "Gizle"}
          </button>
          <button type="button" className="qb-btn" onClick={close} disabled={working}>
            {error ? "Kapat" : "Vazgeç"}
          </button>
        </div>
      </form>
    </dialog>
  );
}
