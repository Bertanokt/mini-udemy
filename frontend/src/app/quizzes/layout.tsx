import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";
import { Loading } from "@/components/common/Loading";

/**
 * /quizzes altındaki sayfalar yalnız STUDENT rolüne açık (#141).
 * Suspense: RequireRole adres bilgisini (usePathname) okur; `[id]` gibi dinamik yollarda bu, bir bekleme sınırı ister.
 */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<Loading />}>
      <RequireRole roles={["STUDENT"]}>{children}</RequireRole>
    </Suspense>
  );
}
