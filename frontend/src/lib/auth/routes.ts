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
