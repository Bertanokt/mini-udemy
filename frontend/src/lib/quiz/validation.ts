import type { QuestionRequest } from "@/lib/api/quiz";

// Sınırlar docs/api/openapi.yaml › QuestionRequest / OptionRequest ile aynıdır.
export const TEXT_MIN = 3;
export const TEXT_MAX = 2000;
export const POINTS_MIN = 1;
export const POINTS_MAX = 100;
export const OPTION_MAX_LENGTH = 500;
export const OPTIONS_MIN = 2;
export const OPTIONS_MAX = 10;

export interface QuestionErrors {
  text?: string;
  points?: string;
  /** Şık listesi düzeyindeki hata (sayı, doğru şık yok). */
  options?: string;
  /** Şık sırasına göre metin hataları. */
  optionTexts?: Record<number, string>;
}

/** Form alanları; puan kullanıcı yazarken metin olarak tutulur. */
export interface QuestionDraft {
  text: string;
  points: string;
  options: { text: string; correct: boolean }[];
}

export function validateQuestion(draft: QuestionDraft): QuestionErrors {
  const errors: QuestionErrors = {};
  const text = draft.text.trim();
  if (text.length < TEXT_MIN || text.length > TEXT_MAX) {
    errors.text = `Soru metni ${TEXT_MIN} ile ${TEXT_MAX} karakter arasında olmalı.`;
  }
  const points = Number(draft.points);
  if (draft.points.trim() === "" || !Number.isInteger(points) || points < POINTS_MIN || points > POINTS_MAX) {
    errors.points = `Puan ${POINTS_MIN} ile ${POINTS_MAX} arasında bir tam sayı olmalı.`;
  }
  if (draft.options.length < OPTIONS_MIN || draft.options.length > OPTIONS_MAX) {
    errors.options = `Soru en az ${OPTIONS_MIN}, en çok ${OPTIONS_MAX} şıktan oluşmalı.`;
  } else if (!draft.options.some((o) => o.correct)) {
    errors.options = "En az bir şık doğru olarak işaretlenmeli.";
  }
  draft.options.forEach((option, index) => {
    const length = option.text.trim().length;
    if (length < 1 || length > OPTION_MAX_LENGTH) {
      (errors.optionTexts ??= {})[index] = `Şık metni 1 ile ${OPTION_MAX_LENGTH} karakter arasında olmalı.`;
    }
  });
  return errors;
}

export function hasErrors(errors: QuestionErrors): boolean {
  return Boolean(errors.text || errors.points || errors.options || errors.optionTexts);
}

export function toRequest(draft: QuestionDraft): QuestionRequest {
  return {
    text: draft.text.trim(),
    points: Number(draft.points),
    options: draft.options.map((o) => ({ text: o.text.trim(), correct: o.correct })),
  };
}

/** Sunucunun `errors` alanını (text, points, options, options[0].text) form hatalarına çevirir. */
export function fromServerErrors(errors: Record<string, string> | undefined): QuestionErrors {
  const result: QuestionErrors = {};
  if (!errors) return result;
  for (const [key, message] of Object.entries(errors)) {
    if (key === "text") result.text = message;
    else if (key === "points") result.points = message;
    else if (key === "options") result.options = message;
    else {
      const match = /^options\[(\d+)\]/.exec(key);
      if (match) (result.optionTexts ??= {})[Number(match[1])] = message;
    }
  }
  return result;
}
