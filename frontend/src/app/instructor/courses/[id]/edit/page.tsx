import type { Metadata } from "next";
import { Suspense } from "react";
import { EditCourse } from "./EditCourse";
import { Loading } from "@/components/common/Loading";

export const metadata: Metadata = {
  title: "Kursu düzenle · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) her kurs satırının "Düzenle" bağlantısından açılır (#33). */
export default function EditCoursePage() {
  return (
    <Suspense fallback={<Loading />}>
      <EditCourse />
    </Suspense>
  );
}
