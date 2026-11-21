import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki CourseProgressResponse ile aynıdır.

export interface SectionProgress {
  sectionId: number;
  progressPercent: number;
}

export interface CourseProgressResponse {
  courseId: number;
  progressPercent: number;
  completedLessonIds: number[];
  sections: SectionProgress[];
}

/** Kursiyerin kurstaki ilerlemesi (STUDENT); kayıtlı değilse 403 (ApiError). */
export function getMyCourseProgress(courseId: number): Promise<CourseProgressResponse> {
  return apiFetch<CourseProgressResponse>(`/api/me/courses/${courseId}/progress`);
}

/**
 * Dersi tamamlandı işaretler (STUDENT, kayıtlı); kursun güncel ilerlemesini döner. İdempotent: zaten tamamlanmış
 * derste ilerleme değişmez.
 */
export function completeLesson(lessonId: number): Promise<CourseProgressResponse> {
  return apiFetch<CourseProgressResponse>(`/api/lessons/${lessonId}/complete`, { method: "POST" });
}
