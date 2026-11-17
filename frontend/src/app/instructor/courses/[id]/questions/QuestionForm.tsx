"use client";

import { useState, type FormEvent } from "react";
import { ApiError } from "@/lib/api/client";
import { createQuestion, updateQuestion, type QuestionResponse } from "@/lib/api/quiz";
import {
  OPTIONS_MAX,
  OPTIONS_MIN,
  OPTION_MAX_LENGTH,
  POINTS_MAX,
  POINTS_MIN,
  TEXT_MAX,
  fromServerErrors,
  hasErrors,
  toRequest,
  validateQuestion,
  type QuestionDraft,
  type QuestionErrors,
} from "@/lib/quiz/validation";
import { errorMessage } from "./errors";

function emptyDraft(): QuestionDraft {
  return {
    text: "",
    points: "10",
    options: [
      { text: "", correct: true },
      { text: "", correct: false },
    ],
  };
}

function draftOf(question: QuestionResponse): QuestionDraft {
  return {
    text: question.text,
    points: String(question.points),
    options: question.options.map((o) => ({ text: o.text, correct: o.correct })),
  };
}

/**
 * Soru ekleme / düzenleme formu (#122). `question` verilirse düzenleme (PUT), yoksa ekleme (POST).
 * İstemci doğrulaması sözleşmedeki sınırlarla aynıdır; sunucunun 400 alan hataları ilgili alanın altında gösterilir.
 */
export function QuestionForm({
  courseId,
  question,
  onSaved,
  onCancel,
}: {
  courseId: number;
  question: QuestionResponse | null;
  onSaved: (saved: QuestionResponse) => void;
  onCancel: () => void;
}) {
  const [draft, setDraft] = useState<QuestionDraft>(() => (question ? draftOf(question) : emptyDraft()));
  const [errors, setErrors] = useState<QuestionErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function setOption(index: number, patch: Partial<QuestionDraft["options"][number]>) {
    setDraft((d) => ({ ...d, options: d.options.map((o, i) => (i === index ? { ...o, ...patch } : o)) }));
  }

  function addOption() {
    setDraft((d) => ({ ...d, options: [...d.options, { text: "", correct: false }] }));
  }

  function removeOption(index: number) {
    setDraft((d) => ({ ...d, options: d.options.filter((_, i) => i !== index) }));
    setErrors((e) => ({ ...e, optionTexts: undefined }));
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    const clientErrors = validateQuestion(draft);
    setErrors(clientErrors);
    if (hasErrors(clientErrors)) {
      setMessage("Girilen bilgilerde hata var.");
      return;
    }
    setSubmitting(true);
    try {
      const request = toRequest(draft);
      const saved = question ? await updateQuestion(question.id, request) : await createQuestion(courseId, request);
      onSaved(saved);
    } catch (e) {
      setSubmitting(false);
      if (e instanceof ApiError) setErrors(fromServerErrors(e.body?.errors));
      setMessage(errorMessage(e, "Soru kaydedilemedi. Lütfen tekrar deneyin."));
    }
  }

  const titleId = "question-form-title";
  return (
    <form className="qb-form" onSubmit={onSubmit} noValidate aria-labelledby={titleId}>
      <h2 id={titleId}>{question ? "Soruyu düzenle" : "Yeni soru"}</h2>
      {message && (
        <p className="auth-error" role="alert">
          {message}
        </p>
      )}
      <label>
        Soru metni
        <textarea
          name="text"
          rows={3}
          maxLength={TEXT_MAX}
          value={draft.text}
          onChange={(e) => setDraft((d) => ({ ...d, text: e.target.value }))}
          aria-invalid={errors.text ? true : undefined}
        />
        {errors.text && <span className="field-error">{errors.text}</span>}
      </label>
      <label className="qb-points">
        Puan
        <input
          name="points"
          type="number"
          inputMode="numeric"
          min={POINTS_MIN}
          max={POINTS_MAX}
          value={draft.points}
          onChange={(e) => setDraft((d) => ({ ...d, points: e.target.value }))}
          aria-invalid={errors.points ? true : undefined}
        />
        {errors.points && <span className="field-error">{errors.points}</span>}
      </label>
      <fieldset className="qb-options">
        <legend>
          Şıklar ({draft.options.length}/{OPTIONS_MAX}), doğru olanları işaretleyin
        </legend>
        {draft.options.map((option, index) => (
          <div className="qb-option-row" key={index}>
            <label className="qb-correct">
              <input
                type="checkbox"
                checked={option.correct}
                onChange={(e) => setOption(index, { correct: e.target.checked })}
                aria-label={`${index + 1}. şık doğru`}
              />
              Doğru
            </label>
            <div className="qb-option-text">
              <input
                value={option.text}
                maxLength={OPTION_MAX_LENGTH}
                placeholder={`${index + 1}. şık`}
                aria-label={`${index + 1}. şık metni`}
                onChange={(e) => setOption(index, { text: e.target.value })}
                aria-invalid={errors.optionTexts?.[index] ? true : undefined}
              />
              {errors.optionTexts?.[index] && <span className="field-error">{errors.optionTexts[index]}</span>}
            </div>
            <button
              type="button"
              className="qb-btn"
              onClick={() => removeOption(index)}
              disabled={draft.options.length <= OPTIONS_MIN}
              aria-label={`${index + 1}. şıkkı sil`}
            >
              Sil
            </button>
          </div>
        ))}
        {errors.options && <span className="field-error">{errors.options}</span>}
        <button type="button" className="qb-btn" onClick={addOption} disabled={draft.options.length >= OPTIONS_MAX}>
          Şık ekle
        </button>
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
