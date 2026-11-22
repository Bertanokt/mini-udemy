import type { Metadata } from "next";
import { Suspense } from "react";
import { MyCourses } from "./MyCourses";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Eğitmen paneli · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function Page() {
  return (
    <Suspense fallback={<Loading />}>
      <MyCourses />
    </Suspense>
  );
}
