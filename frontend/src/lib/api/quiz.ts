import { apiFetch } from "./client";
import type { ScoringType } from "./quizzes";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (OptionRequest, QuestionRequest, OptionResponse, QuestionResponse).

export interface OptionRequest {
  text: string;
  correct: boolean;
}

export interface QuestionRequest {
  text: string;
  points: number;
  options: OptionRequest[];
}

export interface OptionResponse {
  id: number;
  text: string;
  correct: boolean;
}

export interface QuestionResponse {
  id: number;
  courseId: number;
  text: string;
  points: number;
  options: OptionResponse[];
}

export function listQuestions(courseId: number): Promise<QuestionResponse[]> {
  return apiFetch<QuestionResponse[]>(`/api/courses/${courseId}/questions`);
}

export function createQuestion(courseId: number, request: QuestionRequest): Promise<QuestionResponse> {
  return apiFetch<QuestionResponse>(`/api/courses/${courseId}/questions`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateQuestion(id: number, request: QuestionRequest): Promise<QuestionResponse> {
  return apiFetch<QuestionResponse>(`/api/questions/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function deleteQuestion(id: number): Promise<void> {
  return apiFetch<void>(`/api/questions/${id}`, { method: "DELETE" });
}

// Sınav (eğitmen) tipleri docs/api/openapi.yaml'daki QuizRequest, QuizSummaryResponse ve InstructorQuizResponse ile
// aynıdır. ScoringType öğrenci uçlarıyla ortaktır (lib/api/quizzes.ts).

export interface QuizRequest {
  title: string;
  scoringType: ScoringType;
  /** Geçme eşiği (yüzde, 0–100). */
  passThreshold: number;
  /** Sertifika için geçilmesi zorunlu mu. */
  required: boolean;
  /** Aynı kursun soru bankasından; verilen sıra sınavdaki sıradır. */
  questionIds: number[];
}

export interface QuizSummaryResponse {
  id: number;
  sectionId: number;
  title: string;
  scoringType: ScoringType;
  passThreshold: number;
  required: boolean;
  questionCount: number;
}

export interface InstructorQuizResponse extends QuizSummaryResponse {
  /** Sınavdaki sırayla, doğru şık bilgisiyle. */
  questions: QuestionResponse[];
}

/** listSectionQuizzes: bölümün sınav özetleri. Kursun sahibi değilse 403, bölüm yoksa 404. */
export function listSectionQuizzes(sectionId: number): Promise<QuizSummaryResponse[]> {
  return apiFetch<QuizSummaryResponse[]>(`/api/sections/${sectionId}/quizzes`);
}

/** createQuiz: 400 başka kursun/tekrarlı soru, 403 sahibi değil, 409 kurs düzenlenemez. */
export function createQuiz(sectionId: number, request: QuizRequest): Promise<InstructorQuizResponse> {
  return apiFetch<InstructorQuizResponse>(`/api/sections/${sectionId}/quizzes`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** getQuizForInstructor: sınav doğru cevaplarla (kursun sahibi). */
export function getQuizForInstructor(id: number): Promise<InstructorQuizResponse> {
  return apiFetch<InstructorQuizResponse>(`/api/instructor/quizzes/${id}`);
}

/** updateQuiz: denemesi olan sınavda soru ve puanlama tipi değişirse 409. */
export function updateQuiz(id: number, request: QuizRequest): Promise<InstructorQuizResponse> {
  return apiFetch<InstructorQuizResponse>(`/api/quizzes/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

/** deleteQuiz: denemesi olan sınav silinemez (409). */
export function deleteQuiz(id: number): Promise<void> {
  return apiFetch<void>(`/api/quizzes/${id}`, { method: "DELETE" });
}
