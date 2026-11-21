import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";
import { Loading } from "@/components/common/Loading";

/**
 * /admin ve altındaki tüm sayfalar yalnız ADMIN rolüne açık (#19).
 * Suspense: RequireRole adres bilgisini (usePathname) okur; `/admin/courses/[id]` gibi dinamik yollarda bu, bir
 * bekleme sınırı ister (#62; eğitmen paneliyle aynı).
 */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<Loading />}>
      <RequireRole roles={["ADMIN"]}>{children}</RequireRole>
    </Suspense>
  );
}
