import { apiFetch } from "./client";
import type { CourseSummaryResponse } from "./catalog";

// Tipler docs/api/openapi.yaml'daki CourseDetailResponse, SectionResponse ve LessonResponse ile aynıdır.

export type ContentType = "VIDEO" | "PDF" | "DOCUMENT";

export interface LessonResponse {
  id: number;
  sectionId: number;
  title: string;
  contentType: ContentType;
  mandatory: boolean;
  durationSeconds: number;
  position: number;
  hasContent: boolean;
}

export interface SectionResponse {
  id: number;
  courseId: number;
  title: string;
  position: number;
  lessons: LessonResponse[];
}

export interface CourseDetailResponse extends CourseSummaryResponse {
  description: string;
  learningOutcomes: string[];
  sections: SectionResponse[];
}

/** Kurs detayı; token isteğe bağlıdır (apiFetch varsa ekler). Yayımlanmamış/olmayan kurs 404. */
export function getCourse(id: number): Promise<CourseDetailResponse> {
  return apiFetch<CourseDetailResponse>(`/api/courses/${id}`);
}
