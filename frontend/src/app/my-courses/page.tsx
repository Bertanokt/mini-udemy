import type { Metadata } from "next";
import { MyCoursesView } from "./MyCoursesView";

export const metadata: Metadata = {
  title: "Kayıtlı kurslarım · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function MyCoursesPage() {
  return <MyCoursesView />;
}
