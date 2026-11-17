import type { Metadata } from "next";
import { MyCoursesView } from "./MyCoursesView";

export const metadata: Metadata = {
  title: "Kayıtlı kurslarım · Mini Udemy",
};

export default function MyCoursesPage() {
  return <MyCoursesView />;
}
