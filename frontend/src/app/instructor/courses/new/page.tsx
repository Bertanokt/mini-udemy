import type { Metadata } from "next";
import { Suspense } from "react";
import { CourseForm } from "../CourseForm";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Yeni kurs · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) "Yeni kurs" düğmesinden açılır (#33). */
export default function NewCoursePage() {
  return (
    <Suspense fallback={<Loading />}>
      <CourseForm courseId={null} />
    </Suspense>
  );
}
