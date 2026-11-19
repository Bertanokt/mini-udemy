import type { Metadata } from "next";
import { Suspense } from "react";
import { LearnStart } from "./LearnStart";

export const metadata: Metadata = {
  title: "Ders · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function LearnStartPage() {
  return (
    <Suspense fallback={<p className="auth-status">Ders yükleniyor…</p>}>
      <LearnStart />
    </Suspense>
  );
}
