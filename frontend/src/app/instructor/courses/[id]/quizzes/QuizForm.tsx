"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api/client";
import {
  createQuiz,
  getQuizForInstructor,
  updateQuiz,
  type InstructorQuizResponse,
  type QuestionResponse,
} from "@/lib/api/quiz";
import {
  DEFAULT_THRESHOLD,
  QUIZ_TITLE_MAX,
  SCORING_TYPES,
  THRESHOLD_MAX,
  THRESHOLD_MIN,
  fromQuizServerErrors,
  hasQuizErrors,
  questionsNotSingleAnswer,
  toQuizRequest,
  validateQuiz,
  type QuizDraft,
  type QuizErrors,
} from "@/lib/quiz/quizValidation";
import { errorMessage } from "./errors";
import { Loading } from "@/components/common/Loading";

function emptyDraft(): QuizDraft {
  return {
    title: "",
    scoringType: "SINGLE_ANSWER",
    passThreshold: String(DEFAULT_THRESHOLD),
    required: true,
    questions: [],
  };
}

function draftOf(quiz: InstructorQuizResponse): QuizDraft {
  return {
    title: quiz.title,
    scoringType: quiz.scoringType,
    passThreshold: String(quiz.passThreshold),
    required: quiz.required,
    questions: quiz.questions,
  };
}

function correctCount(question: QuestionResponse): number {
  return question.options.filter((o) => o.correct).length;
}

type Load = { state: "loading" } | { state: "error"; message: string } | { state: "ready" };

/**
 * Bölüm sonu sınavı formu (#50). `quizId` verilirse kayıtlı sınav doğru cevaplarla yüklenip düzenlenir (PUT),
 * yoksa yeni sınav oluşturulur (POST). Sorular bankadan seçilir; seçili listedeki sıra sınavdaki sıradır.
 */
