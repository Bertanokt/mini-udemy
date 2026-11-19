import type { Metadata } from "next";
import { Suspense } from "react";
import { LessonPlayerView } from "./LessonPlayerView";

export const metadata: Metadata = {
  title: "Ders · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** `params` çalışma zamanında bilinir; beklenirken kabuk önceden çizilsin diye Suspense içinde çözülür. */
export default function LessonPage() {
  return (
    <Suspense fallback={<p className="auth-status">Ders yükleniyor…</p>}>
      <LessonPlayerView />
    </Suspense>
  );
}
