"use client";

import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api/client";
import {
  getQuizForStudent,
  submitQuizAttempt,
  type AttemptResultResponse,
  type StudentQuizResponse,
} from "@/lib/api/quizzes";
import { buildAnswers, questionLabel, toggleOption, unansweredQuestionIds, type Selection } from "@/lib/quiz/answers";
import { learnHref } from "@/lib/learn/navigation";
import styles from "./take.module.css";

type Loaded =
  | { key: string; kind: "ok"; quiz: StudentQuizResponse }
  | { key: string; kind: "forbidden" }
  | { key: string; kind: "notFound" }
  | { key: string; kind: "error"; message: string };

function fmt(n: number): string {
  return n.toLocaleString("tr-TR", { maximumFractionDigits: 2 });
}

function errorMessage(err: unknown, fallback: string): string {
  return err instanceof ApiError && err.body?.message ? err.body.message : fallback;
}

/** Sınav çözme ekranı (#57): sorular, gönderim öncesi cevapsız uyarısı ve sonuç. */
export function QuizTakeView() {
  const { id: rawId } = useParams<{ id: string }>();
  const id = /^\d+$/.test(rawId) ? Number(rawId) : null;
  // Oynatıcıdan gelinmişse kurs id'si adreste (?course=); sonuçtaki "Kursa dön" için (#58).
  const rawCourse = useSearchParams().get("course");
  const courseId = rawCourse !== null && /^\d+$/.test(rawCourse) ? Number(rawCourse) : null;
  const [retry, setRetry] = useState(0);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [selection, setSelection] = useState<Selection>({});
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [result, setResult] = useState<AttemptResultResponse | null>(null);
  const warningRef = useRef<HTMLDivElement>(null);
  const key = `${rawId}#${retry}`;
  const state: Loaded | null = id === null ? { key, kind: "notFound" } : loaded?.key === key ? loaded : null;

  useEffect(() => {
    if (id === null) return;
    let cancelled = false;
    getQuizForStudent(id)
      .then((quiz) => !cancelled && setLoaded({ key, kind: "ok", quiz }))
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 403) setLoaded({ key, kind: "forbidden" });
        else if (err instanceof ApiError && err.status === 404) setLoaded({ key, kind: "notFound" });
        else
          setLoaded({
            key,
            kind: "error",
            message: errorMessage(err, "Sınav yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin."),
          });
      });
    return () => {
      cancelled = true;
    };
  }, [id, key]);

  useEffect(() => {
    if (confirming) warningRef.current?.focus();
  }, [confirming]);

  const quiz = state?.kind === "ok" ? state.quiz : null;
  const multiple = quiz !== null && quiz.scoringType !== "SINGLE_ANSWER";
  const questionIds = quiz?.questions.map((q) => q.id) ?? [];
  const unanswered = unansweredQuestionIds(questionIds, selection);

  function choose(questionId: number, optionId: number) {
    setSelection((s) => ({ ...s, [questionId]: toggleOption(s[questionId], optionId, multiple) }));
    setConfirming(false);
    setSubmitError(null);
  }

  async function send() {
    if (id === null || quiz === null) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const res = await submitQuizAttempt(id, { answers: buildAnswers(questionIds, selection) });
      setResult(res);
    } catch (err) {
      setSubmitError(errorMessage(err, "Sınav gönderilemedi. Tekrar deneyin."));
      setConfirming(false);
    } finally {
      setSubmitting(false);
    }
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (submitting) return;
    if (unanswered.length > 0 && !confirming) {
      setConfirming(true);
      return;
    }
    void send();
  }

  if (result !== null && quiz !== null) {
    return (
      <Result
        quiz={quiz}
        result={result}
        quizId={id as number}
        courseId={courseId}
        onRetry={() => {
          setResult(null);
          setSelection({});
          setConfirming(false);
          setSubmitError(null);
        }}
      />
    );
  }

  return (
    <section>
      <h1>{quiz?.title ?? "Sınav"}</h1>
      {state === null && <p className={styles.status}>Sınav yükleniyor…</p>}
      {state?.kind === "forbidden" && (
        <p role="alert" className={styles.error}>
          Bu sınava girmek için kursa kayıtlı olmalısın.
        </p>
      )}
      {state?.kind === "notFound" && (
        <p role="alert" className={styles.error}>
          Sınav bulunamadı.
        </p>
      )}
      {state?.kind === "error" && (
        <div role="alert">
          <p className={styles.error}>{state.message}</p>
          <button type="button" className={styles.button} onClick={() => setRetry((r) => r + 1)}>
            Tekrar dene
          </button>
        </div>
      )}
      {quiz && (
        <form onSubmit={onSubmit} noValidate>
          <p className={styles.meta}>
            {quiz.questionCount} soru · Geçme eşiği: %{quiz.passThreshold}
            {quiz.required ? " · Zorunlu sınav" : ""}
          </p>
          {quiz.questions.length === 0 && <p className={styles.status}>Bu sınavda henüz soru yok.</p>}
          {quiz.questions.map((q, i) => {
            const missing = confirming && unanswered.includes(q.id);
            const hintId = `q${q.id}-hint`;
            return (
              <fieldset
                key={q.id}
                className={`${styles.question} ${missing ? styles.missing : ""}`}
                aria-describedby={hintId}
              >
                <legend>
                  {i + 1}. {q.text} ({fmt(q.points)} puan)
                </legend>
                <p id={hintId} className={styles.hint}>
                  {multiple ? "Birden fazla şık seçebilirsin." : "Tek şık seç."}
                  {missing ? " Bu soru cevapsız." : ""}
                </p>
                {q.options.map((o) => (
                  <label key={o.id} className={styles.option}>
                    <input
                      type={multiple ? "checkbox" : "radio"}
                      name={`q${q.id}`}
                      value={o.id}
                      checked={(selection[q.id] ?? []).includes(o.id)}
                      onChange={() => choose(q.id, o.id)}
                    />
                    <span>{o.text}</span>
                  </label>
                ))}
              </fieldset>
            );
          })}
          {confirming && (
            <div ref={warningRef} tabIndex={-1} role="alert" className={styles.warning}>
              <p>
                {unanswered.length} soru cevapsız:{" "}
                {unanswered.map((qid) => questionIds.indexOf(qid) + 1).join(", ")}. Cevapsız sorular 0 puan alır.
                Yine de göndermek istiyor musun?
              </p>
              <div className={styles.actions}>
                <button type="submit" className={`${styles.button} ${styles.primary}`} disabled={submitting}>
                  Yine de gönder
                </button>
                <button type="button" className={styles.button} onClick={() => setConfirming(false)}>
                  Geri dön
                </button>
              </div>
            </div>
          )}
          {submitError && (
            <p role="alert" className={styles.error}>
              {submitError}
            </p>
          )}
          {!confirming && quiz.questions.length > 0 && (
            <div className={styles.actions}>
              <button type="submit" className={`${styles.button} ${styles.primary}`} disabled={submitting}>
                {submitting ? "Gönderiliyor…" : "Sınavı gönder"}
              </button>
              <Link href={`/quizzes/${id}/attempts`}>Önceki denemelerim</Link>
            </div>
          )}
        </form>
      )}
    </section>
  );
}

