import { apiFetch } from "./client";
import type { CategoryResponse } from "./catalog";

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
