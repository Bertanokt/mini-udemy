import type { Metadata } from "next";
import { Suspense } from "react";
import { ReviewModerationList } from "./ReviewModerationList";

export const metadata: Metadata = {
  title: "Yorum moderasyonu · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Yönetici yorum moderasyonu ekranı (#146 yorum kısmı; backend #142). */
export default function AdminReviewsPage() {
  return (
    // Filtreler ve sayfa URL'den okunduğu için istemcide çizilir.
    <Suspense fallback={<p className="auth-status">Yorumlar yükleniyor…</p>}>
      <ReviewModerationList />
    </Suspense>
  );
}
