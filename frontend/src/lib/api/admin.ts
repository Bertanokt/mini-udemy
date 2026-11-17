import { apiFetch } from "./client";
import type { Role } from "./auth";
import type { CategoryResponse, PageMeta } from "./catalog";

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
