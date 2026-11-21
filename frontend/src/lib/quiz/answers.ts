import type { SubmitAttemptRequest } from "@/lib/api/quizzes";

/** Soru id'si → seçili şık id'leri. */
export type Selection = Record<number, number[]>;

/** Boş seçimli soruları atlar (sözleşme: cevapsız soru listeye konmaz). */
export function buildAnswers(questionIds: number[], selection: Selection): SubmitAttemptRequest["answers"] {
  return questionIds
    .filter((q) => (selection[q]?.length ?? 0) > 0)
    .map((q) => ({ questionId: q, optionIds: selection[q] }));
}

export function unansweredQuestionIds(questionIds: number[], selection: Selection): number[] {
  return questionIds.filter((q) => (selection[q]?.length ?? 0) === 0);
}

/** Radio: seçimi değiştirir; checkbox: şıkkı ekler/çıkarır. */
export function toggleOption(current: number[] | undefined, optionId: number, multiple: boolean): number[] {
  if (!multiple) return [optionId];
  const list = current ?? [];
  return list.includes(optionId) ? list.filter((o) => o !== optionId) : [...list, optionId];
}

/**
 * Sonuçtaki soru satırının başı (#296): "1. Soru metni:" — metin zaten noktalamayla (? . ! : …) bitiyorsa iki nokta
 * eklenmez ("1. … hangisidir?" olur, "?:" değil).
 */
export function questionLabel(index: number, text: string): string {
  const trimmed = text.trim();
  return /[?.!:…]$/.test(trimmed) ? `${index}. ${trimmed}` : `${index}. ${trimmed}:`;
}
