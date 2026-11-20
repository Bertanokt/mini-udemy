import { apiFetch } from "./client";
import type { CategoryResponse, CourseLevel, CourseStatus, PageMeta, UserSummary } from "./catalog";
import type { ContentType, LessonResponse, SectionResponse } from "./courses";
import type { RatingSummary } from "./reviews";

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

/** docs/api/openapi.yaml › CreateCourseRequest (UpdateCourseRequest aynı şema). */
export interface CourseRequest {
  title: string;
  description: string;
  categoryId: number;
  level: CourseLevel;
  learningOutcomes: string[];
}

/** createCourse: INSTRUCTOR; kurs DRAFT oluşur. 400 alan hataları (`errors`), olmayan kategori `errors.categoryId`. */
export function createCourse(request: CourseRequest): Promise<CourseResponse> {
  return apiFetch<CourseResponse>("/api/courses", { method: "POST", body: JSON.stringify(request) });
}

/** updateCourse: kursun sahibi; yalnız DRAFT/UNPUBLISHED (State), aksi 409. 403 sahibi değil, 404 kurs yok. */
export function updateCourse(id: number, request: CourseRequest): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/courses/${id}`, { method: "PUT", body: JSON.stringify(request) });
}

/** uploadCourseCover: jpg/png/webp, en fazla 2 MB (tür içerikten denetlenir). 413 büyük, 415 tür, 409 durum. */
export function uploadCourseCover(id: number, file: File): Promise<CourseResponse> {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<CourseResponse>(`/api/courses/${id}/cover`, { method: "POST", body });
}

/** submitCourse: DRAFT/UNPUBLISHED → IN_REVIEW. 400 eksik içerik, 409 geçersiz durum. */
export function submitCourse(id: number): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/courses/${id}/submit`, { method: "POST" });
}

// Bölüm ve ders yönetimi (#24, #25). Hepsi kursun sahibi eğitmene açık; düzenlenebilirlik CourseState'tedir (409).
// Tipler docs/api/openapi.yaml › SectionRequest, LessonRequest, PositionRequest ile aynıdır.

export interface SectionRequest {
  title: string;
}

export interface LessonRequest {
  title: string;
  contentType: ContentType;
  mandatory: boolean;
  durationSeconds: number;
}

/** createSection: bölüm sona eklenir. */
export function createSection(courseId: number, request: SectionRequest): Promise<SectionResponse> {
  return apiFetch<SectionResponse>(`/api/courses/${courseId}/sections`, { method: "POST", body: JSON.stringify(request) });
}

/** updateSection: bölümün adını değiştirir. */
export function updateSection(sectionId: number, request: SectionRequest): Promise<SectionResponse> {
  return apiFetch<SectionResponse>(`/api/sections/${sectionId}`, { method: "PUT", body: JSON.stringify(request) });
}

/** deleteSection: bölümü dersleriyle siler; kalanlar boşluksuz sıralanır. */
export function deleteSection(sectionId: number): Promise<void> {
  return apiFetch<void>(`/api/sections/${sectionId}`, { method: "DELETE" });
}

/** moveSection: bölümü verilen sıraya (0'dan) taşır; kursun yeni sıradaki bölümlerini döner. */
export function moveSection(sectionId: number, position: number): Promise<SectionResponse[]> {
  return apiFetch<SectionResponse[]>(`/api/sections/${sectionId}/position`, {
    method: "PUT",
    body: JSON.stringify({ position }),
  });
}

/** createLesson: ders bölümün sonuna eklenir; içerik ayrı uçla yüklenir (uploadLessonContent). */
export function createLesson(sectionId: number, request: LessonRequest): Promise<LessonResponse> {
  return apiFetch<LessonResponse>(`/api/sections/${sectionId}/lessons`, { method: "POST", body: JSON.stringify(request) });
}

/** updateLesson: `contentType` değişirse yüklü dosya dersten ayrılır (`hasContent` false). */
export function updateLesson(lessonId: number, request: LessonRequest): Promise<LessonResponse> {
  return apiFetch<LessonResponse>(`/api/lessons/${lessonId}`, { method: "PUT", body: JSON.stringify(request) });
}

/** deleteLesson: dersi ve içerik dosyasını siler. */
export function deleteLesson(lessonId: number): Promise<void> {
  return apiFetch<void>(`/api/lessons/${lessonId}`, { method: "DELETE" });
}

/** moveLesson: dersi aynı bölümde verilen sıraya (0'dan) taşır; bölümün yeni sıradaki derslerini döner. */
export function moveLesson(lessonId: number, position: number): Promise<LessonResponse[]> {
  return apiFetch<LessonResponse[]>(`/api/lessons/${lessonId}/position`, {
    method: "PUT",
    body: JSON.stringify({ position }),
  });
}

// Tipler docs/api/openapi.yaml'daki CourseStatsResponse, CourseStudentResponse ve CourseStudentPage ile aynıdır.

export interface QuizStat {
  quizId: number;
  title: string;
  attemptCount: number;
  /** Geçen denemelerin oranı (yüzde). */
  passRate: number;
}

export interface CourseStats {
  courseId: number;
  enrollmentCount: number;
  averageProgressPercent: number;
  /** %100 ilerlemeye ulaşan kursiyerlerin oranı (yüzde). */
  completionRate: number;
  quizzes: QuizStat[];
  rating: RatingSummary;
}

export interface StudentQuizResult {
  quizId: number;
  bestPercent: number;
  passed: boolean;
}

export interface CourseStudent {
  student: UserSummary;
  enrolledAt: string;
  progressPercent: number;
  lastAccessedAt?: string | null;
  quizResults: StudentQuizResult[];
}

export interface CourseStudentPage extends PageMeta {
  content: CourseStudent[];
}

/** getInstructorCourseStats: kursun sahibi eğitmen. 403 sahibi değil, 404 kurs yok. */
export function getCourseStats(courseId: number): Promise<CourseStats> {
  return apiFetch<CourseStats>(`/api/instructor/courses/${courseId}/stats`);
}

/** listInstructorCourseStudents: sayfalı kursiyer ilerleme listesi; `page` 0'dan başlar. */
export function listCourseStudents(courseId: number, page = 0, size = 20): Promise<CourseStudentPage> {
  return apiFetch<CourseStudentPage>(`/api/instructor/courses/${courseId}/students?page=${page}&size=${size}`);
}