/**
 * Sonuç (#58): puan, yüzde, eşik, geçti/kaldı ve soru bazında sonuç. Kalınca "Tekrar dene" aynı sınavı boş cevaplarla
 * yeniden açar; geçince "Kursa dön" oynatıcıya (kurs id'si biliniyorsa) ya da kayıtlı kurslara götürür.
 */
function Result({
  quiz,
  result,
  quizId,
  courseId,
  onRetry,
}: {
  quiz: StudentQuizResponse;
  result: AttemptResultResponse;
  quizId: number;
  courseId: number | null;
  onRetry: () => void;
}) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    headingRef.current?.focus();
  }, []);
  const byId = new Map(result.questions.map((r) => [r.questionId, r]));
  return (
    <section>
      <h1 ref={headingRef} tabIndex={-1}>
        {quiz.title}: sonuç
      </h1>
      <div className={styles.summary} role="group" aria-label="Sonuç özeti">
        <p>
          Puan: {fmt(result.score)} / {fmt(result.maxScore)}
        </p>
        <p>Yüzde: %{result.percent.toLocaleString("tr-TR", { maximumFractionDigits: 1 })}</p>
        <p>Geçme eşiği: %{quiz.passThreshold}</p>
        <p>
          {result.passed ? (
            <span className={styles.pass}>✓ Geçtin</span>
          ) : (
            <span className={styles.fail}>✗ Kaldın</span>
          )}
        </p>
      </div>
      <ol className={styles.results} aria-label="Soru bazında sonuç">
        {quiz.questions.map((q, i) => {
          const r = byId.get(q.id);
          return (
            <li key={q.id}>
              {questionLabel(i + 1, q.text)}{" "}
              {r ? (
                <span className={r.correct ? styles.pass : styles.fail}>
                  {r.correct ? "✓ Doğru" : "✗ Yanlış"} ({fmt(r.earnedPoints)} / {fmt(r.maxPoints)})
                </span>
              ) : (
                "-"
              )}
            </li>
          );
        })}
      </ol>
      <div className={styles.actions}>
        {result.passed ? (
          <Link href={courseId !== null ? learnHref(courseId) : "/my-courses"} className={`${styles.button} ${styles.primary}`}>
            Kursa dön
          </Link>
        ) : (
          <button type="button" className={`${styles.button} ${styles.primary}`} onClick={onRetry}>
            Tekrar dene
          </button>
        )}
        <Link href={`/quizzes/${quizId}/attempts`}>Deneme geçmişim</Link>
        {!result.passed && <Link href={courseId !== null ? learnHref(courseId) : "/my-courses"}>Kursa dön</Link>}
      </div>
    </section>
  );
}
