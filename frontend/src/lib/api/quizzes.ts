import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki ScoringType, StudentQuizResponse ve AttemptSummaryResponse ile aynıdır.
// (Soru bankası ve eğitmen uçları lib/api/quiz.ts'tedir.)

export type ScoringType = "SINGLE_ANSWER" | "MULTI_ALL_OR_NOTHING" | "MULTI_PARTIAL";

export interface StudentQuizResponse {
  id: number;
  sectionId: number;
  title: string;
  scoringType: ScoringType;
  passThreshold: number;
  required: boolean;
  questionCount: number;
  questions: {
    id: number;
    text: string;
    points: number;
    options: { id: number; text: string }[];
  }[];
}

export interface AttemptSummaryResponse {
  id: number;
  quizId: number;
  score: number;
  maxScore: number;
  percent: number;
  passed: boolean;
  submittedAt: string;
}

/** Sınavı öğrenci görünümüyle getirir (doğru cevapsız). Kursa kayıtlı değilse 403, yoksa 404 (ApiError). */
export function getQuizForStudent(quizId: number): Promise<StudentQuizResponse> {
  return apiFetch<StudentQuizResponse>(`/api/quizzes/${quizId}`);
}

/** Kursiyerin bir sınavdaki denemeleri; en yeni önce (STUDENT). Kayıtsızsa 403, sınav yoksa 404. */
export function listMyQuizAttempts(quizId: number): Promise<AttemptSummaryResponse[]> {
  return apiFetch<AttemptSummaryResponse[]>(`/api/me/quizzes/${quizId}/attempts`);
}
