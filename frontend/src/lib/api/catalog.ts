import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (CategoryResponse, CourseSummaryResponse, CourseSummaryPage).

export type CourseLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type CourseStatus = "DRAFT" | "IN_REVIEW" | "PUBLISHED" | "UNPUBLISHED";
export type CatalogSort = "newest" | "title" | "rating" | "popular";

export const LEVEL_LABELS: Record<CourseLevel, string> = {
  BEGINNER: "Başlangıç",
  INTERMEDIATE: "Orta",
  ADVANCED: "İleri",
};

export const SORT_LABELS: Record<CatalogSort, string> = {
  newest: "En yeni",
  title: "Ada göre",
  rating: "En yüksek puan",
  popular: "En popüler",
};

export interface CategoryResponse {
  id: number;
  name: string;
}

export interface UserSummary {
  id: number;
  fullName: string;
}

export interface CourseSummaryResponse {
  id: number;
  title: string;
  category: CategoryResponse;
  level: CourseLevel;
  status: CourseStatus;
  instructor: UserSummary;
  coverUrl: string | null;
  lessonCount: number;
  totalDurationSeconds: number;
  enrollmentCount: number;
  averageRating?: number | null;
  reviewCount: number;
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CourseSummaryPage extends PageMeta {
  content: CourseSummaryResponse[];
}

export interface CatalogQuery {
  q?: string;
  categoryId?: number;
  level?: CourseLevel;
  instructorId?: number;
  sort?: CatalogSort;
  page?: number;
  size?: number;
}

export function listCategories(): Promise<CategoryResponse[]> {
  return apiFetch<CategoryResponse[]>("/api/categories");
}

export function searchCatalog(query: CatalogQuery): Promise<CourseSummaryPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  const qs = params.toString();
  return apiFetch<CourseSummaryPage>(`/api/courses${qs ? `?${qs}` : ""}`);
}
