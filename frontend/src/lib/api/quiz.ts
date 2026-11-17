import { apiFetch } from "./client";

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
