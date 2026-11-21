import type { Metadata } from "next";
import { Suspense } from "react";
import { QuizTakeView } from "./QuizTakeView";

export const metadata: Metadata = {
  title: "Sınav · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function QuizTakePage() {
  return (
    <Suspense fallback={<p className="auth-status">Sınav yükleniyor…</p>}>
      <QuizTakeView />
    </Suspense>
  );
}
