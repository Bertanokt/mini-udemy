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

/** Adres ayrıştırmada kullanılan sahte origin; yalnız "sonuç aynı origin'de mi" sorusu için. */
const PARSE_BASE = "http://site.invalid";

/**
 * Girişten sonra dönülecek adres (`/login?next=...`). Yalnız site içi yollar kabul edilir; geçersizse `null`.
 *
 * Kontrol, tarayıcının yapacağı ayrıştırmanın aynısıyla (`URL`) yapılır: sonuç aynı origin'de değilse reddedilir.
 * Yalnız metnin başına bakmak yetmiyor: tarayıcı adresteki sekme ve satır sonlarını sildiği için `/\t/örnek.com`
 * `//örnek.com`'a, yani başka bir siteye dönüşür (açık yönlendirme; #197 review'u, 17.11.2026'da ölçüldü).
 * `javascript:` gibi şemalar da aynı kontrolle reddedilir. Dönen değer ayrıştırılmış yoldur (pathname + search + hash).
 */
export function safeNextPath(next: string | null): string | null {
  if (!next || !next.startsWith("/")) {
    return null;
  }
  let url: URL;
  try {
    url = new URL(next, PARSE_BASE);
  } catch {
    return null;
  }
  if (url.origin !== PARSE_BASE) {
    return null;
  }
  return url.pathname + url.search + url.hash;
}

/** Giriş sayfasının, dönülecek adresi taşıyan bağlantısı. */
export function loginHref(next: string): string {
  return `/login?next=${encodeURIComponent(next)}`;
}
