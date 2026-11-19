import { apiFetch } from "./client";
import type { Role } from "./auth";
import type { CategoryResponse, CourseStatus, CourseSummaryPage, PageMeta, UserSummary } from "./catalog";
import type { CourseResponse } from "./instructor";
import type { ReviewResponse } from "./reviews";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (CategoryRequest). Liste herkese açık uç olduğundan
// `listCategories` catalog.ts'te.

export interface CategoryRequest {
  name: string;
}

export function createCategory(request: CategoryRequest): Promise<CategoryResponse> {
  return apiFetch<CategoryResponse>("/api/admin/categories", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateCategory(id: number, request: CategoryRequest): Promise<CategoryResponse> {
  return apiFetch<CategoryResponse>(`/api/admin/categories/${id}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function deleteCategory(id: number): Promise<void> {
  return apiFetch<void>(`/api/admin/categories/${id}`, { method: "DELETE" });
}

// Kullanıcı yönetimi (#63): AdminUserResponse, AdminUserPage, UserStatusRequest. passwordHash hiçbir yanıtta yoktur.
export interface AdminUserResponse {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface AdminUserPage extends PageMeta {
  content: AdminUserResponse[];
}

export interface AdminUserQuery {
  role?: Role;
  active?: boolean;
  q?: string;
  page?: number;
  size?: number;
}

export function listUsers(query: AdminUserQuery): Promise<AdminUserPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return apiFetch<AdminUserPage>(`/api/admin/users${qs ? `?${qs}` : ""}`);
}

export function setUserStatus(id: number, active: boolean): Promise<AdminUserResponse> {
  return apiFetch<AdminUserResponse>(`/api/admin/users/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ active }),
  });
}

// Platform raporu (#145, #147): PlatformReportResponse. Sayılar tüm zamanlarındır; from/to her zaman null (İnce kapsam).
export interface PlatformReportResponse {
  from: string | null;
  to: string | null;
  usersByRole: Record<string, number>;
  coursesByStatus: Record<string, number>;
  enrollmentCount: number;
  completionCount: number;
  certificateCount: number;
  topCourses: { courseId: number; title: string; enrollmentCount: number }[];
  categoryDistribution: { categoryId: number; name: string; courseCount: number }[];
}

export function getPlatformReport(): Promise<PlatformReportResponse> {
  return apiFetch<PlatformReportResponse>("/api/admin/reports");
}

// Kurs onayı (#62): adminListCourses, adminApproveCourse, adminRejectCourse, adminUnpublishCourse. Liste
// CourseSummaryPage, işlemler CourseResponse döner. Durum geçişini backend'deki CourseState verir; uygun olmayan
// durumda 409. Gerekçe ReasonRequest'tir (5–1000 karakter).
export interface AdminCourseQuery {
  status?: CourseStatus;
  page?: number;
  size?: number;
}

export function listAdminCourses(query: AdminCourseQuery): Promise<CourseSummaryPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return apiFetch<CourseSummaryPage>(`/api/admin/courses${qs ? `?${qs}` : ""}`);
}

/** IN_REVIEW → PUBLISHED. */
export function approveCourse(id: number): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/admin/courses/${id}/approve`, { method: "POST" });
}

/** IN_REVIEW → DRAFT; gerekçe kursta saklanır. */
export function rejectCourse(id: number, reason: string): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/admin/courses/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

/** PUBLISHED → UNPUBLISHED; gerekçe kursta saklanır. */
export function unpublishCourse(id: number, reason: string): Promise<CourseResponse> {
  return apiFetch<CourseResponse>(`/api/admin/courses/${id}/unpublish`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

// Yorum moderasyonu (#142, #146): adminListReviews (ReviewPage), adminSetReviewVisibility (ReviewVisibilityRequest).
// Gizli yorum herkese açık listede görünmez ve ortalamaya katılmaz; gerekçe yalnız bu uçların yanıtında döner.
// Gizlerken gerekçe zorunludur (en fazla 1000 karakter); geri açınca sunucu gerekçeyi siler.
export interface ReviewPage extends PageMeta {
  content: ReviewResponse[];
}

export interface AdminReviewQuery {
  courseId?: number;
  hidden?: boolean;
  page?: number;
  size?: number;
}

export function listAdminReviews(query: AdminReviewQuery): Promise<ReviewPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return apiFetch<ReviewPage>(`/api/admin/reviews${qs ? `?${qs}` : ""}`);
}

export function setReviewVisibility(id: number, hidden: boolean, reason?: string): Promise<ReviewResponse> {
  return apiFetch<ReviewResponse>(`/api/admin/reviews/${id}/visibility`, {
    method: "PUT",
    body: JSON.stringify(hidden ? { hidden, reason } : { hidden }),
  });
}

// Şikâyet incelemesi (#144, #146): ComplaintResponse, ComplaintPage, ComplaintStatusRequest. Şikâyet ya bir kursa
// (courseId) ya bir yoruma (reviewId) aittir. Yönetici yalnız REVIEWED ya da REJECTED seçebilir.
export type ComplaintStatus = "OPEN" | "REVIEWED" | "REJECTED";

export interface ComplaintResponse {
  id: number;
  reporter: UserSummary;
  courseId: number | null;
  reviewId: number | null;
  reason: string;
  status: ComplaintStatus;
  createdAt: string;
  resolvedAt: string | null;
}

export interface ComplaintPage extends PageMeta {
  content: ComplaintResponse[];
}

export function listComplaints(page: number, size: number): Promise<ComplaintPage> {
  return apiFetch<ComplaintPage>(`/api/admin/complaints?page=${page}&size=${size}`);
}

export function setComplaintStatus(id: number, status: Exclude<ComplaintStatus, "OPEN">): Promise<ComplaintResponse> {
  return apiFetch<ComplaintResponse>(`/api/admin/complaints/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status }),
  });
}
