import { apiFetch } from "./client";

// Tipler docs/api/openapi.yaml'daki şemalarla aynıdır (ReviewRequest, ReviewResponse, RatingSummary, CourseReviewsResponse).

export interface ReviewRequest {
  rating: number;
  comment?: string;
}

export interface ReviewAuthor {
  id: number;
  fullName: string;
}

export interface ReviewResponse {
  id: number;
  courseId: number;
  author: ReviewAuthor;
  rating: number;
  comment?: string | null;
  hidden: boolean;
  moderationReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RatingSummary {
  averageRating?: number | null;
  reviewCount: number;
  /** Puan başına değerlendirme sayısı (anahtar "1"–"5"). */
  distribution: Record<string, number>;
}

export interface CourseReviewsResponse {
  content: ReviewResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  summary: RatingSummary;
}

/** Kursun görünür değerlendirmeleri ve özeti; herkese açık. `page` 0'dan başlar. */
export function listCourseReviews(courseId: number, page = 0, size = 10): Promise<CourseReviewsResponse> {
  return apiFetch<CourseReviewsResponse>(`/api/courses/${courseId}/reviews?page=${page}&size=${size}`);
}

/** Kursu değerlendirir (kayıtlı STUDENT). Kayıtsızsa 403, ikinci kez 409 (ApiError). */
export function createReview(courseId: number, request: ReviewRequest): Promise<ReviewResponse> {
  return apiFetch<ReviewResponse>(`/api/courses/${courseId}/reviews`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}

/** Kendi değerlendirmeni günceller (STUDENT, sahibi). */
export function updateReview(reviewId: number, request: ReviewRequest): Promise<ReviewResponse> {
  return apiFetch<ReviewResponse>(`/api/reviews/${reviewId}`, {
    method: "PUT",
    body: JSON.stringify(request),
  });
}
