import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki CourseProgressResponse ve ResumeResponse ile aynıdır.

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

export interface ResumeResponse {
  courseId: number;
  lessonId?: number | null;
  lessonTitle?: string | null;
  sectionId?: number | null;
  progressPercent: number;
  lastAccessedAt?: string | null;
}

/**
 * Kaldığı yerden devam (#119): son açılan ders; hiç açılmamışsa ilk tamamlanmamış ders; kursta ders yoksa
 * `lessonId` null. Kayıtlı değilse 403 (ApiError).
 */
export function getMyResumePoint(courseId: number): Promise<ResumeResponse> {
  return apiFetch<ResumeResponse>(`/api/me/courses/${courseId}/resume`);
}
