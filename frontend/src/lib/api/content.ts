import { API_BASE_URL, apiFetch, apiUpload, type UploadProgress } from "./client";
import type { LessonResponse } from "./courses";

/**
 * Dersin içerik dosyasını yükler (#112, kursun sahibi eğitmen). Kural dersin `contentType`'ının fabrikasındadır
 * (ADR-011): VIDEO mp4/webm ≤200 MB, PDF ≤20 MB, DOCUMENT pdf/docx/pptx ≤20 MB; tür içerikten denetlenir.
 * 413 büyük, 415 tür, 409 kurs durumu izin vermiyor (ApiError).
 */
export function uploadLessonContent(lessonId: number, file: File, onProgress?: UploadProgress): Promise<LessonResponse> {
  const body = new FormData();
  body.append("file", file);
  return apiUpload<LessonResponse>(`/api/lessons/${lessonId}/content`, body, onProgress);
}

// Tipler docs/api/openapi.yaml'daki ContentTokenResponse ile aynıdır.

export interface ContentTokenResponse {
  token: string;
  expiresAt: string;
}

/**
 * Dersin içeriği için 5 dakika geçerli token (#45, ADR-012). Kural içeriği açmakla aynı: kayıtlı STUDENT, kursun
 * eğitmeni ya da ADMIN; diğerleri 403, içerik yoksa 404 (ApiError).
 */
export function createContentToken(lessonId: number): Promise<ContentTokenResponse> {
  return apiFetch<ContentTokenResponse>(`/api/lessons/${lessonId}/content-token`, { method: "POST" });
}

/**
 * İçeriğin token'lı adresi. `<video>` ve bağlantılar `Authorization` başlığı gönderemediği için kimlik adresteki
 * token'dadır; yetkiyi backend her istekte yeniden denetler (ADR-012).
 */
export function lessonContentUrl(lessonId: number, token: string): string {
  return `${API_BASE_URL}/api/lessons/${lessonId}/content?token=${encodeURIComponent(token)}`;
}
