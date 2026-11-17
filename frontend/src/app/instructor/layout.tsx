import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";

/**
 * /instructor ve altındaki tüm sayfalar yalnız INSTRUCTOR rolüne açık (#19). Panel sayfaları #38'de.
 * Suspense: RequireRole adres bilgisini (usePathname) okur; `[id]` gibi dinamik yollarda bu, bir bekleme sınırı ister.
 */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<p className="auth-status">Yükleniyor…</p>}>
      <RequireRole roles={["INSTRUCTOR"]}>{children}</RequireRole>
    </Suspense>
  );
}
