import { ApiError } from "@/lib/api/client";

/** Sözleşmedeki sınırlar (CategoryRequest): ad 2–100 karakter, baştaki/sondaki boşluklar sayılmaz. */
export const NAME_MIN = 2;
export const NAME_MAX = 100;

/** Alan hatası mesajı; geçerliyse null. */
export function validateName(raw: string): string | null {
  const name = raw.trim();
  if (name.length === 0) return "Kategori adı zorunludur.";
  if (name.length < NAME_MIN || name.length > NAME_MAX) return `Kategori adı ${NAME_MIN}–${NAME_MAX} karakter olmalıdır.`;
  return null;
}

/** API hatasını kullanıcıya gösterilecek cümleye çevirir; sunucunun Türkçe mesajı varsa o kullanılır. */
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    if (e.status === 403) return e.body?.message ?? "Kategorileri yönetme yetkiniz yok.";
    if (e.status === 404) return e.body?.message ?? "Kategori bulunamadı; liste yenilendi.";
    return e.body?.message ?? fallback;
  }
  return "Sunucuya ulaşılamadı. Bağlantınızı kontrol edip tekrar deneyin.";
}

/** Sunucunun alan hatası (errors.name) varsa onu, yoksa null döndürür. */
export function nameFieldError(e: unknown): string | null {
  return e instanceof ApiError ? (e.body?.errors?.name ?? null) : null;
}

export function sortByName<T extends { name: string }>(items: T[]): T[] {
  return [...items].sort((a, b) => a.name.localeCompare(b.name, "tr"));
}
