import type { Metadata } from "next";
import { Suspense } from "react";
import { UserManager } from "./UserManager";

export const metadata: Metadata = {
  title: "Kullanıcı yönetimi · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function UsersPage() {
  return (
    // Filtreler URL'den okunduğu için istemcide çizilir.
    <Suspense fallback={<p className="auth-status">Kullanıcılar yükleniyor…</p>}>
      <UserManager />
    </Suspense>
  );
}
