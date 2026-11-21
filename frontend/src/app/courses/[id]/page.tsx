import type { Metadata } from "next";
import { Suspense } from "react";
import { CourseDetailView } from "./CourseDetailView";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Kurs · Mini Udemy",
};

/** `params` çalışma zamanında bilinir; beklenirken kabuk önceden çizilsin diye Suspense içinde çözülür. */
export default function CoursePage() {
  return (
    <Suspense fallback={<Loading text="Kurs yükleniyor…" />}>
      <CourseDetailView />
    </Suspense>
  );
}
