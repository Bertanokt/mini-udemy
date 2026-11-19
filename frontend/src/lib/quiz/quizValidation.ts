import type { QuestionResponse, QuizRequest } from "@/lib/api/quiz";
import type { ScoringType } from "@/lib/api/quizzes";

// Sınırlar docs/api/openapi.yaml › QuizRequest ile aynıdır; varsayılan eşik FR-24'e göre %70.
export const QUIZ_TITLE_MAX = 200;
export const THRESHOLD_MIN = 0;
export const THRESHOLD_MAX = 100;
export const DEFAULT_THRESHOLD = 70;

/**
 * Puanlama tipleri ve kısa açıklamaları (docs/api/openapi.yaml › ScoringType). Puanı istemci hesaplamaz;
 * kural sunucuda sınavın tipine göre seçilen stratejidedir (ADR-008).
 */
export const SCORING_TYPES: { value: ScoringType; label: string; hint: string }[] = [
  {
    value: "SINGLE_ANSWER",
    label: "Tek doğru şık",
    hint: "Her soruda tek doğru şık vardır; doğru seçilirse sorunun puanı alınır.",
  },
  {
    value: "MULTI_ALL_OR_NOTHING",
    label: "Çok doğru, hepsi ya da hiç",
    hint: "Tüm doğru şıklar ve yalnız onlar seçilirse tam puan, aksi halde 0.",
  },
  {
    value: "MULTI_PARTIAL",
    label: "Çok doğru, kısmi puan",
    hint: "Doğru seçilen şık oranında puan; her yanlış seçim bir doğruyu götürür, puan 0'ın altına inmez.",
  },
];

export const SCORING_LABELS = Object.fromEntries(SCORING_TYPES.map((t) => [t.value, t.label])) as Record<
  ScoringType,
  string
>;

/** Form alanları; eşik kullanıcı yazarken metin olarak tutulur, sorular sınavdaki sırayla. */
export interface QuizDraft {
  title: string;
  scoringType: ScoringType;
  passThreshold: string;
  required: boolean;
  questions: QuestionResponse[];
}

export interface QuizErrors {
  title?: string;
  scoringType?: string;
  passThreshold?: string;
  questionIds?: string;
}

/** Tek doğru şıklı puanlamaya uymayan sorular: doğru şık sayısı 1 olmayanlar (sunucuda deneme anında hata verir). */
export function questionsNotSingleAnswer(questions: QuestionResponse[]): QuestionResponse[] {
  return questions.filter((q) => q.options.filter((o) => o.correct).length !== 1);
}

export function validateQuiz(draft: QuizDraft): QuizErrors {
  const errors: QuizErrors = {};
  const title = draft.title.trim();
  if (title.length < 1 || title.length > QUIZ_TITLE_MAX) {
    errors.title = `Sınav başlığı 1 ile ${QUIZ_TITLE_MAX} karakter arasında olmalı.`;
  }
  const threshold = Number(draft.passThreshold);
  if (
    draft.passThreshold.trim() === "" ||
    !Number.isInteger(threshold) ||
    threshold < THRESHOLD_MIN ||
    threshold > THRESHOLD_MAX
  ) {
    errors.passThreshold = `Başarı eşiği ${THRESHOLD_MIN} ile ${THRESHOLD_MAX} arasında bir tam sayı olmalı.`;
  }
  if (draft.questions.length === 0) {
    errors.questionIds = "Sınavda en az bir soru olmalı.";
  } else if (draft.scoringType === "SINGLE_ANSWER" && questionsNotSingleAnswer(draft.questions).length > 0) {
    errors.scoringType =
      "Tek doğru şıklı puanlamada seçilen her sorunun tam bir doğru şıkkı olmalı. İşaretli soruları çıkarın ya da çok doğrulu bir puanlama tipi seçin.";
  }
  return errors;
}

export function hasQuizErrors(errors: QuizErrors): boolean {
  return Boolean(errors.title || errors.scoringType || errors.passThreshold || errors.questionIds);
}

export function toQuizRequest(draft: QuizDraft): QuizRequest {
  return {
    title: draft.title.trim(),
    scoringType: draft.scoringType,
    passThreshold: Number(draft.passThreshold),
    required: draft.required,
    questionIds: draft.questions.map((q) => q.id),
  };
}

/** Sunucunun `errors` alanını (title, scoringType, passThreshold, questionIds[…]) form hatalarına çevirir. */
export function fromQuizServerErrors(errors: Record<string, string> | undefined): QuizErrors {
  const result: QuizErrors = {};
  if (!errors) return result;
  for (const [key, message] of Object.entries(errors)) {
    if (key === "title") result.title = message;
    else if (key === "scoringType") result.scoringType = message;
    else if (key === "passThreshold") result.passThreshold = message;
    else if (key.startsWith("questionIds")) result.questionIds = message;
  }
  return result;
}
