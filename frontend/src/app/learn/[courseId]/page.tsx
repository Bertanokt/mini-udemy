import type { Metadata } from "next";
import { Suspense } from "react";
import { LearnStart } from "./LearnStart";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Ders · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function LearnStartPage() {
  return (
    <Suspense fallback={<Loading text="Ders yükleniyor…" />}>
      <LearnStart />
    </Suspense>
  );
}
