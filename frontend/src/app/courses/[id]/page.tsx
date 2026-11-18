import type { Metadata } from "next";
import { Suspense } from "react";
import { CourseDetailView } from "./CourseDetailView";

export const metadata: Metadata = {
  title: "Kurs · Mini Udemy",
};

/** `params` çalışma zamanında bilinir; beklenirken kabuk önceden çizilsin diye Suspense içinde çözülür. */
export default function CoursePage() {
  return (
    <Suspense fallback={<p className="auth-status">Kurs yükleniyor…</p>}>
      <CourseDetailView />
    </Suspense>
  );
}
