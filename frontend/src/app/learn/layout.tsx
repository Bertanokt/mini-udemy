import { Suspense } from "react";
import { RequireRole } from "@/components/auth/RequireRole";
import { Loading } from "@/components/common/Loading";

/**
 * Ders oynatıcı (#45) giriş ister. Kim hangi derse erişir kuralı backend'dedir (kayıtlı kursiyer, kursun eğitmeni,
 * yönetici; diğerlerine 403); sayfa 403'te kayıt ekranını gösterir.
 * Suspense: RequireRole adres bilgisini (usePathname) okur; dinamik yollarda bu, bir bekleme sınırı ister.
 */
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<Loading />}>
      <RequireRole roles={["STUDENT", "INSTRUCTOR", "ADMIN"]}>{children}</RequireRole>
    </Suspense>
  );
}
