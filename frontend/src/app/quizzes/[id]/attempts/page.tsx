import type { Metadata } from "next";
import { Suspense } from "react";
import { AttemptsView } from "./AttemptsView";

export const metadata: Metadata = {
  title: "Sınav denemelerim · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** `params` çalışma zamanında bilinir; beklenirken kabuk önceden çizilsin diye Suspense içinde çözülür. */
export default function QuizAttemptsPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<p className="auth-status">Denemeler yükleniyor…</p>}>
      <AttemptsView params={params} />
    </Suspense>
  );
}
