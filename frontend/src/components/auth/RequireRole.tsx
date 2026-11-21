"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import type { Role } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/AuthProvider";
import { loginHref } from "@/lib/auth/routes";
import { Loading } from "@/components/common/Loading";

/**
 * Sayfayı yalnız verilen rollere gösterir (#19).
 * - Oturum bilinmiyorsa (token var, `GET /api/me` bekleniyor) bekleme mesajı;
 * - giriş yapılmamışsa `/login?next=<bu adres>`'e yönlendirir;
 * - rol uymuyorsa "yetkiniz yok" gösterir.
 *
 * Bu yalnız arayüz korumasıdır: kullanıcıya yanlış sayfayı göstermez. Asıl yetki kontrolü backend'dedir
 * (`/api/admin/**`, `/api/instructor/**` ve `@PreAuthorize` → 403; docs/architecture.md › Yetki kontrolleri).
 */
export function RequireRole({ roles, children }: { roles: Role[]; children: React.ReactNode }) {
  const { status, user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "anonymous") {
      router.replace(loginHref(pathname + window.location.search));
    }
  }, [status, pathname, router]);

  if (status !== "authenticated" || user === null) {
    return <Loading text="Oturum kontrol ediliyor…" />;
  }
  if (!roles.includes(user.role)) {
    return (
      <section className="placeholder-page" role="alert">
        <h1>Yetkiniz yok</h1>
        <p>Bu sayfayı görüntüleme yetkiniz yok.</p>
      </section>
    );
  }
  return <>{children}</>;
}
