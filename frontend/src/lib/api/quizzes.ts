import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki ScoringType, QuizSummaryResponse, StudentQuizResponse ve AttemptSummaryResponse
// ile aynıdır.
// (Soru bankası ve eğitmen uçları lib/api/quiz.ts'tedir.)

export type ScoringType = "SINGLE_ANSWER" | "MULTI_ALL_OR_NOTHING" | "MULTI_PARTIAL";

/** Sınavın özeti (sözleşme › QuizSummaryResponse); bölümün sınav listesinde döner. */
export interface QuizSummaryResponse {
  id: number;
  sectionId: number;
  title: string;
  scoringType: ScoringType;
  passThreshold: number;
  required: boolean;
  questionCount: number;
}

/** Bölümün sınavları (kursa kayıtlı STUDENT, kursun eğitmeni ya da ADMIN); kayıtsızsa 403 (ApiError). */
export function listSectionQuizzes(sectionId: number): Promise<QuizSummaryResponse[]> {
  return apiFetch<QuizSummaryResponse[]>(`/api/sections/${sectionId}/quizzes`);
}

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

export interface SubmitAttemptRequest {
  answers: { questionId: number; optionIds: number[] }[];
}

/** AttemptResultResponse: özet alanlarına ek olarak soru bazında sonuç. */
export interface AttemptResultResponse extends AttemptSummaryResponse {
  questions: { questionId: number; correct: boolean; earnedPoints: number; maxPoints: number }[];
}

/** Sınavı gönderir (STUDENT, kursa kayıtlı). Cevapsız soru listeye konmaz. 201 + sonuç. */
export function submitQuizAttempt(quizId: number, request: SubmitAttemptRequest): Promise<AttemptResultResponse> {
  return apiFetch<AttemptResultResponse>(`/api/quizzes/${quizId}/attempts`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}
