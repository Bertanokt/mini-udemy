import type { Role } from "@/lib/api/auth";

/**
 * Rol başına giriş sonrası açılan sayfa (#18). Eğitmen paneli (#38) ve yönetici paneli (#62) bu adreslere yazılır;
 * adres değişirse yalnız burası değişir.
 */
export const HOME_BY_ROLE: Record<Role, string> = {
  STUDENT: "/",
  INSTRUCTOR: "/instructor",
  ADMIN: "/admin",
};

export function homeForRole(role: Role): string {
  return HOME_BY_ROLE[role];
}

/**
 * Girişten sonra dönülecek adres (`/login?next=...`). Yalnız site içi yollar kabul edilir: `/` ile başlamalı,
 * `//` ya da `/\` ile başlamamalı (tarayıcı bunları başka bir siteye giden adres sayar; açık yönlendirme açığı).
 * Geçersizse `null`.
 */
export function safeNextPath(next: string | null): string | null {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) {
    return null;
  }
  return next;
}

/** Giriş sayfasının, dönülecek adresi taşıyan bağlantısı. */
export function loginHref(next: string): string {
  return `/login?next=${encodeURIComponent(next)}`;
}
