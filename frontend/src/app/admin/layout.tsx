import { RequireRole } from "@/components/auth/RequireRole";

/** /admin ve altındaki tüm sayfalar yalnız ADMIN rolüne açık (#19). Panel sayfaları #62'de. */
export default function Layout({ children }: { children: React.ReactNode }) {
  return <RequireRole roles={["ADMIN"]}>{children}</RequireRole>;
}
