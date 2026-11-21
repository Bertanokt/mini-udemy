// Red ve yayından kaldırma gerekçesi: backend'deki ReasonRequest ile aynı kural (@NotBlank, 5–1000 karakter).
// Sunucu da denetler; burada kullanıcıya istek atmadan önce aynı mesaj gösterilir.

export const REASON_MIN = 5;
export const REASON_MAX = 1000;

/** Hata mesajı ya da geçerliyse null. Gerekçe kırpılmış haliyle gönderilir, uzunluk da onunla ölçülür. */
export function validateReason(reason: string): string | null {
  const trimmed = reason.trim();
  if (trimmed.length === 0) return "Gerekçe zorunludur.";
  if (trimmed.length < REASON_MIN || trimmed.length > REASON_MAX) {
    return `Gerekçe ${REASON_MIN}–${REASON_MAX} karakter olmalıdır.`;
  }
  return null;
}
