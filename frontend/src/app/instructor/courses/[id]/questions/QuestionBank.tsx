"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { deleteQuestion, listQuestions, type QuestionResponse } from "@/lib/api/quiz";
import { errorMessage } from "./errors";
import { QuestionForm } from "./QuestionForm";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

type Load =
  | { state: "loading" }
  | { state: "error"; message: string; forbidden: boolean }
  | { state: "ready"; questions: QuestionResponse[] };

/** Düzenlenen soru: null = form kapalı, "new" = yeni soru. */
type Editing = null | "new" | QuestionResponse;

/** Kurs için soru bankası (#122): liste, arama, ekleme, düzenleme, silme. */
export function QuestionBank() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  const validId = Number.isInteger(courseId) && courseId > 0;

  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [editing, setEditing] = useState<Editing>(null);
  const [search, setSearch] = useState("");
  const [toDelete, setToDelete] = useState<QuestionResponse | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!validId) return;
    let cancelled = false;
    listQuestions(courseId)
      .then((questions) => {
        if (!cancelled) setLoad({ state: "ready", questions });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setLoad({
          state: "error",
          message: errorMessage(e, "Sorular yüklenemedi."),
          forbidden: e instanceof ApiError && e.status === 403,
        });
      });
    return () => {
      cancelled = true;
    };
  }, [validId, courseId, attempt]);

  function retry() {
    setLoad({ state: "loading" });
    setAttempt((n) => n + 1);
  }

  const dialogRef = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (toDelete && !dialog.open) dialog.showModal();
    if (!toDelete && dialog.open) dialog.close();
  }, [toDelete]);

  const questions = useMemo(() => (load.state === "ready" ? load.questions : []), [load]);
  const filtered = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("tr");
    if (!term) return questions;
    return questions.filter(
      (q) =>
        q.text.toLocaleLowerCase("tr").includes(term) ||
        q.options.some((o) => o.text.toLocaleLowerCase("tr").includes(term)),
    );
  }, [questions, search]);

  function onSaved(saved: QuestionResponse) {
    setLoad((current) => {
      if (current.state !== "ready") return current;
      const exists = current.questions.some((q) => q.id === saved.id);
      return {
        state: "ready",
        questions: exists
          ? current.questions.map((q) => (q.id === saved.id ? saved : q))
          : [...current.questions, saved],
      };
    });
    setNotice(editing === "new" ? "Soru eklendi." : "Soru güncellendi.");
    setEditing(null);
  }

  async function confirmDelete() {
    if (!toDelete) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteQuestion(toDelete.id);
      const id = toDelete.id;
      setLoad((current) =>
        current.state === "ready"
          ? { state: "ready", questions: current.questions.filter((q) => q.id !== id) }
          : current,
      );
      setToDelete(null);
      setNotice("Soru silindi.");
    } catch (e) {
      setDeleteError(errorMessage(e, "Soru silinemedi. Lütfen tekrar deneyin."));
    } finally {
      setDeleting(false);
    }
  }

  function closeDelete() {
    if (deleting) return;
    setToDelete(null);
    setDeleteError(null);
  }

  if (!validId) {
    return (
      <section className="placeholder-page" role="alert">
        <h1>Soru bankası</h1>
        <p>Geçersiz kurs adresi.</p>
      </section>
    );
  }

  return (
    <section className="qb">
      <div className="qb-head">
        <h1>Soru bankası</h1>
        {load.state === "ready" && editing === null && (
          <button
            type="button"
            className="qb-btn qb-primary"
            onClick={() => {
              setNotice(null);
              setEditing("new");
            }}
          >
            Soru ekle
          </button>
        )}
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      {load.state === "loading" && <Loading text="Sorular yükleniyor…" />}

      {load.state === "error" && <ErrorMessage message={load.message} onRetry={load.forbidden ? undefined : retry} />}

      {load.state === "ready" && editing !== null && (
        <QuestionForm
          key={editing === "new" ? "new" : editing.id}
          courseId={courseId}
          question={editing === "new" ? null : editing}
          onSaved={onSaved}
          onCancel={() => setEditing(null)}
        />
      )}

      {load.state === "ready" && (
        <>
          {questions.length > 0 && (
            <label className="qb-search">
              <span className="visually-hidden">Sorularda ara</span>
              <input
                type="search"
                placeholder="Sorularda ara (soru ya da şık metni)"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
          )}

          {questions.length === 0 && (
            <EmptyState message="Bu kursun soru bankası boş. “Soru ekle” ile ilk soruyu oluşturun." />
          )}
          {questions.length > 0 && filtered.length === 0 && <EmptyState message="Aramanızla eşleşen soru yok." />}

          <ul className="qb-list">
            {filtered.map((q) => (
              <li key={q.id} className="qb-item">
                <div className="qb-item-head">
                  <p className="qb-text">{q.text}</p>
                  <span className="tag">{q.points} puan</span>
                </div>
                <ul className="qb-opts">
                  {q.options.map((o) => (
                    <li key={o.id} className={o.correct ? "qb-opt qb-opt-correct" : "qb-opt"}>
                      <span aria-hidden="true">{o.correct ? "✓" : "○"}</span>
                      <span>{o.text}</span>
                      {o.correct && <span className="visually-hidden">(doğru şık)</span>}
                    </li>
                  ))}
                </ul>
                <div className="qb-actions">
                  <button
                    type="button"
                    className="qb-btn"
                    onClick={() => {
                      setNotice(null);
                      setEditing(q);
                    }}
                  >
                    Düzenle
                  </button>
                  <button
                    type="button"
                    className="qb-btn qb-danger"
                    onClick={() => {
                      setNotice(null);
                      setDeleteError(null);
                      setToDelete(q);
                    }}
                  >
                    Sil
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      <dialog
        ref={dialogRef}
        className="qb-dialog"
        onClose={closeDelete}
        onCancel={(e) => {
          if (deleting) e.preventDefault();
        }}
      >
        <h2>Soruyu sil</h2>
        {toDelete && <p className="qb-dialog-text">“{toDelete.text}”</p>}
        <p>Bu soru silinecek. Bu işlem geri alınamaz.</p>
        {deleteError && (
          <p className="auth-error" role="alert">
            {deleteError}
          </p>
        )}
        <div className="qb-actions">
          <button type="button" className="qb-btn qb-danger" onClick={() => void confirmDelete()} disabled={deleting}>
            {deleting ? "Siliniyor…" : "Evet, sil"}
          </button>
          <button type="button" className="qb-btn" onClick={closeDelete} disabled={deleting}>
            Vazgeç
          </button>
        </div>
      </dialog>
    </section>
  );
}
