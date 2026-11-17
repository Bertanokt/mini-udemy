import { RequireRole } from "@/components/auth/RequireRole";

/** /instructor ve altındaki tüm sayfalar yalnız INSTRUCTOR rolüne açık (#19). Panel sayfaları #38'de. */
export default function Layout({ children }: { children: React.ReactNode }) {
  return <RequireRole roles={["INSTRUCTOR"]}>{children}</RequireRole>;
}
