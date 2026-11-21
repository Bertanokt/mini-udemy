"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { approveCourse, rejectCourse, unpublishCourse } from "@/lib/api/admin";
import type { CourseStatus } from "@/lib/api/catalog";
import { ApiError } from "@/lib/api/client";
import type { CourseResponse } from "@/lib/api/instructor";
import { REASON_MAX, validateReason } from "./reasonValidation";

export type CourseAction = "approve" | "reject" | "unpublish";

export interface CourseActionTarget {
  course: { id: number; title: string };
  action: CourseAction;
}

const TEXT: Record<CourseAction, { title: string; body: string; confirm: string; done: string }> = {
  approve: {
    title: "Kursu onayla",
    body: "Kurs yayımlanır ve katalogda görünür. Eğitmene bildirim gider.",
    confirm: "Evet, onayla",
    done: "Kurs onaylandı ve yayımlandı.",
  },
  reject: {
    title: "Kursu reddet",
    body: "Kurs Taslak'a döner. Gerekçe eğitmenin panelinde görünür.",
    confirm: "Reddet",
    done: "Kurs reddedildi ve Taslak'a döndü.",
  },
  unpublish: {
    title: "Yayından kaldır",
    body: "Kurs katalogdan çıkar ve yeni kayıt almaz; kayıtlı kursiyerler derslerine devam eder. Gerekçe eğitmenin panelinde görünür.",
    confirm: "Yayından kaldır",
    done: "Kurs yayından kaldırıldı.",
  },
};

/** İşlem başarıyla bittiğinde gösterilecek kısa mesaj. */
export function actionDoneMessage(action: CourseAction): string {
  return TEXT[action].done;
}

interface Props {
  target: CourseActionTarget;
  onClose: () => void;
  onDone: (updated: CourseResponse, action: CourseAction) => void;
  /** Kurs bu arada başka duruma geçmiş ya da silinmişse (409/404) çağıranın veriyi tazelemesi için. */
  onStale: () => void;
}

/**
 * Yönetici kurs işlemi penceresi (#62): onay, gerekçeli red ve gerekçeli yayından kaldırma. Hangi durumda hangi
 * geçişin geçerli olduğuna backend'deki CourseState karar verir; burada yalnız sonucu (409 dahil) gösterilir.
 * Her hedef için yeniden kurulur (çağıran `key` verir), böylece gerekçe ve hata durumu taşınmaz.
 */
export function CourseActionDialog({ target, onClose, onDone, onStale }: Props) {
  const { course, action } = target;
  const needsReason = action !== "approve";
  const text = TEXT[action];
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [reason, setReason] = useState("");
  const [reasonError, setReasonError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);
  // 409/404: kurs bu arada başka duruma geçmiş ya da silinmiş; aynı işlemi yinelemek anlamsız.
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
    if (needsReason) {
      const invalid = validateReason(reason);
      setReasonError(invalid);
      if (invalid) return;
    }
    setWorking(true);
    try {
      const trimmed = reason.trim();
      const updated =
        action === "approve"
          ? await approveCourse(course.id)
          : action === "reject"
            ? await rejectCourse(course.id, trimmed)
            : await unpublishCourse(course.id, trimmed);
      onDone(updated, action);
    } catch (err) {
      const body = err instanceof ApiError ? err.body : undefined;
      if (body?.errors?.reason) setReasonError(body.errors.reason);
      else setError(body?.message ?? "İşlem tamamlanamadı. Lütfen tekrar deneyin.");
      if (err instanceof ApiError && (err.status === 409 || err.status === 404)) {
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
      aria-labelledby="course-action-title"
      onClose={close}
      onCancel={(e) => {
        if (working) e.preventDefault();
      }}
    >
      <form onSubmit={(e) => void submit(e)} noValidate>
        <h2 id="course-action-title">{text.title}</h2>
        <p className="qb-dialog-text">{course.title}</p>
        <p>{text.body}</p>
        {needsReason && (
          <label className="adm-reason">
            Gerekçe
            <textarea
              name="reason"
              rows={4}
              maxLength={REASON_MAX}
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (reasonError) setReasonError(null);
              }}
              aria-invalid={reasonError ? true : undefined}
              aria-describedby={reasonError ? "course-action-reason-error" : undefined}
              disabled={working}
              autoFocus
            />
            {reasonError && (
              <span id="course-action-reason-error" className="auth-error">
                {reasonError}
              </span>
            )}
          </label>
        )}
        {error && (
          <p className="auth-error" role="alert">
            {error}
          </p>
        )}
        <div className="qb-actions">
          <button
            type="submit"
            className={action === "approve" ? "qb-btn qb-primary" : "qb-btn qb-danger"}
            disabled={working || stale}
          >
            {working ? "Kaydediliyor…" : text.confirm}
          </button>
          <button type="button" className="qb-btn" onClick={close} disabled={working}>
            {error ? "Kapat" : "Vazgeç"}
          </button>
        </div>
      </form>
    </dialog>
  );
}

const ACTION_LABELS: Record<CourseAction, string> = {
  approve: "Onayla",
  reject: "Reddet",
  unpublish: "Yayından kaldır",
};

/**
 * Durumda gösterilecek işlem düğmeleri. Bu yalnız arayüz ipucudur; geçişin geçerli olup olmadığına backend'deki
 * CourseState karar verir (geçersizse 409).
 */
export function availableActions(status: CourseStatus): CourseAction[] {
  if (status === "IN_REVIEW") return ["approve", "reject"];
  if (status === "PUBLISHED") return ["unpublish"];
  return [];
}

/** Bir kursun durumuna uygun işlem düğmeleri; liste ve inceleme sayfası ortak kullanır. */
export function CourseActionButtons({
  course,
  onSelect,
}: {
  course: { id: number; title: string; status: CourseStatus };
  onSelect: (target: CourseActionTarget) => void;
}) {
  return (
    <>
      {availableActions(course.status).map((action) => (
        <button
          key={action}
          type="button"
          className={action === "approve" ? "qb-btn qb-primary" : "qb-btn qb-danger"}
          aria-label={`${course.title}: ${ACTION_LABELS[action]}`}
          onClick={() => onSelect({ course: { id: course.id, title: course.title }, action })}
        >
          {ACTION_LABELS[action]}
        </button>
      ))}
    </>
  );
}
