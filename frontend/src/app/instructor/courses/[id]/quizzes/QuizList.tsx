"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { listSections, type SectionResponse } from "@/lib/api/courses";
import {
  deleteQuiz,
  listQuestions,
  listSectionQuizzes,
  type InstructorQuizResponse,
  type QuestionResponse,
  type QuizSummaryResponse,
} from "@/lib/api/quiz";
import { SCORING_LABELS } from "@/lib/quiz/quizValidation";
import { errorMessage } from "./errors";
import { QuizForm } from "./QuizForm";
import { Loading } from "@/components/common/Loading";
import { ErrorMessage } from "@/components/common/ErrorMessage";
import { EmptyState } from "@/components/common/EmptyState";

interface SectionQuizzes {
  section: SectionResponse;
  quizzes: QuizSummaryResponse[];
}

type Load =
  | { state: "loading" }
  | { state: "error"; message: string; final: boolean }
  | { state: "ready"; sections: SectionQuizzes[]; bank: QuestionResponse[] };

/** Açık form: null = kapalı; quizId null ise yeni sınav, değilse kayıtlı sınavın düzenlenmesi. */
type Editing = null | { sectionId: number; quizId: number | null };

function toSummary(quiz: InstructorQuizResponse): QuizSummaryResponse {
  return {
    id: quiz.id,
    sectionId: quiz.sectionId,
    title: quiz.title,
    scoringType: quiz.scoringType,
    passThreshold: quiz.passThreshold,
    required: quiz.required,
    questionCount: quiz.questions.length,
  };
}

