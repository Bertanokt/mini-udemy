import type { Metadata } from "next";
import { CategoryManager } from "./CategoryManager";

export const metadata: Metadata = {
  title: "Kategori yönetimi · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un (admin/layout.tsx) altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

export default function CategoriesPage() {
  return <CategoryManager />;
}
