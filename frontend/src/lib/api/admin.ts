import { apiFetch } from "./client";
import type { Role } from "./auth";
import type { CategoryResponse, CourseStatus, CourseSummaryPage, PageMeta } from "./catalog";
import type { CourseResponse } from "./instructor";

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
