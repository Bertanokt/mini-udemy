import { apiFetch } from "./client";
import type { CategoryResponse, CourseLevel, CourseStatus, UserSummary } from "./catalog";

// Tipler docs/api/openapi.yaml'daki CourseResponse ile aynıdır (eğitmen/yönetici görünümü).

export interface CourseResponse {
  id: number;
  title: string;
  description: string;
  category: CategoryResponse;
  level: CourseLevel;
  status: CourseStatus;
  instructor: UserSummary;
  coverUrl?: string | null;
  learningOutcomes: string[];
  rejectionReason?: string | null;
  unpublishReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export const STATUS_LABELS: Record<CourseStatus, string> = {
  DRAFT: "Taslak",
  IN_REVIEW: "İncelemede",
  PUBLISHED: "Yayımlandı",
  UNPUBLISHED: "Yayından kaldırıldı",
};

/** listMyInstructorCourses: eğitmenin tüm kursları; `status` ile süzülür. */
export function listMyCourses(status?: CourseStatus): Promise<CourseResponse[]> {
  const qs = status ? `?status=${encodeURIComponent(status)}` : "";
  return apiFetch<CourseResponse[]>(`/api/instructor/courses${qs}`);
}

/** submitCourse: DRAFT/UNPUBLISHED → IN_REVIEW. 400 eksik içerik, 409 geçersiz durum. */
export function submitCourse(id: number): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/courses/${id}/submit`, { method: "POST" });
}