export function QuizForm({
  sectionId,
  quizId,
  bank,
  onSaved,
  onCancel,
}: {
  sectionId: number;
  quizId: number | null;
  bank: QuestionResponse[];
  onSaved: (saved: InstructorQuizResponse) => void;
  onCancel: () => void;
}) {
  const [load, setLoad] = useState<Load>(quizId === null ? { state: "ready" } : { state: "loading" });
  const [draft, setDraft] = useState<QuizDraft>(emptyDraft);
  const [errors, setErrors] = useState<QuizErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (quizId === null) return;
    let cancelled = false;
    getQuizForInstructor(quizId)
      .then((quiz) => {
        if (cancelled) return;
        setDraft(draftOf(quiz));
        setLoad({ state: "ready" });
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoad({ state: "error", message: errorMessage(e, "Sınav yüklenemedi.") });
      });
    return () => {
      cancelled = true;
    };
  }, [quizId]);

  const selectedIds = useMemo(() => new Set(draft.questions.map((q) => q.id)), [draft.questions]);
  const available = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("tr");
    return bank.filter(
      (q) =>
        !selectedIds.has(q.id) &&
        (!term ||
          q.text.toLocaleLowerCase("tr").includes(term) ||
          q.options.some((o) => o.text.toLocaleLowerCase("tr").includes(term))),
    );
  }, [bank, selectedIds, search]);

  const singleAnswer = draft.scoringType === "SINGLE_ANSWER";
  const mismatched = useMemo(
    () => new Set(singleAnswer ? questionsNotSingleAnswer(draft.questions).map((q) => q.id) : []),
    [singleAnswer, draft.questions],
  );
  const totalPoints = draft.questions.reduce((sum, q) => sum + q.points, 0);

  function setQuestions(change: (questions: QuestionResponse[]) => QuestionResponse[]) {
    setDraft((d) => ({ ...d, questions: change(d.questions) }));
    setErrors((e) => ({ ...e, questionIds: undefined, scoringType: undefined }));
  }

  function move(index: number, delta: -1 | 1) {
    setQuestions((qs) => {
      const target = index + delta;
      if (target < 0 || target >= qs.length) return qs;
      const next = [...qs];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const clientErrors = validateQuiz(draft);
    setErrors(clientErrors);
    if (hasQuizErrors(clientErrors)) {
      setMessage("Girilen bilgilerde hata var.");
      return;
    }
    setSubmitting(true);
    try {
      const request = toQuizRequest(draft);
      const saved = quizId === null ? await createQuiz(sectionId, request) : await updateQuiz(quizId, request);
      onSaved(saved);
    } catch (e) {
      setSubmitting(false);
      if (e instanceof ApiError) setErrors(fromQuizServerErrors(e.body?.errors));
      setMessage(errorMessage(e, "Sınav kaydedilemedi. Lütfen tekrar deneyin."));
    }
  }

  const titleId = `quiz-form-title-${quizId ?? `new-${sectionId}`}`;

  if (load.state === "loading") return <Loading text="Sınav yükleniyor…" />;
  if (load.state === "error") {
    return (
      <div className="qb-state" role="alert">
        <p className="auth-error">{load.message}</p>
        <button type="button" className="qb-btn" onClick={onCancel}>
          Kapat
        </button>
      </div>
    );
  }

  return (
    <form className="qb-form" onSubmit={onSubmit} noValidate aria-labelledby={titleId}>
      <h3 id={titleId}>{quizId === null ? "Yeni sınav" : "Sınavı düzenle"}</h3>
      {message && (
        <p className="auth-error" role="alert">
          {message}
        </p>
      )}

      <label>
        Sınav başlığı
        <input
          name="title"
          maxLength={QUIZ_TITLE_MAX}
          value={draft.title}
          onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
          aria-invalid={errors.title ? true : undefined}
        />
        {errors.title && <span className="field-error">{errors.title}</span>}
      </label>

      <fieldset className="qb-options">
        <legend>Puanlama tipi</legend>
        {SCORING_TYPES.map((t) => (
          <label key={t.value} className="qz-radio">
            <input
              type="radio"
              name="scoringType"
              value={t.value}
              checked={draft.scoringType === t.value}
              onChange={() => {
                setDraft((d) => ({ ...d, scoringType: t.value }));
                setErrors((e) => ({ ...e, scoringType: undefined }));
              }}
            />
            <span>
              <strong>{t.label}</strong>
              <span className="qz-muted"> — {t.hint}</span>
            </span>
          </label>
        ))}
        {errors.scoringType && <span className="field-error">{errors.scoringType}</span>}
      </fieldset>

      <label className="qb-points">
        Başarı eşiği (%)
        <input
          name="passThreshold"
          type="number"
          inputMode="numeric"
          min={THRESHOLD_MIN}
          max={THRESHOLD_MAX}
          value={draft.passThreshold}
          onChange={(e) => setDraft((d) => ({ ...d, passThreshold: e.target.value }))}
          aria-invalid={errors.passThreshold ? true : undefined}
        />
        {errors.passThreshold && <span className="field-error">{errors.passThreshold}</span>}
      </label>

      <label className="qz-radio">
        <input
          type="checkbox"
          checked={draft.required}
          onChange={(e) => setDraft((d) => ({ ...d, required: e.target.checked }))}
        />
        <span>Sertifika için geçilmesi zorunlu</span>
      </label>

      <fieldset className="qb-options">
        <legend>
          Sınavdaki sorular ({draft.questions.length} soru, toplam {totalPoints} puan) — sıra sınavdaki sıradır
        </legend>
        {draft.questions.length === 0 && <p className="qz-muted">Henüz soru seçilmedi. Aşağıdaki bankadan ekleyin.</p>}
        <ol className="qz-selected">
          {draft.questions.map((q, index) => (
            <li key={q.id} className={mismatched.has(q.id) ? "qz-row qz-row-warn" : "qz-row"}>
              <span className="qz-pos" aria-hidden="true">
                {index + 1}.
              </span>
              <span className="qz-row-text">
                {q.text}
                <span className="qz-muted">
                  {" "}
                  · {q.points} puan · {correctCount(q)} doğru şık
                </span>
                {mismatched.has(q.id) && <span className="field-error"> Tek doğru şıklı puanlamaya uymuyor.</span>}
              </span>
              <span className="qb-actions">
                <button
                  type="button"
                  className="qb-btn"
                  onClick={() => move(index, -1)}
                  disabled={index === 0}
                  aria-label={`${index + 1}. soruyu yukarı taşı`}
                >
                  ↑
                </button>
                <button
                  type="button"
                  className="qb-btn"
                  onClick={() => move(index, 1)}
                  disabled={index === draft.questions.length - 1}
                  aria-label={`${index + 1}. soruyu aşağı taşı`}
                >
                  ↓
                </button>
                <button
                  type="button"
                  className="qb-btn"
                  onClick={() => setQuestions((qs) => qs.filter((x) => x.id !== q.id))}
                  aria-label={`${index + 1}. soruyu sınavdan çıkar`}
                >
                  Çıkar
                </button>
              </span>
            </li>
          ))}
        </ol>
        {errors.questionIds && <span className="field-error">{errors.questionIds}</span>}
      </fieldset>

      <fieldset className="qb-options">
        <legend>Soru bankası ({available.length} soru eklenebilir)</legend>
        <label className="qb-search">
          <span className="visually-hidden">Bankada ara</span>
          <input
            type="search"
            placeholder="Bankada ara (soru ya da şık metni)"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </label>
        {available.length === 0 && (
          <p className="qz-muted">
            {search.trim() ? "Aramanızla eşleşen soru yok." : "Bankadaki tüm sorular sınava eklendi."}
          </p>
        )}
        <ul className="qz-selected">
          {available.map((q) => (
            <li key={q.id} className="qz-row">
              <span className="qz-row-text">
                {q.text}
                <span className="qz-muted">
                  {" "}
                  · {q.points} puan · {correctCount(q)} doğru şık
                </span>
              </span>
              <button
                type="button"
                className="qb-btn"
                onClick={() => setQuestions((qs) => [...qs, q])}
                aria-label={`“${q.text}” sorusunu sınava ekle`}
              >
                Ekle
              </button>
            </li>
          ))}
        </ul>
      </fieldset>

      <div className="qb-actions">
        <button type="submit" className="qb-btn qb-primary" disabled={submitting}>
          {submitting ? "Kaydediliyor…" : "Kaydet"}
        </button>
        <button type="button" className="qb-btn" onClick={onCancel} disabled={submitting}>
          Vazgeç
        </button>
      </div>
    </form>
  );
}
