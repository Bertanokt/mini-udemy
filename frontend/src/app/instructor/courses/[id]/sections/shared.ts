import { ApiError } from "@/lib/api/client";

/**
 * Kurs durumuna göre arayüzde açık gösterilen işlemler (ipucu; asıl karar `CourseState`'te, ADR-010).
 * - editable: Taslak ve Yayından kaldırıldı — her şey.
 * - canAddLesson / canUploadNew: ayrıca Yayımlandı — yeni ders ve içeriği olmayan derse yükleme.
 */
export interface Permissions {
  editable: boolean;
  canAddLesson: boolean;
  canUploadNew: boolean;
}

export function apiMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.status === 403) return e.body?.message ?? "Bu kursu düzenleme yetkiniz yok. Yalnız kursun sahibi eğitmen düzenleyebilir.";
    if (e.status === 404) return e.body?.message ?? "Kurs bulunamadı.";
    return e.body?.message ?? fallback;
  }
  return "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.";
}

export function isConflict(e: unknown): boolean {
  return e instanceof ApiError && e.status === 409;
}

export function fieldErrors(e: unknown): Record<string, string> | undefined {
  return e instanceof ApiError ? e.body?.errors : undefined;
}