/** Kursun bölüm sonu sınavları (#50): bölüm bölüm liste, oluşturma, düzenleme, silme. */
export function QuizList() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  const validId = Number.isInteger(courseId) && courseId > 0;

  const [load, setLoad] = useState<Load>({ state: "loading" });
  const [editing, setEditing] = useState<Editing>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [toDelete, setToDelete] = useState<QuizSummaryResponse | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!validId) return;
    let cancelled = false;
    Promise.all([listSections(courseId), listQuestions(courseId)])
      .then(async ([sections, bank]) => {
        const quizzes = await Promise.all(sections.map((s) => listSectionQuizzes(s.id)));
        if (cancelled) return;
        setLoad({
          state: "ready",
          bank,
          sections: sections.map((section, i) => ({ section, quizzes: quizzes[i] })),
        });
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setLoad({
          state: "error",
          message: errorMessage(e, "Sınavlar yüklenemedi."),
          final: e instanceof ApiError && (e.status === 403 || e.status === 404),
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

  function updateSection(sectionId: number, change: (quizzes: QuizSummaryResponse[]) => QuizSummaryResponse[]) {
    setLoad((current) =>
      current.state === "ready"
        ? {
            ...current,
            sections: current.sections.map((s) =>
              s.section.id === sectionId ? { ...s, quizzes: change(s.quizzes) } : s,
            ),
          }
        : current,
    );
  }

  function onSaved(saved: InstructorQuizResponse) {
    const summary = toSummary(saved);
    const isNew = editing?.quizId == null;
    updateSection(summary.sectionId, (quizzes) =>
      quizzes.some((q) => q.id === summary.id)
        ? quizzes.map((q) => (q.id === summary.id ? summary : q))
        : [...quizzes, summary],
    );
    setNotice(isNew ? "Sınav oluşturuldu." : "Sınav güncellendi.");
    setEditing(null);
  }

  async function confirmDelete() {
    if (!toDelete) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteQuiz(toDelete.id);
      const { id, sectionId } = toDelete;
      updateSection(sectionId, (quizzes) => quizzes.filter((q) => q.id !== id));
      setToDelete(null);
      setNotice("Sınav silindi.");
    } catch (e) {
      setDeleteError(errorMessage(e, "Sınav silinemedi. Lütfen tekrar deneyin."));
    } finally {
      setDeleting(false);
    }
  }

  function closeDelete() {
    if (deleting) return;
    setToDelete(null);
    setDeleteError(null);
  }

  function open(next: Editing) {
    setNotice(null);
    setEditing(next);
  }

  if (!validId) {
    return (
      <section className="placeholder-page" role="alert">
        <h1>Bölüm sınavları</h1>
        <p>Geçersiz kurs adresi.</p>
      </section>
    );
  }

  const bankEmpty = load.state === "ready" && load.bank.length === 0;

  return (
    <section className="qb">
      <div className="qb-head">
        <h1>Bölüm sınavları</h1>
        <Link className="qb-btn" href={`/instructor/courses/${courseId}/questions`}>
          Soru bankası
        </Link>
      </div>

      {notice && (
        <p className="qb-notice" role="status">
          {notice}
        </p>
      )}

      {load.state === "loading" && <Loading text="Sınavlar yükleniyor…" />}

      {load.state === "error" && <ErrorMessage message={load.message} onRetry={load.final ? undefined : retry} />}

      {load.state === "ready" && (
        <>
          {bankEmpty && (
            <EmptyState
              message="Soru bankası boş. Sınav oluşturmak için önce soru bankasına soru ekleyin."
              action={{ label: "Soru bankasına git", href: `/instructor/courses/${courseId}/questions` }}
            />
          )}
          {load.sections.length === 0 && (
            <EmptyState
              message="Bu kursun henüz bölümü yok. Sınav bir bölüme eklenir; önce bölüm oluşturun."
              action={{ label: "Bölüm ve dersler", href: `/instructor/courses/${courseId}/sections` }}
            />
          )}

          <ul className="qb-list">
            {load.sections.map(({ section, quizzes }, sectionIndex) => (
              <li key={section.id} className="qb-item">
                <div className="qb-item-head">
                  <h2 className="qz-section-title">
                    {sectionIndex + 1}. {section.title}
                  </h2>
                  {!(editing?.sectionId === section.id && editing.quizId === null) && (
                    <button
                      type="button"
                      className="qb-btn"
                      disabled={bankEmpty}
                      onClick={() => open({ sectionId: section.id, quizId: null })}
                      aria-label={`${section.title} bölümüne sınav ekle`}
                    >
                      Sınav ekle
                    </button>
                  )}
                </div>

                {quizzes.length === 0 && !(editing?.sectionId === section.id) && (
                  <p className="qz-muted">Bu bölümde sınav yok.</p>
                )}

                <ul className="qz-quizzes">
                  {quizzes.map((quiz) =>
                    editing?.quizId === quiz.id ? (
                      <li key={quiz.id}>
                        <QuizForm
                          sectionId={section.id}
                          quizId={quiz.id}
                          bank={load.bank}
                          onSaved={onSaved}
                          onCancel={() => setEditing(null)}
                        />
                      </li>
                    ) : (
                      <li key={quiz.id} className="qz-quiz">
                        <div>
                          <p className="qb-text">{quiz.title}</p>
                          <div className="course-tags">
                            <span className="tag">{SCORING_LABELS[quiz.scoringType]}</span>
                            <span className="tag">Eşik %{quiz.passThreshold}</span>
                            <span className="tag">{quiz.questionCount} soru</span>
                            {quiz.required && <span className="tag">Sertifika için zorunlu</span>}
                          </div>
                        </div>
                        <div className="qb-actions">
                          <button
                            type="button"
                            className="qb-btn"
                            onClick={() => open({ sectionId: section.id, quizId: quiz.id })}
                            aria-label={`${quiz.title} sınavını düzenle`}
                          >
                            Düzenle
                          </button>
                          <button
                            type="button"
                            className="qb-btn qb-danger"
                            onClick={() => {
                              setNotice(null);
                              setDeleteError(null);
                              setToDelete(quiz);
                            }}
                            aria-label={`${quiz.title} sınavını sil`}
                          >
                            Sil
                          </button>
                        </div>
                      </li>
                    ),
                  )}
                </ul>

                {editing?.sectionId === section.id && editing.quizId === null && (
                  <QuizForm
                    sectionId={section.id}
                    quizId={null}
                    bank={load.bank}
                    onSaved={onSaved}
                    onCancel={() => setEditing(null)}
                  />
                )}
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
        <h2>Sınavı sil</h2>
        {toDelete && <p className="qb-dialog-text">“{toDelete.title}”</p>}
        <p>Sınav silinecek; sorular soru bankasında kalır. Denemesi olan sınav silinemez.</p>
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
