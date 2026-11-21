import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";
import { Loading } from "@/components/common/Loading";

/** /profile giriş yapmış her role açık (#117). */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<Loading />}>
      <RequireRole roles={["STUDENT", "INSTRUCTOR", "ADMIN"]}>{children}</RequireRole>
    </Suspense>
  );
}
