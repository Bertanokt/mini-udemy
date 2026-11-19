import type { Metadata } from "next";
import { CourseReview } from "./CourseReview";

export const metadata: Metadata = {
  title: "Kurs inceleme · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Yöneticinin kursu incelediği sayfa (#62); kurs onayı listesinden bağlanır. */
export default function AdminCourseReviewPage() {
  return <CourseReview />;
}
