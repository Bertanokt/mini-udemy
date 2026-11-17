import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";

/**
 * /quizzes altındaki sayfalar yalnız STUDENT rolüne açık (#141).
 * Suspense: RequireRole adres bilgisini (usePathname) okur; `[id]` gibi dinamik yollarda bu, bir bekleme sınırı ister.
 */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<p className="auth-status">Yükleniyor…</p>}>
      <RequireRole roles={["STUDENT"]}>{children}</RequireRole>
    </Suspense>
  );
}
