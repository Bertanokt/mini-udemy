import { apiFetch } from "./client";
import type { CourseSummaryResponse } from "./catalog";

// Tipler docs/api/openapi.yaml'daki EnrollmentResponse ile aynıdır.

export interface EnrollmentResponse {
  id: number;
  course: CourseSummaryResponse;
  enrolledAt: string;
  progressPercent: number;
  lastLessonId?: number | null;
  lastAccessedAt?: string | null;
}

/** Kursa kayıt olur (STUDENT). Zaten kayıtlıysa 409, yayımlanmamış/olmayan kursta 404 (ApiError). */
export function enrollCourse(courseId: number): Promise<EnrollmentResponse> {
  return apiFetch<EnrollmentResponse>(`/api/courses/${courseId}/enroll`, { method: "POST" });
}

/** Kursiyerin kayıtlı kursları; en son kaydolunan önce (STUDENT). */
export function listMyEnrollments(): Promise<EnrollmentResponse[]> {
  return apiFetch<EnrollmentResponse[]>("/api/me/enrollments");
}
