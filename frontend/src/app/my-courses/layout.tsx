import { RequireRole } from "@/components/auth/RequireRole";

/** /my-courses yalnız STUDENT rolüne açık (#37). */
export default function Layout({ children }: { children: React.ReactNode }) {
  return <RequireRole roles={["STUDENT"]}>{children}</RequireRole>;
}
