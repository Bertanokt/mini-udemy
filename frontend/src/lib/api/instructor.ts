import { apiFetch } from "./client";
import type { CategoryResponse, CourseLevel, CourseStatus, PageMeta, UserSummary } from "./catalog";
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

/** submitCourse: DRAFT/UNPUBLISHED → IN_REVIEW. 400 eksik içerik, 409 geçersiz durum. */
export function submitCourse(id: number): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/courses/${id}/submit`, { method: "POST" });
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
