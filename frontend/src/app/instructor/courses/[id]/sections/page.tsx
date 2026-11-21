import type { Metadata } from "next";
import { SectionManager } from "./SectionManager";

export const metadata: Metadata = {
  title: "Bölüm ve dersler · Mini Udemy",
};

// Sayfa, oturum bilgisini bekleyen RequireRole'un altında; anlık gezinme doğrulamasından bilerek çıkarıldı.
export const instant = false;

/** Eğitmen panelindeki (#38) kurs satırından ve kurs formundan (#33) bağlanır (#34). */
export default function SectionsPage() {
  return <SectionManager />;
}
