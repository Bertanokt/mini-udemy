import type { Metadata } from "next";
import { Suspense } from "react";
import { CourseReviewList } from "./CourseReviewList";

export const metadata: Metadata = {
  title: "Kurs onayı · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Yönetici kurs onay ekranı (#62): Senaryo 1'in son adımı. */
export default function AdminCoursesPage() {
  return (
    // Durum filtresi ve sayfa URL'den okunduğu için istemcide çizilir.
    <Suspense fallback={<p className="auth-status">Kurslar yükleniyor…</p>}>
      <CourseReviewList />
    </Suspense>
  );
}
