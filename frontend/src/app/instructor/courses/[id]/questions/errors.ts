import { ApiError } from "@/lib/api/client";

/** API hatasını kullanıcıya gösterilecek cümleye çevirir; sunucunun Türkçe mesajı varsa o kullanılır. */
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.status === 403) {
      return (
        e.body?.message ??
        "Bu kursun soru bankasını yönetme yetkiniz yok. Yalnız kursun sahibi eğitmen yönetebilir."
      );
    }
    if (e.status === 404) return e.body?.message ?? "Kurs ya da soru bulunamadı.";
    return e.body?.message ?? fallback;
  }
  return "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.";
}
