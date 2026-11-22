import type { Metadata } from "next";
import { Suspense } from "react";
import { ComplaintList } from "./ComplaintList";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Şikâyetler · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function ComplaintsPage() {
  return (
    // Sayfa numarası URL'den okunduğu için istemcide çizilir.
    <Suspense fallback={<Loading text="Şikâyetler yükleniyor…" />}>
      <ComplaintList />
    </Suspense>
  );
}
